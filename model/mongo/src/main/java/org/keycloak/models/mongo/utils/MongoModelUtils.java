package org.keycloak.models.mongo.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import org.bson.types.ObjectId;
import org.keycloak.models.ClientModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.mongo.api.context.MongoStoreInvocationContext;
import org.keycloak.models.mongo.keycloak.adapters.AbstractAdapter;
import org.keycloak.models.mongo.keycloak.adapters.UserAdapter;
import org.keycloak.models.mongo.keycloak.entities.RoleEntity;
import org.keycloak.models.mongo.keycloak.entities.ScopedEntity;
import org.keycloak.models.mongo.keycloak.entities.UserEntity;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class MongoModelUtils {

    // Get everything including both application and realm roles
    public static List<RoleEntity> getAllRolesOfUser(UserModel user, MongoStoreInvocationContext invContext) {
        UserEntity userEntity = ((UserAdapter)user).getUser();
        List<String> roleIds = userEntity.getRoleIds();

        if (roleIds == null || roleIds.isEmpty()) {
            return Collections.EMPTY_LIST;
        }

        DBObject query = new QueryBuilder()
                .and("_id").in(roleIds)
                .get();
        return invContext.getMongoStore().loadEntities(RoleEntity.class, query, invContext);
    }

    // Get everything including both application and realm scopes
    public static List<RoleEntity> getAllScopesOfClient(ClientModel client, MongoStoreInvocationContext invContext) {
        ScopedEntity scopedEntity = (ScopedEntity)((AbstractAdapter)client).getMongoEntity();
        List<String> scopeIds = scopedEntity.getScopeIds();

        if (scopeIds == null || scopeIds.isEmpty()) {
            return Collections.EMPTY_LIST;
        }

        DBObject query = new QueryBuilder()
                .and("_id").in(scopeIds)
                .get();
        return invContext.getMongoStore().loadEntities(RoleEntity.class, query, invContext);
    }
}
