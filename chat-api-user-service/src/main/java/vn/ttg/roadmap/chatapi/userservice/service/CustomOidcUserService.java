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

/**
 * Handles OIDC logins (e.g. Google "openid" scope).
 *
 * Spring Security uses {@link OidcUserService} for OIDC providers and returns an {@link OidcUser}
 * principal (usually {@code DefaultOidcUser}). This service hooks into that flow to create/update
 * our {@link User} record in the database.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CustomOidcUserService extends OidcUserService {

    private final UserService userService;

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        // Delegate to default implementation to validate tokens and build the OidcUser
        OidcUser oidcUser = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        log.info("Loading OIDC user for registration: {}", registrationId);

        // Convert OIDC attributes to our normalized OAuth2UserInfo
        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(registrationId, oidcUser.getAttributes());
        if (userInfo.getEmail() == null || userInfo.getEmail().isEmpty()) {
            log.error("Email not found from OIDC provider: {}", registrationId);
            throw new OAuth2AuthenticationException("Email not found from OIDC provider");
        }

        User.Provider provider = getProvider(registrationId);

        // Find by provider + providerId (Google uses "sub")
        User user = userService.findByProviderAndProviderId(provider, userInfo.getId());
        if (user == null) {
            // If an account exists with same email, link it; otherwise create new
            user = userService.findByEmail(userInfo.getEmail());
            if (user != null) {
                user = linkExistingUser(user, userInfo, provider);
            } else {
                user = userService.registerOAuth2User(userInfo, provider);
            }
        } else {
            user = userService.updateOAuth2User(user, userInfo);
        }

        log.info("OIDC user processed: {} (USER_ID: {}, USER_UUID: {})",
                user.getEmail(), user.getId(), user.getUserUuid());

        // Important: must return an OidcUser for OIDC flows
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

