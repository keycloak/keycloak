package org.keycloak.models.mongo.utils;

import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import org.keycloak.connections.mongo.api.context.MongoStoreInvocationContext;
import org.keycloak.models.ClientModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.entities.ClientEntity;
import org.keycloak.models.mongo.keycloak.adapters.ClientAdapter;
import org.keycloak.models.mongo.keycloak.adapters.UserAdapter;
import org.keycloak.models.mongo.keycloak.entities.MongoRoleEntity;
import org.keycloak.models.mongo.keycloak.entities.MongoUserEntity;

import java.util.Collections;
import java.util.List;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class MongoModelUtils {

    // Get everything including both application and realm roles
    public static List<MongoRoleEntity> getAllRolesOfUser(UserModel user, MongoStoreInvocationContext invContext) {
        MongoUserEntity userEntity = ((UserAdapter)user).getUser();
        List<String> roleIds = userEntity.getRoleIds();

        if (roleIds == null || roleIds.isEmpty()) {
            return Collections.EMPTY_LIST;
        }

        DBObject query = new QueryBuilder()
                .and("_id").in(roleIds)
                .get();
        return invContext.getMongoStore().loadEntities(MongoRoleEntity.class, query, invContext);
    }

    // Get everything including both application and realm scopes
    public static List<MongoRoleEntity> getAllScopesOfClient(ClientModel client, MongoStoreInvocationContext invContext) {
        ClientEntity scopedEntity = ((ClientAdapter)client).getMongoEntityAsClient();
        List<String> scopeIds = scopedEntity.getScopeIds();

        if (scopeIds == null || scopeIds.isEmpty()) {
            return Collections.EMPTY_LIST;
        }

        DBObject query = new QueryBuilder()
                .and("_id").in(scopeIds)
                .get();
        return invContext.getMongoStore().loadEntities(MongoRoleEntity.class, query, invContext);
    }
}
