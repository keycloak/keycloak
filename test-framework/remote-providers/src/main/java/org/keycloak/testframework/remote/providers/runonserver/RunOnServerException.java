package org.keycloak.testframework.remote.providers.runonserver;

/**
 * Created by st on 26.01.17.
 */
public class RunOnServerException extends RuntimeException {

    public RunOnServerException(Throwable throwable) {
        super(throwable);
    }

}
