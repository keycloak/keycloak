package org.keycloak.common.util;

public interface ResteasyProvider {

    <R> R getContextData(Class<R> type);

}
