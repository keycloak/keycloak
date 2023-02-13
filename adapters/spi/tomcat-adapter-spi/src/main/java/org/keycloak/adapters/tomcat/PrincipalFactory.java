package org.keycloak.adapters.tomcat;

import org.apache.catalina.Realm;
import org.apache.catalina.realm.GenericPrincipal;

import java.security.Principal;
import java.util.Set;

public interface PrincipalFactory {
    GenericPrincipal createPrincipal(Realm realm, final Principal identity, final Set<String> roleSet);
}
