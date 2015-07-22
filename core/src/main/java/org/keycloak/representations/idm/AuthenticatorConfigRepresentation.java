package org.keycloak.representations.idm;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
* @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
* @version $Revision: 1 $
*/
public class AuthenticatorConfigRepresentation implements Serializable {
    private static final long serialVersionUID = 1L;

    private String alias;
    private Map<String, String> config = new HashMap<String, String>();


    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }



    public Map<String, String> getConfig() {
        return config;
    }

    public void setConfig(Map<String, String> config) {
        this.config = config;
    }
}
