package org.keycloak.adapters.springsecurity.management;

import javax.servlet.http.HttpSession;
import java.util.Collection;

/**
 * Defines a session management strategy.
 *
 * @author <a href="mailto:srossillo@smartling.com">Scott Rossillo</a>
 * @version $Revision: 1 $
 */
public interface SessionManagementStrategy {

    /**
     * Removes all sessions.
     */
    void clear();

    /**
     * Returns a collection containing all sessions.
     *
     * @return a <code>Collection</code> of all known <code>HttpSession</code>s, if any;
     * an empty <code>Collection</code> otherwise
     */
    Collection<HttpSession> getAll();

    /**
     * Stores the given session.
     *
     * @param session the <code>HttpSession</code> to store (required)
     */
    void store(HttpSession session);

    /**
     * The unique identifier for the session to remove.
     *
     * @param id the unique identifier for the session to remove (required)
     * @return the <code>HttpSession</code> if it exists; <code>null</code> otherwise
     */
    HttpSession remove(String id);
}
