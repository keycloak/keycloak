package org.keycloak.testsuite.runonserver;

/**
 * Created by st on 26.01.17.
 */
public class RunOnServerException extends RuntimeException {

    public RunOnServerException(Throwable throwable) {
        super(throwable);
    }

}
