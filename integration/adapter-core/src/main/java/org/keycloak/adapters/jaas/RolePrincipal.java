package org.keycloak.adapters.jaas;

import java.io.Serializable;
import java.security.Principal;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class RolePrincipal implements Principal, Serializable {

    private static final long serialVersionUID = -5538962177019315447L;
    private String roleName = null;

    public RolePrincipal(String roleName) {
        this.roleName = roleName;
    }

    public boolean equals (Object p) {
        if (! (p instanceof RolePrincipal))
            return false;
        return getName().equals(((RolePrincipal)p).getName());
    }

    public int hashCode () {
        return getName().hashCode();
    }

    public String getName () {
        return this.roleName;
    }

    public String toString ()
    {
        return getName();
    }

}
