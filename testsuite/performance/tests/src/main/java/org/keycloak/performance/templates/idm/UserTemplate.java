package org.keycloak.performance.templates.idm;

import java.util.LinkedList;
import java.util.List;
import static java.util.stream.Collectors.toList;
import org.keycloak.performance.dataset.attr.AttributeMap;
import org.keycloak.performance.dataset.idm.Client;
import org.keycloak.performance.dataset.idm.ClientRole;
import org.keycloak.performance.dataset.idm.ClientRoleMappings;
import org.keycloak.performance.dataset.idm.Credential;
import org.keycloak.performance.dataset.idm.Realm;
import org.keycloak.performance.dataset.idm.RealmRole;
import org.keycloak.performance.dataset.idm.RoleMappings;
import org.keycloak.performance.dataset.idm.RoleMappingsRepresentation;
import org.keycloak.performance.dataset.idm.User;
import org.keycloak.performance.templates.NestedEntityTemplate;
import org.keycloak.performance.templates.NestedEntityTemplateWrapperList;
import org.keycloak.performance.templates.attr.StringListAttributeTemplate;
import org.keycloak.performance.iteration.RandomSublist;
import org.keycloak.performance.util.ValidateNumber;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

/**
 *
 * @author tkyjovsk
 */
public class UserTemplate extends NestedEntityTemplate<Realm, User, UserRepresentation> {

    public static final String USERS_PER_REALM = "usersPerRealm";
    public static final String REALM_ROLES_PER_USER = "realmRolesPerUser";
    public static final String CLIENT_ROLES_PER_USER = "clientRolesPerUser";

    public final int usersPerRealm;
    public final int usersTotal;
    public final int realmRolesPerUser;
    public final int clientRolesPerUser;

    public final UserAttributeTemplate attributeTemplate;
    public final CredentialTemplate credentialTemplate;

    public UserTemplate(RealmTemplate realmTemplate) {
        super(realmTemplate);

        this.usersPerRealm = getConfiguration().getInt(USERS_PER_REALM, 0);
        this.usersTotal = usersPerRealm * realmTemplate.realms;
        this.realmRolesPerUser = getConfiguration().getInt(REALM_ROLES_PER_USER, 0);
        this.clientRolesPerUser = getConfiguration().getInt(CLIENT_ROLES_PER_USER, 0);

        this.attributeTemplate = new UserAttributeTemplate();
        this.credentialTemplate = new CredentialTemplate();
    }

    public RealmTemplate realmTemplate() {
        return (RealmTemplate) getParentEntityTemplate();
    }

    @Override
    public User newEntity(Realm parentEntity, int index) {
        return new User(parentEntity, index);
    }

    @Override
    public void processMappings(User user) {

        user.setCredentials(new NestedEntityTemplateWrapperList<>(user, credentialTemplate));

        // note: attributes are embedded in user rep.
        user.getRepresentation().setAttributes(new AttributeMap(new NestedEntityTemplateWrapperList<>(user, attributeTemplate)));

        // REALM ROLE MAPPINGS
        List<RealmRole> realmRoles = new RandomSublist(
                user.getRealm().getRealmRoles(), // original list
                user.hashCode(), // random seed
                realmRolesPerUser, // sublist size
                false // unique randoms?
        );
        RoleMappingsRepresentation rmr = new RoleMappingsRepresentation();
        realmRoles.forEach(rr -> rmr.add(rr.getRepresentation()));
        user.setRealmRoleMappings(new RoleMappings<>(user, rmr));

        // CLIENT ROLE MAPPINGS
        List<ClientRole> clientRoles = new RandomSublist(
                user.getRealm().getClientRoles(), // original list
                user.hashCode(), // random seed
                clientRolesPerUser, // sublist size
                false // unique randoms?
        );

        List<ClientRoleMappings<User>> clientRoleMappingsList = new LinkedList<>();
        List<Client> clients = clientRoles.stream().map(ClientRole::getClient).distinct().collect(toList());
        clients.forEach(client -> {
            List<ClientRole> clientClientRoles = clientRoles.stream().filter(clientRole
                    -> client.equals(clientRole.getClient()))
                    .collect(toList());

            RoleMappingsRepresentation cmr = new RoleMappingsRepresentation();
            clientClientRoles.forEach(cr -> cmr.add(cr.getRepresentation()));

            ClientRoleMappings<User> crm = new ClientRoleMappings(user, client, cmr);
            clientRoleMappingsList.add(crm);
        });
        user.setClientRoleMappingsList(clientRoleMappingsList);
    }

    @Override
    public int getEntityCountPerParent() {
        return usersPerRealm;
    }

    @Override
    public void validateConfiguration() {

        // sizing
        logger().info(String.format("%s: %s, total: %s", USERS_PER_REALM, usersPerRealm, usersTotal));
        ValidateNumber.minValue(usersPerRealm, 0);

        // mappings
        attributeTemplate.validateConfiguration();

        logger().info(String.format("%s: %s", REALM_ROLES_PER_USER, realmRolesPerUser));
        ValidateNumber.isInRange(realmRolesPerUser, 0,
                realmTemplate().realmRoleTemplate.realmRolesPerRealm);

        logger().info(String.format("%s: %s", CLIENT_ROLES_PER_USER, clientRolesPerUser));
        ClientTemplate ct = realmTemplate().clientTemplate;
        ValidateNumber.isInRange(clientRolesPerUser, 0,
                ct.clientsPerRealm * ct.clientRoleTemplate.clientRolesPerClient);

    }

    public class CredentialTemplate extends NestedEntityTemplate<User, Credential, CredentialRepresentation> {

        public CredentialTemplate() {
            super(UserTemplate.this);
        }

        @Override
        public int getEntityCountPerParent() {
            return 1;
        }

        @Override
        public void validateConfiguration() {
        }

        @Override
        public Credential newEntity(User parentEntity, int index) {
            return new Credential(parentEntity, index);
        }

        @Override
        public void processMappings(Credential entity) {
        }

    }

    public class UserAttributeTemplate extends StringListAttributeTemplate<User> {

        public static final String ATTRIBUTES_PER_USER = "attributesPerUser";

        public final int attributesPerUser;
        public final int attributesTotal;

        public UserAttributeTemplate() {
            super(UserTemplate.this);
            this.attributesPerUser = getConfiguration().getInt(ATTRIBUTES_PER_USER, 0);
            this.attributesTotal = attributesPerUser * usersTotal;
        }

        @Override
        public int getEntityCountPerParent() {
            return attributesPerUser;
        }

        @Override
        public void validateConfiguration() {
            logger().info(String.format("%s: %s", ATTRIBUTES_PER_USER, attributesPerUser));
            ValidateNumber.minValue(attributesPerUser, 0);
        }

    }

}
