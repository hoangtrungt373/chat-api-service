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

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {
    
    private final UserService userService;
    
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        log.info("Loading OAuth2 user for registration: {}", userRequest.getClientRegistration().getRegistrationId());
        
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);
        
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        
        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(registrationId, oAuth2User.getAttributes());
        
        if (userInfo.getEmail() == null || userInfo.getEmail().isEmpty()) {
            log.error("Email not found from OAuth2 provider: {}", registrationId);
            throw new OAuth2AuthenticationException("Email not found from OAuth2 provider");
        }
        
        User.Provider provider = getProvider(registrationId);
        User user = userService.findByProviderAndProviderId(provider, userInfo.getId());
        
        if (user == null) {
            // Check if user exists with same email
            user = userService.findByEmail(userInfo.getEmail());
            if (user != null) {
                // Update existing user with OAuth2 provider info
                user = updateExistingUserWithOAuth2(user, userInfo, provider);
            } else {
                // Create new user
                user = userService.registerOAuth2User(userInfo, provider);
            }
        } else {
            // Update existing OAuth2 user
            user = userService.updateOAuth2User(user, userInfo);
        }
        
        List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
        
        return CustomOAuth2User.builder()
                .id(user.getUserUuid())  // Use USER_UUID for external identification
                .email(user.getEmail())
                .name(user.getUsername())
                .attributes(oAuth2User.getAttributes())
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
