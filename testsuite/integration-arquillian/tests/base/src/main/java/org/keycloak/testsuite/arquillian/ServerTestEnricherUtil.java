/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.arquillian;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.jboss.logging.Logger;
import org.wildfly.extras.creaper.commands.undertow.AddUndertowListener;
import org.wildfly.extras.creaper.commands.undertow.RemoveUndertowListener;
import org.wildfly.extras.creaper.commands.undertow.SslVerifyClient;
import org.wildfly.extras.creaper.commands.undertow.UndertowListenerType;
import org.wildfly.extras.creaper.core.CommandFailedException;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.extras.creaper.core.online.operations.admin.Administration;

public class ServerTestEnricherUtil {

    private final static Logger LOG = Logger.getLogger(ServerTestEnricherUtil.class);

    /**
     * Remove Undertow HTTPS listener and reload server
     */
    public static boolean removeHttpsListener(OnlineManagementClient client, Administration administration) throws InterruptedException, TimeoutException, IOException {
        try {
            LOG.debug("Remove Undertow HTTPS listener 'https' for default server and reload/restart server");
            client.apply(new RemoveUndertowListener.Builder(UndertowListenerType.HTTPS_LISTENER, "https").forDefaultServer());
            reloadOrRestartTimeoutClient(administration);
            return true;
        } catch (CommandFailedException e) {
            LOG.warn("Undertow HTTPS listener doesn't already exist");
            return false;
        }
    }

    /**
     * Add Undertow HTTPS listener
     */
    public static boolean addHttpsListener(OnlineManagementClient client) {
        try {
            LOG.debug("Add Undertow HTTPS listener 'https'");
            client.execute("/subsystem=undertow/server=default-server/https-listener=https:add(ssl-context=httpsSSC, socket-binding=https)");
            return true;
        } catch (Exception e) {
            LOG.warn("Cannot add HTTPS listener 'https'");
            return false;
        }
    }

    /**
     * Add Undertow HTTPS listener for Wildfly 23
     */
    public static boolean addHttpsListenerAppServer(OnlineManagementClient client) {
        try {
            LOG.debug("Add Undertow HTTPS listener 'https'");
            client.apply(new AddUndertowListener.HttpsBuilder("https", "default-server", "https")
                    .securityRealm("UndertowRealm")
                    .verifyClient(SslVerifyClient.REQUESTED)
                    .build());
            return true;
        } catch (CommandFailedException e) {
            LOG.warn("Cannot add HTTPS listener 'https'");
            return false;
        }
    }

    /**
     * Restart client after timeout for reloading
     */
    public static void reloadOrRestartTimeoutClient(Administration administration) throws IOException, InterruptedException, TimeoutException {
        try {
            if (administration == null) return;
            administration.reloadIfRequired();
        } catch (TimeoutException e) {
            LOG.warn("Cannot reload server; trying to restart it");
            administration.restart();
        }
    }
}
