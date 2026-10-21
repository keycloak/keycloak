package org.keycloak.testframework.remote.providers.runonserver;

/**
 * Created by st on 26.01.17.
 */
public interface FetchOnServerWrapper<T> {

    FetchOnServer getRunOnServer();

    Class<T> getResultClass();

}
