package org.keycloak.testframework.remote.runonserver;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.keycloak.common.VerificationException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.testframework.remote.providers.runonserver.RunOnServer;

public class RunTestOnServer implements RunOnServer {

    private final String testClass;
    private final String testMethod;

    public RunTestOnServer(String testClass, String testMethod) {
        this.testClass = testClass;
        this.testMethod = testMethod;
    }

    @Override
    public void run(KeycloakSession session) throws IOException, VerificationException {
        try {
            Class<?> clazz = this.getClass().getClassLoader().loadClass(testClass);
            Object test = clazz.getDeclaredConstructor().newInstance();
            Method method = clazz.getDeclaredMethod(testMethod, KeycloakSession.class);
            method.invoke(test, session);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException re) {
                throw re;
            }
            if (cause instanceof Error er) {
                throw er;
            }
            throw new RuntimeException(cause);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
