package vn.ttg.roadmap.chatapi.userservice.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import vn.ttg.roadmap.chatapi.userservice.dto.CustomOAuth2User;

/**
 * Utility class for getting current user information from Spring Security context
 */
@Slf4j
public class UserUtils {
    
    private static final String DEFAULT_USER = "system";
    
    /**
     * Get the current username from Spring Security context
     * 
     * @return username if authenticated, otherwise "system"
     */
    public static String getUserName() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || !authentication.isAuthenticated()) {
                return DEFAULT_USER;
            }
            
            Object principal = authentication.getPrincipal();
            
            // Handle CustomOAuth2User (OAuth2 login)
            if (principal instanceof CustomOAuth2User) {
                CustomOAuth2User oAuth2User = (CustomOAuth2User) principal;
                return oAuth2User.getName() != null ? oAuth2User.getName() : oAuth2User.getEmail();
            }
            
            // Handle JWT token (JWT authentication)
            if (principal instanceof Jwt) {
                Jwt jwt = (Jwt) principal;
                String username = jwt.getClaimAsString("username");
                if (username != null) {
                    return username;
                }
                // Fallback to subject (email)
                return jwt.getSubject() != null ? jwt.getSubject() : DEFAULT_USER;
            }
            
            // Handle String principal (username)
            if (principal instanceof String) {
                return (String) principal;
            }
            
            // Fallback to authentication name
            String name = authentication.getName();
            return name != null && !name.equals("anonymousUser") ? name : DEFAULT_USER;
            
        } catch (Exception e) {
            log.warn("Error getting current user from security context: {}", e.getMessage());
            return DEFAULT_USER;
        }
    }
    
    /**
     * Get the current user email from Spring Security context
     * 
     * @return email if available, otherwise null
     */
    public static String getUserEmail() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || !authentication.isAuthenticated()) {
                return null;
            }
            
            Object principal = authentication.getPrincipal();
            
            // Handle CustomOAuth2User (OAuth2 login)
            if (principal instanceof CustomOAuth2User) {
                CustomOAuth2User oAuth2User = (CustomOAuth2User) principal;
                return oAuth2User.getEmail();
            }
            
            // Handle JWT token (JWT authentication)
            if (principal instanceof Jwt) {
                Jwt jwt = (Jwt) principal;
                String email = jwt.getClaimAsString("email");
                if (email != null) {
                    return email;
                }
                // Fallback to subject (email)
                return jwt.getSubject();
            }
            
            return null;
            
        } catch (Exception e) {
            log.warn("Error getting current user email from security context: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Get the current user ID (USER_UUID) from Spring Security context
     * 
     * @return user UUID if available, otherwise null
     */
    public static String getUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || !authentication.isAuthenticated()) {
                return null;
            }
            
            Object principal = authentication.getPrincipal();
            
            // Handle CustomOAuth2User (OAuth2 login)
            if (principal instanceof CustomOAuth2User) {
                CustomOAuth2User oAuth2User = (CustomOAuth2User) principal;
                return oAuth2User.getId();
            }
            
            // Handle JWT token (JWT authentication)
            if (principal instanceof Jwt) {
                Jwt jwt = (Jwt) principal;
                return jwt.getClaimAsString("userId");
            }
            
            return null;
            
        } catch (Exception e) {
            log.warn("Error getting current user ID from security context: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Check if there is an authenticated user
     * 
     * @return true if user is authenticated, false otherwise
     */
    public static boolean isAuthenticated() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            return authentication != null && authentication.isAuthenticated() 
                    && !authentication.getName().equals("anonymousUser");
        } catch (Exception e) {
            return false;
        }
    }
}

