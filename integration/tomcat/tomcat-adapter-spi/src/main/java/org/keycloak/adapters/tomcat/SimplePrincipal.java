package org.keycloak.adapters.tomcat;

import java.io.Serializable;
import java.security.Principal;

/**
 * Simple security principal implementation.
 *
 * @author Marvin S. Addison
 * @version $Revision: 22071 $
 * @since 3.1.11
 *
 */
public class SimplePrincipal implements Principal, Serializable {

    /** SimplePrincipal.java */
    private static final long serialVersionUID = -5645357206342793145L;

    /** The unique identifier for this principal. */
    private final String name;

    /**
     * Creates a new principal with the given name.
     * @param name Principal name.
     */
    public SimplePrincipal(final String name) {
        this.name = name;
    }

    public final String getName() {
        return this.name;
    }

    public String toString() {
        return getName();
    }

    public boolean equals(final Object o) {
        if (o == null) {
            return false;
        } else if (!(o instanceof SimplePrincipal)) {
            return false;
        } else {
            return getName().equals(((SimplePrincipal)o).getName());
        }
    }

    public int hashCode() {
        return 37 * getName().hashCode();
    }
}