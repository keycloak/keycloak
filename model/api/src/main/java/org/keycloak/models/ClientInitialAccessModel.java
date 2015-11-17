package org.keycloak.models;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface ClientInitialAccessModel {

    String getId();

    RealmModel getRealm();

    int getTimestamp();

    int getExpiration();

    int getCount();

    int getRemainingCount();

    void decreaseRemainingCount();

}
