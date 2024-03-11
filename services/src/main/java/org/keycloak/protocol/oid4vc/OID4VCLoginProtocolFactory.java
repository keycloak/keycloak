package org.keycloak.protocol.oid4vc;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.component.ComponentModel;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.protocol.LoginProtocolFactory;
import org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerEndpoint;
import org.keycloak.protocol.oid4vc.issuance.OffsetTimeProvider;
import org.keycloak.protocol.oid4vc.issuance.VCIssuerException;
import org.keycloak.protocol.oid4vc.issuance.mappers.OID4VPSubjectIdMapper;
import org.keycloak.protocol.oid4vc.issuance.mappers.OID4VPTargetRoleMapper;
import org.keycloak.protocol.oid4vc.issuance.mappers.OID4VPUserAttributeMapper;
import org.keycloak.protocol.oid4vc.issuance.signing.VCSigningServiceProviderFactory;
import org.keycloak.protocol.oid4vc.issuance.signing.VerifiableCredentialsSigningService;
import org.keycloak.protocol.oid4vc.model.Format;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.services.managers.AppAuthManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * This factory is required to get the capability of creating {@link OID4VCClientModel}.
 * Clients cannot be created without a matching protocol. We do not support logging into keycloak with it, nor any other
 * "native" functionality, thus we don't implement anything beside the
 *
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 */
public class OID4VCLoginProtocolFactory implements LoginProtocolFactory, OID4VCEnvironmentProviderFactory {

    private static final Logger LOGGER = Logger.getLogger(OID4VCLoginProtocolFactory.class);

    public static final String PROTOCOL_ID = "oid4vc";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String CLIENT_ROLES_MAPPER = "client-roles";
    private static final String SUBJECT_ID_MAPPER = "subject-id";
    private static final String USERNAME_MAPPER = "username";
    private static final String EMAIL_MAPPER = "email";
    private static final String LAST_NAME_MAPPER = "last-name";
    private static final String FIRST_NAME_MAPPER = "first-name";

    private Map<String, ProtocolMapperModel> builtins = new HashMap<>();

    @Override
    public void init(Config.Scope config) {
        builtins.put(CLIENT_ROLES_MAPPER, OID4VPTargetRoleMapper.create("id", "client roles"));
        builtins.put(SUBJECT_ID_MAPPER, OID4VPSubjectIdMapper.create("subject id", "id"));
        builtins.put(USERNAME_MAPPER, OID4VPUserAttributeMapper.create(USERNAME_MAPPER, "username", "username", false));
        builtins.put(EMAIL_MAPPER, OID4VPUserAttributeMapper.create(EMAIL_MAPPER, "email", "email", false));
        builtins.put(FIRST_NAME_MAPPER, OID4VPUserAttributeMapper.create(FIRST_NAME_MAPPER, "firstName", "firstName", false));
        builtins.put(LAST_NAME_MAPPER, OID4VPUserAttributeMapper.create(LAST_NAME_MAPPER, "lastName", "familyName", false));
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // no-op
    }

    @Override
    public void close() {
        // no-op
    }

    @Override
    public Map<String, ProtocolMapperModel> getBuiltinMappers() {
        return builtins;
    }

    private void addServiceFromComponent(Map<Format, VerifiableCredentialsSigningService> signingServices, KeycloakSession keycloakSession, ComponentModel componentModel) {
        ProviderFactory<VerifiableCredentialsSigningService> factory = keycloakSession
                .getKeycloakSessionFactory()
                .getProviderFactory(VerifiableCredentialsSigningService.class, componentModel.getProviderId());
        if (factory instanceof VCSigningServiceProviderFactory sspf) {
            signingServices.put(sspf.supportedFormat(), sspf.create(keycloakSession, componentModel));
        } else {
            throw new IllegalArgumentException(String.format("The component %s is not a VerifiableCredentialsSigningServiceProviderFactory", componentModel.getProviderId()));
        }

    }

    @Override
    public Object createProtocolEndpoint(KeycloakSession keycloakSession, EventBuilder event) {

        Map<Format, VerifiableCredentialsSigningService> signingServices = new HashMap<>();
        RealmModel realm = keycloakSession.getContext().getRealm();
        realm.getComponentsStream(realm.getId(), VerifiableCredentialsSigningService.class.getName())
                .forEach(cm -> addServiceFromComponent(signingServices, keycloakSession, cm));

        String issuerDid = Optional.ofNullable(keycloakSession.getContext().getRealm().getAttribute("issuerDid"))
                .orElseThrow(() -> new VCIssuerException("No issuerDid  configured."));

        return new OID4VCIssuerEndpoint(
                keycloakSession,
                issuerDid,
                signingServices,
                new AppAuthManager.BearerTokenAuthenticator(keycloakSession),
                OBJECT_MAPPER,
                new OffsetTimeProvider());
    }

    @Override
    public void createDefaultClientScopes(RealmModel newRealm, boolean addScopesToExistingClients) {
        LOGGER.debugf("Create default scopes for realm %s", newRealm.getName());

        ClientScopeModel naturalPersonScope = KeycloakModelUtils.getClientScopeByName(newRealm, "natural_person");
        if (naturalPersonScope == null) {
            LOGGER.debug("Add natural person scope");
            naturalPersonScope = newRealm.addClientScope("natural_person");
            naturalPersonScope.setDescription("OIDC$VP Scope, that adds all properties required for a natural person.");
            naturalPersonScope.setProtocol(PROTOCOL_ID);
            naturalPersonScope.addProtocolMapper(builtins.get(SUBJECT_ID_MAPPER));
            naturalPersonScope.addProtocolMapper(builtins.get(CLIENT_ROLES_MAPPER));
            naturalPersonScope.addProtocolMapper(builtins.get(EMAIL_MAPPER));
            naturalPersonScope.addProtocolMapper(builtins.get(FIRST_NAME_MAPPER));
            naturalPersonScope.addProtocolMapper(builtins.get(LAST_NAME_MAPPER));
            newRealm.addDefaultClientScope(naturalPersonScope, true);
        }
    }

    @Override
    public void setupClientDefaults(ClientRepresentation rep, ClientModel newClient) {
        // validate before setting the defaults
        OID4VCClientRegistrationProvider.validate(rep);
    }

    @Override
    public LoginProtocol create(KeycloakSession session) {
        return new OID4VCLoginProtocol(session);
    }

    @Override
    public String getId() {
        return PROTOCOL_ID;
    }

}