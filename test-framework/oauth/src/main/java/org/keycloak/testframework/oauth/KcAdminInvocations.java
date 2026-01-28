package org.keycloak.testframework.oauth;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.keycloak.representations.adapters.action.LogoutAction;
import org.keycloak.representations.adapters.action.PushNotBeforeAction;
import org.keycloak.representations.adapters.action.TestAvailabilityAction;

public class KcAdminInvocations {

    private final BlockingQueue<LogoutAction> adminLogoutActions = new LinkedBlockingQueue<>();
    private final BlockingQueue<PushNotBeforeAction> adminPushNotBeforeActions = new LinkedBlockingQueue<>();
    private final BlockingQueue<TestAvailabilityAction> adminTestAvailabilityAction = new LinkedBlockingQueue<>();

    KcAdminInvocations() {
    }

    public PushNotBeforeAction getAdminPushNotBefore() throws InterruptedException {
        return adminPushNotBeforeActions.poll(10, TimeUnit.SECONDS);
    }

    void add(PushNotBeforeAction action) {
        adminPushNotBeforeActions.add(action);
    }

    public TestAvailabilityAction getTestAvailable() throws InterruptedException {
        return adminTestAvailabilityAction.poll(10, TimeUnit.SECONDS);
    }

    void add(TestAvailabilityAction action) {
        adminTestAvailabilityAction.add(action);
    }

    public LogoutAction getAdminLogoutAction() throws InterruptedException {
        return adminLogoutActions.poll(10, TimeUnit.SECONDS);
    }

    void add(LogoutAction action) {
        adminLogoutActions.add(action);
    }

    public void clear() {
        adminLogoutActions.clear();
        adminPushNotBeforeActions.clear();
        adminTestAvailabilityAction.clear();
    }

}
