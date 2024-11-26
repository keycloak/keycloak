package org.keycloak.federation.scim.core;

import de.captaingoldfish.scim.sdk.common.constants.HttpHeader;
import jakarta.ws.rs.core.MediaType;
import org.apache.commons.lang3.BooleanUtils;
import org.jboss.logging.Logger;
import org.keycloak.common.Profile;
import org.keycloak.common.Profile.Feature;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.UserStorageProviderFactory;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.storage.user.ImportSynchronization;
import org.keycloak.storage.user.SynchronizationResult;
import org.keycloak.federation.scim.event.ScimBackgroundGroupMembershipUpdater;
import org.keycloak.storage.user.UserRegistrationProvider;

import java.util.Date;
import java.util.List;

/**
 * Allows to register and configure Scim endpoints through Admin console, using the provided config properties.
 */
public class ScimEndpointConfigurationStorageProviderFactory implements
        UserStorageProviderFactory<ScimEndpointConfigurationStorageProviderFactory.ScimEndpointConfigurationStorageProvider>,
        ImportSynchronization {
    public static final String ID = "scim";
    private static final Logger LOGGER = Logger.getLogger(ScimEndpointConfigurationStorageProviderFactory.class);

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public SynchronizationResult sync(KeycloakSessionFactory sessionFactory, String realmId, UserStorageProviderModel model) {
        // Manually Launch a synchronization between keycloack and the SCIM endpoint described in the given model
        LOGGER.infof("[SCIM] Sync from ScimStorageProvider - Realm %s - Model %s", realmId, model.getName());
        SynchronizationResult result = new SynchronizationResult();
        KeycloakModelUtils.runJobInTransaction(sessionFactory, session -> {
            RealmModel realm = session.realms().getRealm(realmId);
            session.getContext().setRealm(realm);
            ScimDispatcher dispatcher = new ScimDispatcher(session);
            if (BooleanUtils.TRUE.equals(model.get(ScrimEndPointConfiguration.CONF_KEY_PROPAGATION_USER))) {
                dispatcher.dispatchUserModificationToOne(model, client -> client.sync(result));
            }
            if (BooleanUtils.TRUE.equals(model.get(ScrimEndPointConfiguration.CONF_KEY_PROPAGATION_GROUP))) {
                dispatcher.dispatchGroupModificationToOne(model, client -> client.sync(result));
            }
            dispatcher.close();
        });
        return result;
    }

    @Override
    public SynchronizationResult syncSince(Date lastSync, KeycloakSessionFactory sessionFactory, String realmId,
            UserStorageProviderModel model) {
        return this.sync(sessionFactory, realmId, model);
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        if (Profile.isFeatureEnabled(Feature.SCIM_FEDERATION)) {
            ScimBackgroundGroupMembershipUpdater scimBackgroundGroupMembershipUpdater = new ScimBackgroundGroupMembershipUpdater(
                    factory);
            scimBackgroundGroupMembershipUpdater.startBackgroundUpdates();
        }
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        // These Config Properties will be use to generate configuration page in Admin Console
        return ProviderConfigurationBuilder.create().property().name(ScrimEndPointConfiguration.CONF_KEY_ENDPOINT)
                .type(ProviderConfigProperty.STRING_TYPE).required(true).label("SCIM 2.0 endpoint")
                .helpText("External SCIM 2.0 base "
                        + "URL (/ServiceProviderConfig  /Schemas and /ResourcesTypes should be accessible)")
                .add().property().name(ScrimEndPointConfiguration.CONF_KEY_CONTENT_TYPE).type(ProviderConfigProperty.LIST_TYPE)
                .label("Endpoint content type").helpText("Only used when endpoint doesn't support application/scim+json")
                .options(MediaType.APPLICATION_JSON, HttpHeader.SCIM_CONTENT_TYPE).defaultValue(HttpHeader.SCIM_CONTENT_TYPE)
                .add().property().name(ScrimEndPointConfiguration.CONF_KEY_AUTH_MODE).type(ProviderConfigProperty.LIST_TYPE)
                .label("Auth mode").helpText("Select the authorization mode").options("NONE", "BASIC_AUTH", "BEARER")
                .defaultValue("NONE").add().property().name(ScrimEndPointConfiguration.CONF_KEY_AUTH_USER)
                .type(ProviderConfigProperty.STRING_TYPE).label("Auth username").helpText("Required for basic authentication.")
                .add().property().name(ScrimEndPointConfiguration.CONF_KEY_AUTH_PASSWORD).type(ProviderConfigProperty.PASSWORD)
                .label("Auth password/token").helpText("Password or token required for basic or bearer authentication.").add()
                .property().name(ScrimEndPointConfiguration.CONF_KEY_PROPAGATION_USER).type(ProviderConfigProperty.BOOLEAN_TYPE)
                .label("Enable user propagation").helpText("Should operation on users be propagated to this provider?")
                .defaultValue(BooleanUtils.TRUE).add().property().name(ScrimEndPointConfiguration.CONF_KEY_PROPAGATION_GROUP)
                .type(ProviderConfigProperty.BOOLEAN_TYPE).label("Enable group propagation")
                .helpText("Should operation on groups be propagated to this provider?").defaultValue(BooleanUtils.TRUE).add()
                .property().name(ScrimEndPointConfiguration.CONF_KEY_SYNC_IMPORT).type(ProviderConfigProperty.BOOLEAN_TYPE)
                .label("Enable import during sync").add().property()
                .name(ScrimEndPointConfiguration.CONF_KEY_SYNC_IMPORT_ACTION).type(ProviderConfigProperty.LIST_TYPE)
                .label("Import action").helpText("What to do when the user doesn't exists in Keycloak.")
                .options("NOTHING", "CREATE_LOCAL", "DELETE_REMOTE").defaultValue("CREATE_LOCAL").add().property()
                .name(ScrimEndPointConfiguration.CONF_KEY_SYNC_REFRESH).type(ProviderConfigProperty.BOOLEAN_TYPE)
                .label("Enable refresh during sync").name(ScrimEndPointConfiguration.CONF_KEY_LOG_ALL_SCIM_REQUESTS)
                .type(ProviderConfigProperty.BOOLEAN_TYPE).label("Log SCIM requests and responses")
                .helpText("If true, all sent SCIM requests and responses will be logged").add().build();
    }

    @Override
    public ScimEndpointConfigurationStorageProvider create(KeycloakSession session, ComponentModel model) {
        return new ScimEndpointConfigurationStorageProvider();
    }

    /**
     * Empty implementation : we used this {@link ScimEndpointConfigurationStorageProviderFactory} to generate Admin Console
     * page.
     */
    public static final class ScimEndpointConfigurationStorageProvider implements UserStorageProvider, UserRegistrationProvider {
        @Override
        public void close() {
            // Nothing to close here
        }

        @Override
        public UserModel addUser(RealmModel realm, String username) {
            return null;
        }

        @Override
        public boolean removeUser(RealmModel realm, UserModel user) {
            return true;
        }
    }
}
