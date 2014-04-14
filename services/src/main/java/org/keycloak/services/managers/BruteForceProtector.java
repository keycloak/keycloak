package org.keycloak.services.managers;


import org.jboss.resteasy.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UsernameLoginFailureModel;
import org.keycloak.services.ClientConnection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * A single thread will log failures.  This is so that we can avoid concurrent writes as we want an accurate failure count
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class BruteForceProtector implements Runnable {
    protected static Logger logger = Logger.getLogger(BruteForceProtector.class);

    protected int maxFailureWaitSeconds = 900;
    protected int minimumQuickLoginWaitSeconds = 60;
    protected int waitIncrementSeconds = 60;
    protected long quickLoginCheckMilliSeconds = 1000;
    protected long maxDeltaTimeMilliSeconds = 60 * 60 * 12 * 1000; // 12 hours
    protected int failureFactor = 30;
    protected volatile boolean run = true;
    protected KeycloakSessionFactory factory;
    protected CountDownLatch shutdownLatch = new CountDownLatch(1);

    protected volatile long failures;
    protected volatile long lastFailure;
    protected volatile long totalTime;

    protected LinkedBlockingQueue<LoginEvent> queue = new LinkedBlockingQueue<LoginEvent>();
    public static final int TRANSACTION_SIZE = 20;


    protected abstract class LoginEvent implements Comparable<LoginEvent> {
        protected final String realmId;
        protected final String username;
        protected final String ip;

        protected LoginEvent(String realmId, String username, String ip) {
            this.realmId = realmId;
            this.username = username;
            this.ip = ip;
        }

        @Override
        public int compareTo(LoginEvent o) {
            return username.compareTo(o.username);
        }
    }

    protected class SuccessfulLogin extends LoginEvent {
        public SuccessfulLogin(String realmId, String userId, String ip) {
            super(realmId, userId, ip);
        }
    }

    protected class ShutdownEvent extends LoginEvent {
        public ShutdownEvent() {
            super(null, null, null);
        }
    }

    protected class FailedLogin extends LoginEvent {
        protected final CountDownLatch latch = new CountDownLatch(1);

        public FailedLogin(String realmId, String username, String ip) {
            super(realmId, username, ip);
        }
    }

    public BruteForceProtector(KeycloakSessionFactory factory) {
        this.factory = factory;
    }

    public void failure(KeycloakSession session, LoginEvent event) {
        UsernameLoginFailureModel user = getUserModel(session, event);
        if (user == null) return;
        user.setLastIPFailure(event.ip);
        long currentTime = System.currentTimeMillis();
        long last = user.getLastFailure();
        long deltaTime = 0;
        if (last > 0) {
            deltaTime = currentTime - last;
        }
        user.setLastFailure(currentTime);
        if (deltaTime > 0) {
            // if last failure was more than MAX_DELTA clear failures
            if (deltaTime > maxDeltaTimeMilliSeconds) {
                user.clearFailures();
            }
        }
        user.incrementFailures();

        int waitSeconds = waitIncrementSeconds * (user.getNumFailures() / failureFactor);
        if (waitSeconds == 0) {
            if (deltaTime > quickLoginCheckMilliSeconds) {
                waitSeconds = minimumQuickLoginWaitSeconds;
            }
        }
        waitSeconds = Math.min(maxFailureWaitSeconds, waitSeconds);
        if (waitSeconds > 0) {
            user.setFailedLoginNotBefore((int) (currentTime / 1000) + waitSeconds);
        }
    }

    protected UsernameLoginFailureModel getUserModel(KeycloakSession session, LoginEvent event) {
        RealmModel realm = getRealmModel(session, event);
        if (realm == null) return null;
        UsernameLoginFailureModel user = realm.getUserLoginFailure(event.username);
        if (user == null) return null;
        return user;
    }

    protected RealmModel getRealmModel(KeycloakSession session, LoginEvent event) {
        RealmModel realm = session.getRealm(event.realmId);
        if (realm == null) return null;
        return realm;
    }

    public void start() {
        new Thread(this, "Brute Force Protector").start();
    }

    public void shutdown() {
        run = false;
        try {
            queue.offer(new ShutdownEvent());
            shutdownLatch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    public void run() {
        final ArrayList<LoginEvent> events = new ArrayList<LoginEvent>(TRANSACTION_SIZE + 1);
        while (run) {
            try {
                LoginEvent take = queue.poll(2, TimeUnit.SECONDS);
                if (take == null) {
                    continue;
                }
                try {
                    events.add(take);
                    queue.drainTo(events, TRANSACTION_SIZE);
                    for (LoginEvent event : events) {
                        if (event instanceof FailedLogin) {
                            logFailure(event);
                        } else if (event instanceof SuccessfulLogin) {
                            logSuccess(event);
                        }
                    }

                    Collections.sort(events); // we sort to avoid deadlock due to ordered updates.  Maybe I'm overthinking this.
                    KeycloakSession session = factory.createSession();
                    try {
                        for (LoginEvent event : events) {
                            if (event instanceof FailedLogin) {
                                failure(session, event);
                            }
                        }
                        session.getTransaction().commit();
                    } catch (Exception e) {
                        session.getTransaction().rollback();
                        throw e;
                    } finally {
                        for (LoginEvent event : events) {
                            if (event instanceof FailedLogin) {
                                ((FailedLogin) event).latch.countDown();
                            }
                        }
                        events.clear();
                        session.close();
                    }
                } catch (Exception e) {
                    logger.error("Failed processing event", e);
                }
            } catch (InterruptedException e) {
                break;
            } finally {
                shutdownLatch.countDown();
            }
        }
    }

    protected void logSuccess(LoginEvent event) {
        logger.warn("login success for user " + event.username + " from ip " + event.ip);
    }

    protected void logFailure(LoginEvent event) {
        logger.warn("login failure for user " + event.username + " from ip " + event.ip);
        failures++;
        long delta = 0;
        if (lastFailure > 0) {
            delta = System.currentTimeMillis() - lastFailure;
            if (delta > maxDeltaTimeMilliSeconds) {
                totalTime = 0;

            } else {
                totalTime += delta;
            }
        }
    }

    public void successfulLogin(RealmModel realm, String username, ClientConnection clientConnection) {
        logger.info("successful login user: " + username + " from ip " + clientConnection.getRemoteAddr());
    }

    public void invalidUser(RealmModel realm, String username, ClientConnection clientConnection) {
        logger.warn("invalid user: " + username + " from ip " + clientConnection.getRemoteAddr());
        // todo more?
    }

    public void failedLogin(RealmModel realm, String username, ClientConnection clientConnection) {
        try {
            FailedLogin event = new FailedLogin(realm.getId(), username, clientConnection.getRemoteAddr());
            queue.offer(event);
            // wait a minimum of seconds for event to process so that a hacker
            // cannot flood with failed logins and overwhelm the queue and not have notBefore updated to block next requests
            // todo failure HTTP responses should be queued via async HTTP
            event.latch.await(5, TimeUnit.SECONDS);

        } catch (InterruptedException e) {
        }
    }

    public boolean isTemporarilyDisabled(RealmModel realm, String username) {
        UsernameLoginFailureModel failure = realm.getUserLoginFailure(username);
        if (failure == null) {
            return false;
        }

        int currTime = (int)(System.currentTimeMillis()/1000);
        if (currTime < failure.getFailedLoginNotBefore()) {
            return true;
        }
        return false;
    }

    public long getFailures() {
        return failures;
    }

    public long getLastFailure() {
        return lastFailure;
    }

    public int getMaxFailureWaitSeconds() {
        return maxFailureWaitSeconds;
    }

    public void setMaxFailureWaitSeconds(int maxFailureWaitSeconds) {
        this.maxFailureWaitSeconds = maxFailureWaitSeconds;
    }

    public int getMinimumQuickLoginWaitSeconds() {
        return minimumQuickLoginWaitSeconds;
    }

    public void setMinimumQuickLoginWaitSeconds(int minimumQuickLoginWaitSeconds) {
        this.minimumQuickLoginWaitSeconds = minimumQuickLoginWaitSeconds;
    }

    public int getWaitIncrementSeconds() {
        return waitIncrementSeconds;
    }

    public void setWaitIncrementSeconds(int waitIncrementSeconds) {
        this.waitIncrementSeconds = waitIncrementSeconds;
    }

    public long getQuickLoginCheckMilliSeconds() {
        return quickLoginCheckMilliSeconds;
    }

    public void setQuickLoginCheckMilliSeconds(long quickLoginCheckMilliSeconds) {
        this.quickLoginCheckMilliSeconds = quickLoginCheckMilliSeconds;
    }

    public long getMaxDeltaTimeMilliSeconds() {
        return maxDeltaTimeMilliSeconds;
    }

    public void setMaxDeltaTimeMilliSeconds(long maxDeltaTimeMilliSeconds) {
        this.maxDeltaTimeMilliSeconds = maxDeltaTimeMilliSeconds;
    }

    public int getFailureFactor() {
        return failureFactor;
    }

    public void setFailureFactor(int failureFactor) {
        this.failureFactor = failureFactor;
    }
}
