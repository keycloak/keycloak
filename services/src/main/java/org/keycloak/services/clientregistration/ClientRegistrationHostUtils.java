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

package org.keycloak.services.clientregistration;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import org.jboss.logging.Logger;
import org.keycloak.models.ClientRegistrationTrustedHostModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ClientRegistrationHostUtils {

    private static final Logger logger = Logger.getLogger(ClientRegistrationHostUtils.class);

    /**
     * @return null if host from request is not trusted. Otherwise return trusted host model
     */
    public static ClientRegistrationTrustedHostModel getTrustedHost(String hostAddress, KeycloakSession session, RealmModel realm) {
        logger.debugf("Verifying remote host : %s", hostAddress);

        List<ClientRegistrationTrustedHostModel> trustedHosts = session.sessions().listClientRegistrationTrustedHosts(realm);

        for (ClientRegistrationTrustedHostModel realmTrustedHost : trustedHosts) {
            try {
                if (realmTrustedHost.getRemainingCount() <= 0) {
                    continue;
                }

                String realmHostIPAddress = InetAddress.getByName(realmTrustedHost.getHostName()).getHostAddress();
                logger.debugf("Trying host '%s' of address '%s'", realmTrustedHost.getHostName(), realmHostIPAddress);
                if (realmHostIPAddress.equals(hostAddress)) {
                    logger.debugf("Successfully verified host : %s", realmTrustedHost.getHostName());
                    return realmTrustedHost;
                }
            } catch (UnknownHostException uhe) {
                logger.debugf("Unknown host from realm configuration: %s", realmTrustedHost.getHostName());
            }
        }

        logger.debugf("Failed to verify remote host : %s", hostAddress);
        return null;
    }

}
