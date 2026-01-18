package vn.ttg.roadmap.chatapi.userservice.controller;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.ttg.roadmap.chatapi.userservice.dto.CustomOAuth2User;
import vn.ttg.roadmap.chatapi.userservice.entity.User;
import vn.ttg.roadmap.chatapi.userservice.security.JwtTokenProvider;
import vn.ttg.roadmap.chatapi.userservice.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class OAuth2Controller {
    
    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    
    @GetMapping("/user")
    public ResponseEntity<UserInfo> getCurrentUser(@AuthenticationPrincipal CustomOAuth2User principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        
        User user = userService.findByEmail(principal.getEmail());
        if (user == null) {
            return ResponseEntity.status(404).build();
        }
        
        UserInfo userInfo = UserInfo.builder()
                .id(user.getUserUuid())  // Use USER_UUID for external identification
                .userId(user.getId())  // Include internal USER_ID if needed
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .profilePicture(user.getProfilePicture())
                .provider(user.getProvider().name())
                .emailVerified(user.getEmailVerified())
                .status(user.getStatus().name())
                .createdAt(user.getDteCreation())
                .lastModified(user.getDteLastModification())
                .build();
        
        return ResponseEntity.ok(userInfo);
    }
    
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        // In a stateless JWT implementation, logout is handled on the client side
        // by removing the token from storage
        log.info("User logout requested");
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refreshToken(@RequestBody RefreshTokenRequest request) {
        try {
            String newToken = jwtTokenProvider.refreshToken(request.getRefreshToken());
            return ResponseEntity.ok(new TokenResponse(newToken));
        } catch (Exception e) {
            log.error("Token refresh failed", e);
            return ResponseEntity.status(401).build();
        }
    }
    
    @GetMapping("/user/{userUuid}")
    public ResponseEntity<UserInfo> getUserByUuid(@PathVariable String userUuid) {
        try {
            Optional<User> user = userService.findByUserUuidOptional(userUuid);
            
            if (user.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            User userEntity = user.get();
            UserInfo userInfo = UserInfo.builder()
                    .id(userEntity.getUserUuid())  // Use USER_UUID for external identification
                    .userId(userEntity.getId())  // Include internal USER_ID if needed
                    .username(userEntity.getUsername())
                    .email(userEntity.getEmail())
                    .firstName(userEntity.getFirstName())
                    .lastName(userEntity.getLastName())
                    .profilePicture(userEntity.getProfilePicture())
                    .provider(userEntity.getProvider().name())
                    .emailVerified(userEntity.getEmailVerified())
                    .status(userEntity.getStatus().name())
                    .createdAt(userEntity.getDteCreation())
                    .lastModified(userEntity.getDteLastModification())
                    .build();
            
            return ResponseEntity.ok(userInfo);
        } catch (Exception e) {
            log.error("Error retrieving user with UUID: {}", userUuid, e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    @Data
    @Builder
    public static class UserInfo {
        private String id;  // USER_UUID for external use
        private Integer userId;  // USER_ID for internal reference
        private String username;
        private String email;
        private String firstName;
        private String lastName;
        private String profilePicture;
        private String provider;
        private Boolean emailVerified;
        private String status;
        private java.time.Instant createdAt;
        private java.time.Instant lastModified;
    }
    
    @Data
    public static class TokenResponse {
        private String accessToken;
        
        public TokenResponse(String accessToken) {
            this.accessToken = accessToken;
        }
    }
    
    @Data
    public static class RefreshTokenRequest {
        private String refreshToken;
    }
}
