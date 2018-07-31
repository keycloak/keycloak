package org.keycloak.performance.templates.idm;

import java.util.List;
import org.keycloak.performance.dataset.idm.Client;
import org.keycloak.performance.dataset.idm.ClientRole;
import org.keycloak.performance.dataset.Dataset;
import org.keycloak.performance.iteration.Flattened2DList;
import org.keycloak.performance.dataset.idm.Realm;
import org.keycloak.performance.dataset.idm.authorization.ResourceServerList;
import org.keycloak.performance.templates.NestedEntityTemplate;
import org.keycloak.performance.templates.NestedEntityTemplateWrapperList;
import org.keycloak.performance.templates.DatasetTemplate;
import org.keycloak.performance.util.ValidateNumber;
import org.keycloak.representations.idm.RealmRepresentation;

/**
 *
 * @author tkyjovsk
 */
public class RealmTemplate extends NestedEntityTemplate<Dataset, Realm, RealmRepresentation> {

    public static final String REALMS = "realms";
    
    public final int realms;
    
    public final ClientTemplate clientTemplate;
    public final RealmRoleTemplate realmRoleTemplate;
    public final UserTemplate userTemplate;
    public final GroupTemplate groupTemplate;

    public RealmTemplate(DatasetTemplate datasetTemplate) {
        super(datasetTemplate);
        this.realms = getConfiguration().getInt(REALMS, 0);
        this.clientTemplate = new ClientTemplate(this);
        this.realmRoleTemplate = new RealmRoleTemplate(this);
        this.userTemplate = new UserTemplate(this);
        this.groupTemplate = new GroupTemplate(this);
    }

    @Override
    public int getEntityCountPerParent() {
        return realms;
    }

    @Override
    public void validateConfiguration() {
        // sizing
        logger().info(String.format("%s: %s", REALMS, realms));
        ValidateNumber.minValue(realms, 0);

        clientTemplate.validateConfiguration();
        realmRoleTemplate.validateConfiguration();
        userTemplate.validateConfiguration();
        groupTemplate.validateConfiguration();
    }

    @Override
    public Realm newEntity(Dataset parentEntity, int index) {
        return new Realm(parentEntity, index);
    }

    @Override
    public void processMappings(Realm realm) {
        realm.setClients(new NestedEntityTemplateWrapperList<>(realm, clientTemplate));
        realm.setResourceServers(new ResourceServerList(realm.getClients()));
        realm.setClientRoles(new Flattened2DList<Client, ClientRole>() {
            @Override
            public List<Client> getXList() {
                return realm.getClients();
            }

            @Override
            public List<ClientRole> getYList(Client client) {
                return client.getClientRoles();
            }

            @Override
            public int getYListSize() {
                return clientTemplate.clientRoleTemplate.clientRolesPerClient;
            }
        });
        realm.setRealmRoles(new NestedEntityTemplateWrapperList<>(realm, realmRoleTemplate));
        realm.setUsers(new NestedEntityTemplateWrapperList<>(realm, userTemplate));
        realm.setGroups(new NestedEntityTemplateWrapperList<>(realm, groupTemplate));
    }

}
