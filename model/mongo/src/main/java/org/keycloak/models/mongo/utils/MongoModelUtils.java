package org.keycloak.models.mongo.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import org.bson.types.ObjectId;
import org.keycloak.models.UserModel;
import org.keycloak.models.mongo.api.MongoStore;
import org.keycloak.models.mongo.keycloak.adapters.UserAdapter;
import org.keycloak.models.mongo.keycloak.entities.RoleEntity;
import org.keycloak.models.mongo.keycloak.entities.UserEntity;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class MongoModelUtils {

    public static List<ObjectId> convertStringsToObjectIds(Collection<String> strings) {
        List<ObjectId> result = new ArrayList<ObjectId>();
        for (String id : strings) {
            result.add(new ObjectId(id));
        }
        return result;
    }

    // Get everything including both application and realm roles
    public static List<RoleEntity> getAllRolesOfUser(UserModel user, MongoStore mongoStore) {
        UserEntity userEntity = ((UserAdapter)user).getUser();
        List<String> roleIds = userEntity.getRoleIds();

        if (roleIds == null || roleIds.isEmpty()) {
            return Collections.EMPTY_LIST;
        }

        DBObject query = new QueryBuilder()
                .and("_id").in(convertStringsToObjectIds(roleIds))
                .get();
        return mongoStore.loadObjects(RoleEntity.class, query);
    }

    // Get everything including both application and realm scopes
    public static List<RoleEntity> getAllScopesOfUser(UserModel user, MongoStore mongoStore) {
        UserEntity userEntity = ((UserAdapter)user).getUser();
        List<String> scopeIds = userEntity.getScopeIds();

        if (scopeIds == null || scopeIds.isEmpty()) {
            return Collections.EMPTY_LIST;
        }

        DBObject query = new QueryBuilder()
                .and("_id").in(convertStringsToObjectIds(scopeIds))
                .get();
        return mongoStore.loadObjects(RoleEntity.class, query);
    }
}
