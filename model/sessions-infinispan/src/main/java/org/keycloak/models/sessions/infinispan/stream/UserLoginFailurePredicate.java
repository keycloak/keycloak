package org.keycloak.models.sessions.infinispan.stream;

import org.keycloak.models.sessions.infinispan.entities.LoginFailureEntity;
import org.keycloak.models.sessions.infinispan.entities.LoginFailureKey;

import java.io.Serializable;
import java.util.Map;
import java.util.function.Predicate;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class UserLoginFailurePredicate implements Predicate<Map.Entry<LoginFailureKey, LoginFailureEntity>>, Serializable {

    private String realm;

    private UserLoginFailurePredicate(String realm) {
        this.realm = realm;
    }

    public static UserLoginFailurePredicate create(String realm) {
        return new UserLoginFailurePredicate(realm);
    }

    @Override
    public boolean test(Map.Entry<LoginFailureKey, LoginFailureEntity> entry) {
        LoginFailureEntity e = entry.getValue();
        return realm.equals(e.getRealm());
    }

}
