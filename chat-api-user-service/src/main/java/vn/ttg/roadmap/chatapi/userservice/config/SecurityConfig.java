package vn.ttg.roadmap.chatapi.userservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

import lombok.RequiredArgsConstructor;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import vn.ttg.roadmap.chatapi.userservice.handler.OAuth2LoginSuccessHandler;
import vn.ttg.roadmap.chatapi.userservice.security.JwtAuthenticationFilter;
import vn.ttg.roadmap.chatapi.userservice.service.CustomOAuth2UserService;
import vn.ttg.roadmap.chatapi.userservice.service.CustomOidcUserService;

/**
 * Spring Security Configuration for OAuth2/OIDC Authentication.
 * 
 * <h3>OAuth2 Flow Configuration:</h3>
 * <ol>
 *   <li><b>Authorization Request:</b> User redirected to OAuth2 provider (Google/Facebook)</li>
 *   <li><b>Authorization Code:</b> Provider redirects back with authorization code</li>
 *   <li><b>Token Exchange:</b> Spring Security automatically exchanges code for tokens</li>
 *   <li><b>User Info Loading:</b> CustomOidcUserService or CustomOAuth2UserService called</li>
 *   <li><b>Success Handler:</b> OAuth2LoginSuccessHandler generates YOUR JWTs</li>
 * </ol>
 * 
 * <h3>Key Components:</h3>
 * <ul>
 *   <li><b>CustomOidcUserService:</b> Handles Google OIDC logins (calls Google UserInfo API)</li>
 *   <li><b>CustomOAuth2UserService:</b> Handles Facebook OAuth2 logins (calls Facebook UserInfo API)</li>
 *   <li><b>OAuth2LoginSuccessHandler:</b> Generates YOUR JWT tokens and redirects to frontend</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final CustomOAuth2UserService customOAuth2UserService;
    private final CustomOidcUserService customOidcUserService;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final CorsConfigurationSource corsConfigurationSource;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * Configures Spring Security filter chain with OAuth2/OIDC support.
     * 
     * <h3>OAuth2 Login Configuration:</h3>
     * <ul>
     *   <li><b>userService:</b> For OAuth2 providers (Facebook) - calls Facebook UserInfo API</li>
     *   <li><b>oidcUserService:</b> For OIDC providers (Google) - calls Google UserInfo API</li>
     *   <li><b>successHandler:</b> Called after user info is loaded - generates YOUR JWTs</li>
     * </ul>
     * 
     * <h3>Flow Sequence:</h3>
     * <pre>
     * 1. User clicks "Login with Google" → /oauth2/authorization/google
     * 2. Spring Security redirects to Google authorization page
     * 3. User authenticates → Google redirects: /login/oauth2/code/google?code=...
     * 4. Spring Security exchanges code for tokens (automatic)
     * 5. Spring Security calls oidcUserService.loadUser() → Google UserInfo API called
     * 6. Spring Security calls successHandler.onAuthenticationSuccess() → YOUR JWTs generated
     * </pre>
     * 
     * @param http HttpSecurity builder
     * @return Configured SecurityFilterChain
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                // Allow CORS preflight requests (OPTIONS)
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // Allow OAuth2 endpoints (authorization, callback, token exchange)
                .requestMatchers("/api/v1/auth/exchange-state", "/api/v1/auth/oauth2/**", "/login/**", "/oauth2/**").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/api/v1/users/public/**").permitAll()
                // Require authentication for protected endpoints
                .requestMatchers("/api/v1/auth/user").authenticated()
                .anyRequest().authenticated()
            )
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            // Add JWT authentication filter before other filters
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            // Configure OAuth2 Login flow
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo
                    // For OAuth2 providers (Facebook) - uses DefaultOAuth2UserService internally
                    // This service calls Facebook's UserInfo endpoint: GET https://graph.facebook.com/v18.0/me
                    .userService(customOAuth2UserService)
                    // For OIDC providers (Google) - uses OidcUserService internally
                    // This service calls Google's UserInfo endpoint: GET https://www.googleapis.com/oauth2/v2/userinfo
                    .oidcUserService(customOidcUserService)
                )
                // Called AFTER user info is loaded and user is created/updated in database
                // Generates YOUR OWN JWT tokens and redirects to frontend with state token
                .successHandler(oAuth2LoginSuccessHandler)
            )
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .headers(headers -> headers
                .frameOptions().disable()
            );
        
        return http.build();
    }
}
