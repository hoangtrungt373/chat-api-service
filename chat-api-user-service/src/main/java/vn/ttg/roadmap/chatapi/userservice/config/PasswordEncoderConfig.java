package vn.ttg.roadmap.chatapi.userservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Password Encoder Configuration
 * 
 * <p>Separated from SecurityConfig to avoid circular dependency:
 * SecurityConfig → CustomOAuth2UserService → UserService → PasswordEncoder
 * 
 * @author ttg
 */
@Configuration
public class PasswordEncoderConfig {
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
