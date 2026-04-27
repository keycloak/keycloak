package org.keycloak.scim.client.authorization;

import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.http.simple.SimpleHttpRequest;

public interface AuthorizationMethod {

    void onBefore(SimpleHttp http, SimpleHttpRequest request);
}
