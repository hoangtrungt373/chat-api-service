package vn.ttg.roadmap.chatapi.userservice.service;

import java.util.Map;
import java.util.UUID;

/**
 * Service interface for managing OAuth2 state tokens
 * Implementations can use different storage backends
 */
public interface StateTokenService {
    
    /**
     * Store token data with a state token
     * @param stateToken The state token (UUID)
     * @param tokenData Map containing accessToken, refreshToken, userId, etc.
     * @param expirationSeconds How long the token should be valid (may be ignored by implementation)
     * @return The stored token data (for caching purposes)
     */
    Map<String, String> storeTokenData(String stateToken, Map<String, String> tokenData, long expirationSeconds);
    
    /**
     * Retrieve token data by state token
     * @param stateToken The state token
     * @return Token data map, or null if not found/expired
     */
    Map<String, String> getTokenData(String stateToken);
    
    /**
     * Delete token data (one-time use)
     * @param stateToken The state token
     */
    void deleteTokenData(String stateToken);
    
    /**
     * Generate a new state token
     * @return UUID string
     */
    default String generateStateToken() {
        return UUID.randomUUID().toString();
    }
}

