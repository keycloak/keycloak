package org.keycloak.adapters.authorization.ceip;

import java.util.Set;

public interface CustomEnforcerResource {

     String getResourceName();
     Set<String> getScopes();
}
