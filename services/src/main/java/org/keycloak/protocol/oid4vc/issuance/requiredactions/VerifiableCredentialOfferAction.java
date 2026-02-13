package org.keycloak.protocol.oid4vc.issuance.requiredactions;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import jakarta.ws.rs.core.Response;

import org.keycloak.Config;
import org.keycloak.authentication.InitiatedActionSupport;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.events.Details;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.oid4vc.OID4VCEnvironmentProviderFactory;
import org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerWellKnownProvider;
import org.keycloak.protocol.oid4vc.issuance.OffsetTimeProvider;
import org.keycloak.protocol.oid4vc.issuance.TimeProvider;
import org.keycloak.protocol.oid4vc.issuance.credentialoffer.CredentialOfferState;
import org.keycloak.protocol.oid4vc.issuance.credentialoffer.CredentialOfferStorage;
import org.keycloak.protocol.oid4vc.model.CredentialsOffer;
import org.keycloak.protocol.oid4vc.model.PreAuthorizedCode;
import org.keycloak.protocol.oid4vc.model.PreAuthorizedGrant;
import org.keycloak.protocol.oid4vc.utils.OID4VCUtil;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.zxing.WriterException;
import org.jboss.logging.Logger;

import static org.keycloak.constants.OID4VCIConstants.CLIENT_ID;
import static org.keycloak.constants.OID4VCIConstants.CREDENTIAL_CONFIGURATION_ID;
import static org.keycloak.constants.OID4VCIConstants.CREDENTIAL_OFFER_NONCE;
import static org.keycloak.constants.OID4VCIConstants.PRE_AUTHORIZED;
import static org.keycloak.constants.OID4VCIConstants.VERIFIABLE_CREDENTIAL_OFFER_PROVIDER_ID;
import static org.keycloak.events.Details.REASON;
import static org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerEndpoint.CODE_LIFESPAN_REALM_ATTRIBUTE_KEY;
import static org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerEndpoint.DEFAULT_CODE_LIFESPAN_S;
import static org.keycloak.protocol.oid4vc.model.ErrorType.INVALID_CREDENTIAL_OFFER_REQUEST;
import static org.keycloak.protocol.oid4vc.model.ErrorType.MISSING_CREDENTIAL_CONFIG;
import static org.keycloak.protocol.oid4vc.model.ErrorType.UNKNOWN_CREDENTIAL_CONFIGURATION;

public class VerifiableCredentialOfferAction implements RequiredActionProvider, RequiredActionFactory, OID4VCEnvironmentProviderFactory {

    private static final Logger logger = Logger.getLogger(VerifiableCredentialOfferAction.class);

    private final TimeProvider timeProvider;

    public VerifiableCredentialOfferAction() {
        this.timeProvider = new OffsetTimeProvider();
    }

    @Override
    public String getDisplayText() {
        return "Register Verifiable Credential Offer";
    }

