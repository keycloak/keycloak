package org.keycloak.adapters;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import org.jboss.logging.Logger;
import org.keycloak.enums.RelativeUrlsUsed;
import org.keycloak.util.HostUtils;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class NodesRegistrationLifecycle {

    private static final Logger log = Logger.getLogger(NodesRegistrationLifecycle.class);

    private final KeycloakDeployment deployment;
    private final Timer timer;

    // True if at least one event was successfully sent
    private volatile boolean registered = false;

    public NodesRegistrationLifecycle(KeycloakDeployment deployment) {
        this.deployment = deployment;
        this.timer =  new Timer();
    }

    public void start() {
        if (!deployment.isRegisterNodeAtStartup() && deployment.getRegisterNodePeriod() <= 0) {
            log.info("Skip registration of cluster nodes at startup");
            return;
        }

        if (deployment.getRelativeUrls() == RelativeUrlsUsed.ALL_REQUESTS) {
            log.warn("Skip registration of cluster nodes at startup as Keycloak node can't be contacted. Make sure to not use relative URI in adapters configuration!");
            return;
        }

        if (deployment.isRegisterNodeAtStartup()) {
            boolean success = sendRegistrationEvent();
            if (!success) {
                throw new IllegalStateException("Failed to register node to keycloak at startup");
            }
        }

        if (deployment.getRegisterNodePeriod() > 0) {
            addPeriodicListener();
        }
    }

    public void stop() {
        removePeriodicListener();
        if (registered) {
            sendUnregistrationEvent();
        }
    }

    protected void addPeriodicListener() {
        TimerTask task = new TimerTask() {

            @Override
            public void run() {
                sendRegistrationEvent();
            }
        };

        long interval = deployment.getRegisterNodePeriod() * 1000;
        log.info("Setup of periodic re-registration event sending each " + interval + " ms");
        timer.schedule(task, interval, interval);
    }

    protected void removePeriodicListener() {
        timer.cancel();
    }

    protected boolean sendRegistrationEvent() {
        log.info("Sending registration event right now");

        String host = HostUtils.getIpAddress();
        try {
            ServerRequest.invokeRegisterNode(deployment, host);
            log.infof("Node '%s' successfully registered in Keycloak", host);
            registered = true;
            return true;
        } catch (ServerRequest.HttpFailure failure) {
            log.error("failed to register node to keycloak");
            log.error("status from server: " + failure.getStatus());
            if (failure.getError() != null) {
                log.error("   " + failure.getError());
            }
            return false;
        } catch (IOException e) {
            log.error("failed to register node to keycloak", e);
            return false;
        }
    }

    protected boolean sendUnregistrationEvent() {
        log.info("Sending UNregistration event right now");

        String host = HostUtils.getIpAddress();
        try {
            ServerRequest.invokeUnregisterNode(deployment, host);
            log.infof("Node '%s' successfully unregistered from Keycloak", host);
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

}
