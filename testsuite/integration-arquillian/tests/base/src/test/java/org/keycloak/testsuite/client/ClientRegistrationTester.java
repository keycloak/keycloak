/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.testsuite.client;

import java.util.Arrays;

import org.keycloak.client.registration.ClientRegistration;
import org.keycloak.client.registration.ClientRegistrationException;
import org.keycloak.client.registration.HttpErrorException;
import org.keycloak.representations.idm.ClientRepresentation;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ClientRegistrationTester {

    public static void main(String[] args) throws ClientRegistrationException {
        ClientRepresentation rep = createRep1();

        ClientRegistration reg = ClientRegistration.create().url("http://localhost:8081/auth", "test").build();

        try {
            ClientRepresentation createdRep = reg.create(rep);
            System.out.println("Created client: " + createdRep.getClientId());
        } catch (ClientRegistrationException ex) {
            HttpErrorException httpEx = (HttpErrorException) ex.getCause();
            System.err.println("HttpException when registering client. Status=" + httpEx.getStatusLine().getStatusCode() + ", Details=" + httpEx.getErrorResponse());
        }
    }


    private static ClientRepresentation createRep1() {
        ClientRepresentation rep = new ClientRepresentation();
        rep.setRedirectUris(Arrays.asList("http://localhost:8080/app"));
        rep.setDefaultRoles(new String[] { "foo-role" });
        return rep;
    }

}
