package vn.ttg.roadmap.chatapi.userservice.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import vn.ttg.roadmap.chatapi.userservice.dto.CustomOAuth2User;
import vn.ttg.roadmap.chatapi.userservice.entity.User;
import vn.ttg.roadmap.chatapi.userservice.security.JwtTokenProvider;
import vn.ttg.roadmap.chatapi.userservice.service.StateTokenService;
import vn.ttg.roadmap.chatapi.userservice.service.UserService;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * OAuth2 Login Success Handler - Option 3: State Parameter
 * 
 * Uses StateTokenService which can be implemented with:
 * - Caffeine (in-memory cache)
 * - Spring Cache (simple cache)
 * - Database (PostgreSQL)
 * - Simple Memory Map (no dependencies)
 * 
 * To use a specific implementation, make sure only one StateTokenService
 * implementation is active (use @Primary or @Qualifier)
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
    
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, 
                                      HttpServletResponse response, 
                                      Authentication authentication) throws IOException, ServletException {
        
        log.info("OAuth2 login successful for user: {}", authentication.getName());
        
        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getEmail();
        
        User user = userService.findByEmail(email);
        if (user == null) {
            log.error("User not found after OAuth2 authentication: {}", email);
            response.sendRedirect(frontendUrl + "/login?error=user_not_found");
            return;
        }
        
        // Update status
        userService.updateStatus(user.getId(), User.UserStatus.ONLINE);
        
        // Generate JWT tokens
        String accessToken = jwtTokenProvider.generateToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);
        
        log.info("Generated tokens for user: {} (USER_ID: {}, USER_UUID: {})", 
                user.getEmail(), user.getId(), user.getUserUuid());
        
        // Generate state token
        String stateToken = stateTokenService.generateStateToken();
        
        // Store tokens in cache/storage with state token as key
        Map<String, String> tokenData = new HashMap<>();
        tokenData.put("accessToken", accessToken);
        tokenData.put("refreshToken", refreshToken);
        tokenData.put("userId", user.getUserUuid());  // Use USER_UUID for external identification
        tokenData.put("username", user.getUsername());
        tokenData.put("email", user.getEmail());
        
        stateTokenService.storeTokenData(stateToken, tokenData, stateTokenExpirationSeconds);
        
        log.debug("Stored tokens with state token: {}", stateToken);
        
        // Redirect to frontend with state token only (no sensitive data)
        response.sendRedirect(frontendUrl + "/auth/callback?state=" + stateToken);
    }
}

