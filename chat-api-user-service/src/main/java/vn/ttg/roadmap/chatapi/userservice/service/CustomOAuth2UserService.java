package vn.ttg.roadmap.chatapi.userservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.ttg.roadmap.chatapi.userservice.dto.CustomOAuth2User;
import vn.ttg.roadmap.chatapi.userservice.dto.OAuth2UserInfo;
import vn.ttg.roadmap.chatapi.userservice.dto.OAuth2UserInfoFactory;
import vn.ttg.roadmap.chatapi.userservice.entity.User;
import vn.ttg.roadmap.chatapi.userservice.handler.OAuth2LoginSuccessHandler;

import java.util.Collections;
import java.util.List;

/**
 * Custom OAuth2 User Service for handling Facebook OAuth2 logins.
 * 
 * <h3>OAuth2 Flow Position:</h3>
 * This service is called AFTER Spring Security has:
 * <ol>
 *   <li>Received authorization code from Facebook (via redirect)</li>
 *   <li>Exchanged authorization code for access_token (automatic)</li>
 * </ol>
 * 
 * <h3>What This Service Does:</h3>
 * <ol>
 *   <li>Calls Spring Security's {@link DefaultOAuth2UserService} which:
 *       <ul>
 *         <li>Calls Facebook's UserInfo endpoint: GET https://graph.facebook.com/v18.0/me</li>
 *         <li>Uses access_token: {@code ?access_token={accessToken}&fields=id,name,email,picture}</li>
 *         <li>Extracts user attributes (id, name, email, picture)</li>
 *         <li>Builds OAuth2User with attributes</li>
 *       </ul>
 *   </li>
 *   <li>Creates/updates User record in PostgreSQL database</li>
 *   <li>Returns CustomOAuth2User principal for Spring Security</li>
 * </ol>
 * 
 * <h3>Difference from CustomOidcUserService:</h3>
 * <ul>
 *   <li>Facebook uses OAuth2 (not OIDC), so no ID token</li>
 *   <li>Returns CustomOAuth2User (not OidcUser)</li>
 *   <li>Uses DefaultOAuth2UserService (not OidcUserService)</li>
 * </ul>
 * 
 * @see CustomOidcUserService For Google OIDC logins
 * @see OAuth2LoginSuccessHandler Called after this service completes
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {
    
    private final UserService userService;
    
    /**
     * Loads OAuth2 user information and creates/updates user in database.
     * 
     * <h3>When Called:</h3>
     * Called by Spring Security AFTER authorization code has been exchanged for access token.
     * The userRequest already contains:
     * <ul>
     *   <li>{@code userRequest.getAccessToken()} - Facebook's access token (already obtained)</li>
     * </ul>
     * 
     * <h3>What Happens:</h3>
     * <ol>
     *   <li><b>Line 35-36:</b> Calls Spring Security's DefaultOAuth2UserService which:
     *       <ul>
     *         <li>Makes HTTP GET request to Facebook's UserInfo endpoint</li>
     *         <li>URL: {@code https://graph.facebook.com/v18.0/me?fields=id,name,email,picture}</li>
     *         <li>Query param: {@code access_token={accessToken}}</li>
     *         <li>Parses response and builds OAuth2User with attributes</li>
     *       </ul>
     *   </li>
     *   <li><b>Line 41:</b> Extracts user info from OAuth2 attributes</li>
     *   <li><b>Line 49-64:</b> Creates or updates User in PostgreSQL</li>
     *   <li><b>Line 68-74:</b> Returns CustomOAuth2User (wraps OAuth2User)</li>
     * </ol>
     * 
     * @param userRequest Contains access_token (already exchanged by Spring Security)
     * @return CustomOAuth2User with user attributes from Facebook
     * @throws OAuth2AuthenticationException if email not found or user processing fails
     */
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        log.info("Loading OAuth2 user for registration: {}", userRequest.getClientRegistration().getRegistrationId());
        
        // ⚠️ IMPORTANT: At this point, Spring Security has already:
        // 1. Exchanged authorization code for access_token
        // 2. userRequest.getAccessToken() contains Facebook's access token
        
        // Use Spring Security's default OAuth2UserService to call Facebook's UserInfo endpoint
        // This will:
        // - Call Facebook's UserInfo endpoint: GET https://graph.facebook.com/v18.0/me
        // - Query params: access_token={accessToken}&fields=id,name,email,picture
        // - Extract user attributes (id, name, email, picture)
        // - Build OAuth2User with attributes
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);
        
        // Now we have user info from Facebook (id, name, email, picture)
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        // Convert OAuth2 attributes to our normalized OAuth2UserInfo DTO
        // oAuth2User.getAttributes() contains data from Facebook's UserInfo endpoint
        // Example: {"id": "123456789", "name": "John Doe", "email": "user@facebook.com", ...}
        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(registrationId, oAuth2User.getAttributes());
        
        if (userInfo.getEmail() == null || userInfo.getEmail().isEmpty()) {
            log.error("Email not found from OAuth2 provider: {}", registrationId);
            throw new OAuth2AuthenticationException("Email not found from OAuth2 provider");
        }
        
        User.Provider provider = getProvider(registrationId);
        
        // Find user by provider + providerId (Facebook uses "id" as providerId)
        User user = userService.findByProviderAndProviderId(provider, userInfo.getId());
        
        if (user == null) {
            // User doesn't exist with this provider, check if account exists with same email
            user = userService.findByEmail(userInfo.getEmail());
            if (user != null) {
                // Link existing account to Facebook provider
                user = updateExistingUserWithOAuth2(user, userInfo, provider);
            } else {
                // New user - create account in database
                user = userService.registerOAuth2User(userInfo, provider);
            }
        } else {
            // User exists - update profile info (name, picture, etc.)
            user = userService.updateOAuth2User(user, userInfo);
        }
        
        // Build CustomOAuth2User principal for Spring Security
        // This will be passed to OAuth2LoginSuccessHandler as Authentication.getPrincipal()
        List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
        
        return CustomOAuth2User.builder()
                .id(user.getUserUuid())  // Use USER_UUID for external identification
                .email(user.getEmail())
                .name(user.getUsername())
                .attributes(oAuth2User.getAttributes())  // Original Facebook attributes
                .authorities(authorities)
                .build();
    }
    
    private User.Provider getProvider(String registrationId) {
        switch (registrationId.toLowerCase()) {
            case "google":
                return User.Provider.GOOGLE;
            case "facebook":
                return User.Provider.FACEBOOK;
            default:
                throw new OAuth2AuthenticationException("Unsupported OAuth2 provider: " + registrationId);
        }
    }
    
    private User updateExistingUserWithOAuth2(User existingUser, OAuth2UserInfo userInfo, User.Provider provider) {
        log.info("Updating existing user with OAuth2 provider info: {} (USER_ID: {})", 
                existingUser.getEmail(), existingUser.getId());
        
        existingUser.setProvider(provider);
        existingUser.setProviderId(userInfo.getId());
        existingUser.setProfilePicture(userInfo.getImageUrl());
        existingUser.setEmailVerified(true);
        existingUser.setUsrLastModification("system");  // Update audit field
        
        return userService.updateOAuth2User(existingUser, userInfo);
    }
}
