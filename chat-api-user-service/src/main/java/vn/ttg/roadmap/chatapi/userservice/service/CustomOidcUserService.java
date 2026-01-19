package vn.ttg.roadmap.chatapi.userservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.ttg.roadmap.chatapi.userservice.dto.OAuth2UserInfo;
import vn.ttg.roadmap.chatapi.userservice.dto.OAuth2UserInfoFactory;
import vn.ttg.roadmap.chatapi.userservice.entity.User;
import vn.ttg.roadmap.chatapi.userservice.handler.OAuth2LoginSuccessHandler;

/**
 * Custom OIDC User Service for handling Google OAuth2/OIDC logins.
 * 
 * <h3>OAuth2 Flow Position:</h3>
 * This service is called AFTER Spring Security has:
 * <ol>
 *   <li>Received authorization code from Google (via redirect)</li>
 *   <li>Exchanged authorization code for access_token + id_token (automatic)</li>
 *   <li>Validated ID token signature and claims (automatic)</li>
 * </ol>
 * 
 * <h3>What This Service Does:</h3>
 * <ol>
 *   <li>Calls parent {@link OidcUserService#loadUser(OidcUserRequest)} which:
 *       <ul>
 *         <li>Calls Google's UserInfo endpoint: GET https://www.googleapis.com/oauth2/v2/userinfo</li>
 *         <li>Uses access_token from userRequest: Authorization: Bearer {accessToken}</li>
 *         <li>Extracts user claims (email, name, picture, sub, etc.)</li>
 *         <li>Builds DefaultOidcUser with validated claims</li>
 *       </ul>
 *   </li>
 *   <li>Creates/updates User record in PostgreSQL database</li>
 *   <li>Returns OidcUser principal for Spring Security</li>
 * </ol>
 * 
 * <h3>Flow Sequence:</h3>
 * <pre>
 * 1. User authenticates with Google
 * 2. Google redirects: /login/oauth2/code/google?code=AUTHORIZATION_CODE
 * 3. Spring Security exchanges code for tokens (automatic)
 * 4. Spring Security calls this service: loadUser(userRequest)
 * 5. This service calls super.loadUser() → Google UserInfo API called
 * 6. User created/updated in database
 * 7. OAuth2LoginSuccessHandler called (generates YOUR JWTs)
 * </pre>
 * 
 * @see OidcUserService Spring Security's base OIDC service
 * @see OAuth2LoginSuccessHandler Called after this service completes
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CustomOidcUserService extends OidcUserService {

    private final UserService userService;

    /**
     * Loads OIDC user information and creates/updates user in database.
     * 
     * <h3>When Called:</h3>
     * Called by Spring Security AFTER authorization code has been exchanged for tokens.
     * The userRequest already contains:
     * <ul>
     *   <li>{@code userRequest.getAccessToken()} - Google's access token (already obtained)</li>
     *   <li>{@code userRequest.getIdToken()} - Google's ID token (already validated)</li>
     * </ul>
     * 
     * <h3>What Happens:</h3>
     * <ol>
     *   <li><b>Line 34:</b> Calls parent {@code super.loadUser(userRequest)} which:
     *       <ul>
     *         <li>Makes HTTP GET request to Google's UserInfo endpoint</li>
     *         <li>URL: {@code https://www.googleapis.com/oauth2/v2/userinfo} (from application.yml)</li>
     *         <li>Headers: {@code Authorization: Bearer {accessToken}}</li>
     *         <li>Parses response and builds OidcUser with claims</li>
     *       </ul>
     *   </li>
     *   <li><b>Line 40:</b> Extracts user info from OIDC claims</li>
     *   <li><b>Line 49-60:</b> Creates or updates User in PostgreSQL</li>
     *   <li><b>Line 66:</b> Returns OidcUser (Spring Security needs this)</li>
     * </ol>
     * 
     * @param userRequest Contains access_token and id_token (already exchanged by Spring Security)
     * @return OidcUser with user claims from Google
     * @throws OAuth2AuthenticationException if email not found or user processing fails
     */
    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        // ⚠️ IMPORTANT: At this point, Spring Security has already:
        // 1. Exchanged authorization code for access_token + id_token
        // 2. Validated ID token signature, expiration, issuer, audience
        // 3. userRequest.getAccessToken() contains Google's access token
        
        // Delegate to Spring Security's default OidcUserService implementation
        // This will:
        // - Call Google's UserInfo endpoint: GET https://www.googleapis.com/oauth2/v2/userinfo
        // - Use access_token: Authorization: Bearer {accessToken}
        // - Extract user claims (email, name, picture, sub, etc.)
        // - Build DefaultOidcUser with validated claims
        OidcUser oidcUser = super.loadUser(userRequest);

        
        // Now we have user info from Google (email, name, picture, sub, etc.)
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        log.info("Loading OIDC user for registration: {}", registrationId);

        // Convert OIDC attributes to our normalized OAuth2UserInfo DTO
        // oidcUser.getAttributes() contains claims from Google's UserInfo endpoint
        // Example: {"sub": "123456789", "email": "user@gmail.com", "name": "John Doe", ...}
        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(registrationId, oidcUser.getAttributes());
        if (userInfo.getEmail() == null || userInfo.getEmail().isEmpty()) {
            log.error("Email not found from OIDC provider: {}", registrationId);
            throw new OAuth2AuthenticationException("Email not found from OIDC provider");
        }

        User.Provider provider = getProvider(registrationId);

        // Find user by provider + providerId (Google uses "sub" claim as providerId)
        // This allows users to login with the same Google account multiple times
        User user = userService.findByProviderAndProviderId(provider, userInfo.getId());
        if (user == null) {
            // User doesn't exist with this provider, check if account exists with same email
            // (e.g., user previously registered with email/password, now linking Google)
            user = userService.findByEmail(userInfo.getEmail());
            if (user != null) {
                // Link existing account to Google provider
                user = linkExistingUser(user, userInfo, provider);
            } else {
                // New user - create account in database
                user = userService.registerOAuth2User(userInfo, provider);
            }
        } else {
            // User exists - update profile info (name, picture, etc.)
            user = userService.updateOAuth2User(user, userInfo);
        }

        log.info("OIDC user processed: {} (USER_ID: {}, USER_UUID: {})",
                user.getEmail(), user.getId(), user.getUserUuid());

        // ⚠️ IMPORTANT: Must return OidcUser (not CustomOAuth2User) for OIDC flows
        // Spring Security expects OidcUser for OIDC providers (Google)
        // This OidcUser will be passed to OAuth2LoginSuccessHandler as Authentication.getPrincipal()
        return oidcUser;
    }

    private User.Provider getProvider(String registrationId) {
        return switch (registrationId.toLowerCase()) {
            case "google" -> User.Provider.GOOGLE;
            case "facebook" -> User.Provider.FACEBOOK;
            default -> throw new OAuth2AuthenticationException("Unsupported OAuth2 provider: " + registrationId);
        };
    }

    private User linkExistingUser(User existingUser, OAuth2UserInfo userInfo, User.Provider provider) {
        log.info("Linking existing user to provider: {} (USER_ID: {})",
                existingUser.getEmail(), existingUser.getId());

        existingUser.setProvider(provider);
        existingUser.setProviderId(userInfo.getId());
        existingUser.setProfilePicture(userInfo.getImageUrl());
        existingUser.setEmailVerified(true);
        existingUser.setUsrLastModification("system");

        return userService.updateOAuth2User(existingUser, userInfo);
    }
}

