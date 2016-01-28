package prefuse.util.io;

import java.net.Authenticator;
import java.net.PasswordAuthentication;

/**
 * A basic username/password authenticator for use with HTTP-Auth.
 * The username or password can be reset for subsequent use as a different
 * user or on a different website.
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class PasswordAuthenticator extends Authenticator {

    private String m_username;
    private String m_password;
    private PasswordAuthentication m_auth;
    
    /**
     * Create a new password authenticator.
     * @param username the user name
     * @param password the password
     */
    PasswordAuthenticator(String username, String password) {
        this.m_password = password;
        this.m_username = username;
    }
    
    /**
     * Get the password.
     * @return the password
     */
    String getPassword() {
        return m_password;
    }

    /**
     * Set the password.
     * @return the password to use
     */
    void setPassword(String password) {
        this.m_password = password;
        this.m_auth = null;
    }

    /**
     * Get the user name.
     * @return the user name
     */
    String getUsername() {
        return m_username;
    }
    
    /**
     * Set the user name.
     * @return the user name to use
     */
    void setUsername(String username) {
        this.m_username = username;
        this.m_auth = null;
    }

    /**
     * Get the singleton PasswordAuthentication instance.
     * @return the PasswordAuthentication instance
     */
    protected PasswordAuthentication getPasswordAuthentication() {
        if ( m_auth == null ) {
            m_auth = new PasswordAuthentication(
                        m_username, m_password.toCharArray());
        }
        return m_auth;
    }

    // ------------------------------------------------------------------------
    
    /**
     * Creates a new PasswordAuthenticator for the given name and password and
     * sets it as the default Authenticator for use within Java networking.
     */
    public static void setAuthentication(String username, String password) {
        Authenticator.setDefault(new PasswordAuthenticator(username,password));
    }
    
} // end of class PasswordAuthenticator