    @Override
    public RequiredActionProvider create(KeycloakSession session) {
        return this;
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public String getId() {
        return VERIFIABLE_CREDENTIAL_OFFER_PROVIDER_ID;
    }

    @Override
    public void evaluateTriggers(RequiredActionContext context) {
        logger.tracef("Evaluate triggers invoked for '%s'", context.getAction());
    }

    @Override
    public void requiredActionChallenge(RequiredActionContext context) {
        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        RealmModel realm = context.getRealm();
        UserModel user = context.getUser();
        EventBuilder event = context.getEvent();
        event.event(EventType.VERIFIABLE_CREDENTIAL_CREATE_OFFER);

        String credentialOfferConfig = authSession.getClientNote(Constants.KC_ACTION_PARAMETER);
        if (credentialOfferConfig == null) {
            event.error(MISSING_CREDENTIAL_CONFIG.getValue());
            context.ignore();
            return;
        }

        CredentialOfferActionConfig actionConfig = getActionConfig(credentialOfferConfig);
        if (actionConfig == null) {
            event.detail(REASON, "Parameter of AIA in incorrect format. KC action parameter value was: " + credentialOfferConfig)
                    .error(INVALID_CREDENTIAL_OFFER_REQUEST.getValue());
            context.ignore();
            return;
        }
        if (actionConfig.getCredentialConfigurationId() == null) {
            event.detail(REASON, "Credential configuration ID was missing. KC action parameter value was: " + credentialOfferConfig)
                    .error(INVALID_CREDENTIAL_OFFER_REQUEST.getValue());
            context.ignore();
            return;
        }
        String credentialConfigId = actionConfig.getCredentialConfigurationId();

        ClientScopeModel clientScope = OID4VCUtil.getClientScopeByCredentialConfigId(context.getSession(), realm, credentialConfigId);
        if (clientScope == null) {
            event.detail(Details.CREDENTIAL_TYPE, credentialConfigId);
            event.detail(REASON, "Client scope was not found for credential configuration ID: " + credentialConfigId)
                    .error(UNKNOWN_CREDENTIAL_CONFIGURATION.getValue());
            context.ignore();
            return;
        }

        logger.debugf("Required action challenge invoked for provider '%s' and config '%s'", context.getAction(), actionConfig);

        String nonce = context.getAuthenticationSession().getAuthNote(CREDENTIAL_OFFER_NONCE);
        if (nonce == null) {
            CredentialOfferState credOfferState = createCredentialsOffer(context.getSession(), realm, user, event, actionConfig);
            nonce = credOfferState.getNonce();
            context.getAuthenticationSession().setAuthNote(CREDENTIAL_OFFER_NONCE, credOfferState.getNonce());
        }

        LoginFormsProvider form = context.form();
        try {
            String displayName = OID4VCUtil.getCredentialDisplayName(context.getSession(), context.getUser(), clientScope);
            form.setAttribute("credentialOffer", new CredentialOfferBean(context.getSession(), nonce));
            form.setAttribute("credentialDisplayName", displayName);
        } catch (WriterException | IOException ex) {
            String message = "Error when generating credential-offer QR code: " + ex.getMessage();
            event.detail(REASON, message)
                    .error(INVALID_CREDENTIAL_OFFER_REQUEST.getValue());
            context.ignore();
            return;
        }

        Response response = form.createForm("oid4vc-credential-offer.ftl");
        context.challenge(response);
    }


    private CredentialOfferState createCredentialsOffer(KeycloakSession session, RealmModel realm, UserModel user, EventBuilder event, CredentialOfferActionConfig actionConfig) {
        boolean preAuthorized = actionConfig.getPreAuthorized() != null && actionConfig.getPreAuthorized();

        String credentialConfigurationId = actionConfig.getCredentialConfigurationId();
        event.detail(Details.CREDENTIAL_TYPE, credentialConfigurationId);

        CredentialsOffer credOffer = new CredentialsOffer()
                .setCredentialIssuer(OID4VCIssuerWellKnownProvider.getIssuer(session.getContext()))
                .setCredentialConfigurationIds(List.of(credentialConfigurationId));

        int preAuthorizedCodeLifeSpan = Optional.ofNullable(realm.getAttribute(CODE_LIFESPAN_REALM_ATTRIBUTE_KEY))
                .map(Integer::valueOf)
                .orElse(DEFAULT_CODE_LIFESPAN_S);
        int expiration = timeProvider.currentTimeSeconds() + preAuthorizedCodeLifeSpan;
        CredentialOfferState offerState = new CredentialOfferState(credOffer, actionConfig.getClientId(), user.getId(), expiration);

        if (preAuthorized) {
            String code = "urn:oid4vci:code:" + SecretGenerator.getInstance().randomString(64);
            credOffer.setGrants(new PreAuthorizedGrant().setPreAuthorizedCode(
                    new PreAuthorizedCode().setPreAuthorizedCode(code)));
        }

        CredentialOfferStorage offerStorage = session.getProvider(CredentialOfferStorage.class);
        offerStorage.putOfferState(session, offerState);

        logger.debugf("Stored credential offer state: [ids=%s, cid=%s, uid=%s, nonce=%s]",
                credOffer.getCredentialConfigurationIds(), offerState.getClientId(), offerState.getUserId(), offerState.getNonce());

        // Add event details
        event.clone().detail(Details.VERIFIABLE_CREDENTIAL_PRE_AUTHORIZED, String.valueOf(preAuthorized));
        if (offerState.getUserId() != null) {
            event.detail(Details.VERIFIABLE_CREDENTIAL_TARGET_USER_ID, offerState.getUserId());
        }
        if (offerState.getClientId() != null) {
            event.detail(Details.VERIFIABLE_CREDENTIAL_TARGET_CLIENT_ID, offerState.getClientId());
        }
        event.success();

        return offerState;
    }

    @Override
    public void processAction(RequiredActionContext context) {
        // Just continue to the login once user consumed his credential offer
        logger.tracef("Process action invoked for: " + context.getAction());
        context.success();
    }

    @Override
    public void close() {
    }

    @Override
    public InitiatedActionSupport initiatedActionSupport() {
        return InitiatedActionSupport.SUPPORTED;
    }

    @Override
    public void initiatedActionCanceled(KeycloakSession session, AuthenticationSessionModel authSession) {
        // User cancelled AIA and rejected credential-offer. Should remove credential-offer
        String nonce = authSession.getAuthNote(CREDENTIAL_OFFER_NONCE);
        if (nonce != null) {
            CredentialOfferStorage offerStore = session.getProvider(CredentialOfferStorage.class);
            CredentialOfferState state = offerStore.findOfferStateByNonce(session, nonce);
            if (state != null) {
                offerStore.removeOfferState(session, state);
            }
        }

        RequiredActionProvider.super.initiatedActionCanceled(session, authSession);
    }


    public static class CredentialOfferActionConfig {

        @JsonProperty(CREDENTIAL_CONFIGURATION_ID)
        private String credentialConfigurationId;

        @JsonProperty(CLIENT_ID)
        private String clientId;

        @JsonProperty(PRE_AUTHORIZED)
        private Boolean preAuthorized;

        public String getCredentialConfigurationId() {
            return credentialConfigurationId;
        }

        public void setCredentialConfigurationId(String credentialConfigurationId) {
            this.credentialConfigurationId = credentialConfigurationId;
        }

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public Boolean getPreAuthorized() {
            return preAuthorized;
        }

        public void setPreAuthorized(Boolean preAuthorized) {
            this.preAuthorized = preAuthorized;
        }

        @Override
        public String toString() {
            return "CredentialOfferUserConfig{" +
                    "credentialConfigurationId='" + credentialConfigurationId + '\'' +
                    ", clientId='" + clientId + '\'' +
                    ", preAuthorized='" + preAuthorized + '\'' +
                    '}';
        }

        // Encode to the string, which can be used as parameter of AIA
        public String asEncodedParameter() throws IOException {
            byte[] bytes = JsonSerialization.writeValueAsBytes(this);
            return Base64Url.encode(bytes);
        }

        // Encode to the string, which can be used as parameter of AIA
        public static VerifiableCredentialOfferAction.CredentialOfferActionConfig decodeConfig(String configStr) throws IOException {
            byte[] bytes = Base64Url.decode(configStr);
            return JsonSerialization.readValue(bytes, VerifiableCredentialOfferAction.CredentialOfferActionConfig.class);
        }
    }

    private CredentialOfferActionConfig getActionConfig(String credentialOfferUserConfig) {
        try {
            return CredentialOfferActionConfig.decodeConfig(credentialOfferUserConfig);
        } catch (IOException ioe) {
            logger.warnf("Parameter of %s AIA in incorrect format. Parameter value was: %s", VERIFIABLE_CREDENTIAL_OFFER_PROVIDER_ID, credentialOfferUserConfig);
            return null;
        }
    }

}
