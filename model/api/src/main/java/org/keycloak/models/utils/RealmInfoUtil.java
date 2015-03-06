package org.keycloak.models.utils;

import org.keycloak.models.RealmModel;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class RealmInfoUtil {

    public static int getDettachedClientSessionLifespan(RealmModel realm) {
        int lifespan = realm.getAccessCodeLifespanLogin();
        if (realm.getAccessCodeLifespanUserAction() > lifespan) {
            lifespan = realm.getAccessCodeLifespanUserAction();
        }
        if (realm.getAccessCodeLifespan() > lifespan) {
            lifespan = realm.getAccessCodeLifespan();
        }
        return lifespan;
    }

}
