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

package org.keycloak.adapters;

import org.jboss.logging.Logger;
import org.keycloak.common.util.HostUtils;
import org.keycloak.common.util.Time;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class NodesRegistrationManagement {

    private static final Logger log = Logger.getLogger(NodesRegistrationManagement.class);

    private final Map<String, NodeRegistrationContext> nodeRegistrations = new ConcurrentHashMap<String, NodeRegistrationContext>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // Sending registration event during first request to application or if re-registration is needed
    public void tryRegister(final KeycloakDeployment resolvedDeployment) {
        if (resolvedDeployment.isRegisterNodeAtStartup()) {
            final String registrationUri = resolvedDeployment.getRegisterNodeUrl();
            if (needRefreshRegistration(registrationUri, resolvedDeployment)) {
                Runnable runnable = new Runnable() {

                    @Override
                    public void run() {
                        // Need to check it again in case that executor triggered by other thread already finished computation in the meantime
                        if (needRefreshRegistration(registrationUri, resolvedDeployment)) {
                            sendRegistrationEvent(resolvedDeployment);
                        }
                    }
                };
                executor.execute(runnable);
            }
        }
    }

    private boolean needRefreshRegistration(String registrationUri, KeycloakDeployment resolvedDeployment) {
        NodeRegistrationContext currentRegistration = nodeRegistrations.get(registrationUri);
        /// We don't yet have any registration for this node
        if (currentRegistration == null) {
            return true;
        }

        return currentRegistration.lastRegistrationTime + resolvedDeployment.getRegisterNodePeriod() < Time.currentTime();
    }

    /**
     * Called during undeployment or server stop. De-register from all previously registered deployments
     */
    public void stop() {
        executor.shutdownNow();

        Collection<NodeRegistrationContext> allRegistrations = nodeRegistrations.values();
        for (NodeRegistrationContext registration : allRegistrations) {
            sendUnregistrationEvent(registration.resolvedDeployment);
        }
    }

    protected void sendRegistrationEvent(KeycloakDeployment deployment) {
        // This method is invoked from single-thread executor, so no synchronization is needed
        // However, it could happen that the same deployment was submitted more than once to that executor
        // Hence we need to recheck that the registration is really needed
        final String registrationUri = deployment.getRegisterNodeUrl();
        if (! needRefreshRegistration(registrationUri, deployment)) {
            return;
        }
        if (Thread.currentThread().isInterrupted()) {
            return;
        }

        log.debug("Sending registration event right now");

        String host = HostUtils.getHostName();
        try {
            ServerRequest.invokeRegisterNode(deployment, host);
            NodeRegistrationContext regContext = new NodeRegistrationContext(Time.currentTime(), deployment);
            nodeRegistrations.put(deployment.getRegisterNodeUrl(), regContext);
            log.debugf("Node '%s' successfully registered in Keycloak", host);
        } catch (ServerRequest.HttpFailure failure) {
            log.error("failed to register node to keycloak");
            log.error("status from server: " + failure.getStatus());
            if (failure.getError() != null) {
                log.error("   " + failure.getError());
            }
        } catch (IOException e) {
            log.error("failed to register node to keycloak", e);
        }
    }

    protected boolean sendUnregistrationEvent(KeycloakDeployment deployment) {
        log.debug("Sending Unregistration event right now");

        String host = HostUtils.getHostName();
        try {
            ServerRequest.invokeUnregisterNode(deployment, host);
            log.debugf("Node '%s' successfully unregistered from Keycloak", host);
            return true;
        } catch (ServerRequest.HttpFailure failure) {
            log.error("failed to unregister node from keycloak");
            log.error("status from server: " + failure.getStatus());
            if (failure.getError() != null) {
                log.error("   " + failure.getError());
            }
            return false;
        } catch (IOException e) {
            log.error("failed to unregister node from keycloak", e);
            return false;
        }
    }

    public static class NodeRegistrationContext {

        private final Integer lastRegistrationTime;
        // deployment instance used for registration request
        private final KeycloakDeployment resolvedDeployment;

        public NodeRegistrationContext(Integer lastRegTime, KeycloakDeployment deployment) {
            this.lastRegistrationTime = lastRegTime;
            this.resolvedDeployment = deployment;
        }
    }

}
