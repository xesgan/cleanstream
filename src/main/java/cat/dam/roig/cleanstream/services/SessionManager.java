package cat.dam.roig.cleanstream.services;

import cat.dam.roig.cleanstream.models.Usuari;

/**
 *
 * @author metku
 */
public class SessionManager {
    
    private String token;
    private Usuari user;
    
    private static final SessionManager instance = new SessionManager();
    
    public static SessionManager get() {
        return instance;
    }
    
    public void setSession(String token, Usuari user) {
        this.token = token;
        this.user = user;
    }
    
    public String getToken() {
        return token;
    }
    
    public Usuari getUser() {
        return user;
    }
    
    public boolean isLogged() {
        return token != null && !token.isBlank();
    }
    
    public void clear() {
        this.token = null;
        this.user = null;
    }
}
