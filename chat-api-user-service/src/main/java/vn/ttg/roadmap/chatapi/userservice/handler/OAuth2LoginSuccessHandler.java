package vn.ttg.roadmap.chatapi.userservice.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import vn.ttg.roadmap.chatapi.userservice.controller.AuthCallbackController;
import vn.ttg.roadmap.chatapi.userservice.dto.CustomOAuth2User;
import vn.ttg.roadmap.chatapi.userservice.entity.User;
import vn.ttg.roadmap.chatapi.userservice.security.JwtTokenProvider;
import vn.ttg.roadmap.chatapi.userservice.service.CustomOAuth2UserService;
import vn.ttg.roadmap.chatapi.userservice.service.CustomOidcUserService;
import vn.ttg.roadmap.chatapi.userservice.service.StateTokenService;
import vn.ttg.roadmap.chatapi.userservice.service.UserService;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * OAuth2 Login Success Handler - Secure Token Delivery Pattern (State Parameter).
 * 
 * <h3>OAuth2 Flow Position:</h3>
 * This handler is called AFTER:
 * <ol>
 *   <li>User authenticates with OAuth2 provider (Google/Facebook)</li>
 *   <li>Spring Security exchanges authorization code for tokens</li>
 *   <li>CustomOidcUserService or CustomOAuth2UserService loads user info</li>
 *   <li>User is created/updated in database</li>
 * </ol>
 * 
 * <h3>Purpose:</h3>
 * <ul>
 *   <li>Generates YOUR OWN JWT tokens (not OAuth2 provider's tokens)</li>
 *   <li>Securely delivers tokens to frontend using state token pattern</li>
 *   <li>Prevents tokens from appearing in browser URL (security best practice)</li>
 * </ul>
 * 
 * <h3>Token Delivery Pattern (Option 3: State Parameter):</h3>
 * <pre>
 * 1. Generate state token (UUID)
 * 2. Store JWT tokens in Redis cache with state token as key
 * 3. Redirect to frontend with ONLY state token in URL
 * 4. Frontend exchanges state token for JWT tokens via API call
 * 5. State token deleted after one-time use
 * </pre>
 * 
 * <h3>Why This Pattern:</h3>
 * <ul>
 *   <li>✅ Secure: Tokens never exposed in URL</li>
 *   <li>✅ One-time use: State token deleted after exchange</li>
 *   <li>✅ TTL: Tokens expire after 5 minutes</li>
 *   <li>✅ CORS-safe: Frontend can call API to exchange token</li>
 * </ul>
 * 
 * <h3>StateTokenService Implementations:</h3>
 * Can be implemented with:
 * <ul>
 *   <li>Redis (distributed cache) - Recommended for production</li>
 *   <li>Caffeine (in-memory cache) - For single-instance apps</li>
 *   <li>Spring Cache (simple cache)</li>
 *   <li>Database (PostgreSQL)</li>
 *   <li>Simple Memory Map (no dependencies)</li>
 * </ul>
 * 
 * @see CustomOidcUserService Called before this handler
 * @see CustomOAuth2UserService Called before this handler
 * @see AuthCallbackController Frontend exchanges state token here
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {
    
    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;
    private final StateTokenService stateTokenService;
    
    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;
    
    @Value("${app.state.token.expiration:300}")  // 5 minutes default
    private long stateTokenExpirationSeconds;
    
    /**
     * Called by Spring Security after successful OAuth2/OIDC authentication.
     * 
     * <h3>Flow:</h3>
     * <ol>
     *   <li><b>Line 58:</b> Extract email from authenticated principal
     *       <ul>
     *         <li>For Google: OidcUser (from CustomOidcUserService)</li>
     *         <li>For Facebook: CustomOAuth2User (from CustomOAuth2UserService)</li>
     *       </ul>
     *   </li>
     *   <li><b>Line 65:</b> Find user in database (already created/updated by CustomOidcUserService)</li>
     *   <li><b>Line 73:</b> Update user status to ONLINE</li>
     *   <li><b>Line 76-77:</b> Generate YOUR OWN JWT tokens (not OAuth2 provider's tokens)</li>
     *   <li><b>Line 83:</b> Generate state token (UUID)</li>
     *   <li><b>Line 86-93:</b> Store JWT tokens in Redis cache with state token as key</li>
     *   <li><b>Line 98:</b> Redirect to frontend with ONLY state token (no sensitive data)</li>
     * </ol>
     * 
     * <h3>What Happens Next:</h3>
     * <ol>
     *   <li>Frontend receives redirect: /auth/callback?state={stateToken}</li>
     *   <li>Frontend calls: POST /api/v1/auth/exchange-state with state token</li>
     *   <li>Backend returns JWT tokens from Redis cache</li>
     *   <li>State token deleted (one-time use)</li>
     * </ol>
     * 
     * @param request HTTP request
     * @param response HTTP response (used for redirect)
     * @param authentication Contains OidcUser (Google) or CustomOAuth2User (Facebook)
     * @throws IOException if redirect fails
     * @throws ServletException if servlet error occurs
     */
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, 
                                      HttpServletResponse response, 
                                      Authentication authentication) throws IOException, ServletException {
        
        log.info("OAuth2 login successful for user: {}", authentication.getName());
        
        // Extract email from authenticated principal
        // Principal type depends on provider:
        // - Google: OidcUser (from CustomOidcUserService)
        // - Facebook: CustomOAuth2User (from CustomOAuth2UserService)
        String email = extractEmail(authentication.getPrincipal());
        if (email == null || email.trim().isEmpty()) {
            log.error("Email not found in OAuth2/OIDC principal");
            response.sendRedirect(frontendUrl + "/login?error=email_not_found");
            return;
        }
        
        // Find user in database
        // Note: User was already created/updated by CustomOidcUserService or CustomOAuth2UserService
        // This lookup is to get the full User entity for token generation
        User user = userService.findByEmail(email);
        if (user == null) {
            log.error("User not found after OAuth2 authentication: {}", email);
            response.sendRedirect(frontendUrl + "/login?error=user_not_found");
            return;
        }
        
        // Update user status to ONLINE
        userService.updateStatus(user.getId(), User.UserStatus.ONLINE);
        
        // ⚠️ IMPORTANT: Generate YOUR OWN JWT tokens (not OAuth2 provider's tokens)
        // These tokens are used to access YOUR API, not Google/Facebook APIs
        // OAuth2 provider's tokens are only used during authentication flow
        String accessToken = jwtTokenProvider.generateToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);
        
        log.info("Generated tokens for user: {} (USER_ID: {}, USER_UUID: {})", 
                user.getEmail(), user.getId(), user.getUserUuid());
        
        // Generate state token (UUID) - this is a temporary, one-time-use token
        // Used to securely exchange for actual JWT tokens
        String stateToken = stateTokenService.generateStateToken();
        
        // Store JWT tokens in Redis cache with state token as key
        // This allows frontend to exchange state token for actual tokens via API call
        // Tokens are stored with TTL (default 5 minutes) for security
        Map<String, String> tokenData = new HashMap<>();
        tokenData.put("accessToken", accessToken);
        tokenData.put("refreshToken", refreshToken);
        tokenData.put("userId", user.getUserUuid());  // Use USER_UUID for external identification
        tokenData.put("username", user.getUsername());
        tokenData.put("email", user.getEmail());
        
        stateTokenService.storeTokenData(stateToken, tokenData, stateTokenExpirationSeconds);
        
        log.debug("Stored tokens with state token: {}", stateToken);
        
        // ⚠️ SECURITY: Redirect to frontend with ONLY state token (no sensitive data in URL)
        // JWT tokens are NOT in the URL - they're stored in Redis cache
        // Frontend will exchange state token for tokens via secure API call
        response.sendRedirect(frontendUrl + "/auth/callback?state=" + stateToken);
    }

    /**
     * Extracts email from authenticated principal.
     * 
     * <h3>Principal Types:</h3>
     * <ul>
     *   <li><b>CustomOAuth2User:</b> Facebook OAuth2 login (from CustomOAuth2UserService)</li>
     *   <li><b>OidcUser:</b> Google OIDC login (from CustomOidcUserService)</li>
     *   <li><b>OAuth2User:</b> Generic OAuth2 user (fallback)</li>
     * </ul>
     * 
     * <h3>Email Extraction Priority (for OidcUser):</h3>
     * <ol>
     *   <li>oidcUser.getEmail() - Direct accessor</li>
     *   <li>idToken.getClaimAsString("email") - From ID token claims</li>
     *   <li>userInfo.getEmail() - From UserInfo endpoint response</li>
     * </ol>
     * 
     * @param principal The authenticated principal (OidcUser, CustomOAuth2User, or OAuth2User)
     * @return Email address if found, null otherwise
     */
    private String extractEmail(Object principal) {
        // Handle Facebook OAuth2 login (CustomOAuth2User)
        if (principal instanceof CustomOAuth2User customOAuth2User) {
            return customOAuth2User.getEmail();
        }
        
        // Handle Google OIDC login (OidcUser)
        if (principal instanceof OidcUser oidcUser) {
            // Try direct accessor first (most reliable)
            String email = oidcUser.getEmail();
            if (email == null || email.trim().isEmpty()) {
                // Fallback to ID token claims (email claim from Google)
                email = oidcUser.getIdToken() != null ? oidcUser.getIdToken().getClaimAsString("email") : null;
            }
            if ((email == null || email.trim().isEmpty()) && oidcUser.getUserInfo() != null) {
                // Fallback to UserInfo endpoint response (from Google's UserInfo API)
                email = oidcUser.getUserInfo().getEmail();
            }
            return email;
        }
        
        // Handle generic OAuth2User (fallback)
        if (principal instanceof OAuth2User oAuth2User) {
            Object emailObj = oAuth2User.getAttributes().get("email");
            return emailObj != null ? emailObj.toString() : null;
        }
        
        return null;
    }
}

