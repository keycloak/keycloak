package org.keycloak.models.mongo.utils;

import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import org.keycloak.connections.mongo.api.context.MongoStoreInvocationContext;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.entities.ClientEntity;
import org.keycloak.models.mongo.keycloak.adapters.ClientAdapter;
import org.keycloak.models.mongo.keycloak.adapters.UserAdapter;
import org.keycloak.models.mongo.keycloak.entities.MongoRoleEntity;
import org.keycloak.models.mongo.keycloak.entities.MongoUserEntity;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class MongoModelUtils {

    // Get everything including both application and realm roles
    public static List<RoleModel> getAllRolesOfUser(RealmModel realm, UserModel user) {
        MongoUserEntity userEntity = ((UserAdapter)user).getUser();
        List<String> roleIds = userEntity.getRoleIds();

        if (roleIds == null || roleIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<RoleModel> roles = new LinkedList<RoleModel>();
        for (String roleId : roleIds) {
            RoleModel role = realm.getRoleById(roleId);
            if (role != null) {
                roles.add(role);
            }
        }
        return roles;
    }

    // Get everything including both application and realm scopes
    public static List<MongoRoleEntity> getAllScopesOfClient(ClientModel client, MongoStoreInvocationContext invContext) {
        ClientEntity scopedEntity = ((ClientAdapter)client).getMongoEntity();
        List<String> scopeIds = scopedEntity.getScopeIds();

        if (scopeIds == null || scopeIds.isEmpty()) {
            return Collections.emptyList();
        }

        DBObject query = new QueryBuilder()
                .and("_id").in(scopeIds)
                .get();
        return invContext.getMongoStore().loadEntities(MongoRoleEntity.class, query, invContext);
    }
}
