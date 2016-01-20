package org.keycloak.adapters.saml.config;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class KeycloakSamlAdapter implements Serializable {
    private List<SP> sps = new LinkedList<>();

    public List<SP> getSps() {
        return sps;
    }

    public void setSps(List<SP> sps) {
        this.sps = sps;
    }
}
