/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.keycloak.testsuite.util;

import org.junit.Assume;
import org.keycloak.testsuite.arquillian.AuthServerTestEnricher;

public class ContainerAssume {

    public static void assumeNotAuthServerUndertow() {
        Assume.assumeFalse("Doesn't work on auth-server-undertow", 
                AuthServerTestEnricher.AUTH_SERVER_CONTAINER.equals(AuthServerTestEnricher.AUTH_SERVER_CONTAINER_DEFAULT));
    }

}
