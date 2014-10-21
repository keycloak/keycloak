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
public class NodesRegistrationManagement {

    private static final Logger log = Logger.getLogger(NodesRegistrationManagement.class);

    private final KeycloakDeployment deployment;
    private final Timer timer;

    // True if at least one event was successfully sent
    private volatile boolean registered = false;

    public NodesRegistrationManagement(KeycloakDeployment deployment) {
        this.deployment = deployment;
        this.timer =  new Timer();
    }

    // Register listener for periodic sending of re-registration event
    public void start() {
        if (deployment.getRegisterNodePeriod() <= 0) {
            log.infof("Skip periodic registration of cluster nodes at startup for application %s", deployment.getResourceName());
            return;
        }

        if (deployment.getRelativeUrls() == null || deployment.getRelativeUrls() == RelativeUrlsUsed.ALL_REQUESTS) {
            log.errorf("Skip periodic registration of cluster nodes at startup for application %s as Keycloak node can't be contacted. Make sure to provide some non-relative URI in adapters configuration.", deployment.getResourceName());
            return;
        }

        addPeriodicListener();
    }

    // Sending registration event during first request to application
    public void tryRegister(KeycloakDeployment resolvedDeployment) {
        if (resolvedDeployment.isRegisterNodeAtStartup() && !registered) {
            synchronized (this) {
                if (!registered) {
                    sendRegistrationEvent(resolvedDeployment);
                }
            }
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
                sendRegistrationEvent(deployment);
            }
        };

        long interval = deployment.getRegisterNodePeriod() * 1000;
        log.info("Setup of periodic re-registration event sending each " + interval + " ms");
        timer.schedule(task, interval, interval);
    }

    protected void removePeriodicListener() {
        timer.cancel();
    }

    protected void sendRegistrationEvent(KeycloakDeployment deployment) {
        log.info("Sending registration event right now");

        String host = HostUtils.getIpAddress();
        try {
            ServerRequest.invokeRegisterNode(deployment, host);
            log.infof("Node '%s' successfully registered in Keycloak", host);
            registered = true;
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

    protected boolean sendUnregistrationEvent() {
        log.info("Sending Unregistration event right now");

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
