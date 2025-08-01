package org.keycloak.services.clientregistration.openid_federation;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.common.util.Time;
import org.keycloak.events.Errors;
import org.keycloak.exceptions.InvalidTrustChainException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OpenIdFederationConfig;
import org.keycloak.models.OpenIdFederationGeneralConfig;
import org.keycloak.models.RealmModel;
import org.keycloak.models.enums.ClientRegistrationTypeEnum;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.openid_federation.EntityStatement;
import org.keycloak.representations.openid_federation.EntityStatementExplicitResponse;
import org.keycloak.representations.openid_federation.RPMetadata;
import org.keycloak.representations.openid_federation.TrustChainResolution;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.Urls;
import org.keycloak.services.clientregistration.AbstractClientRegistrationProvider;
import org.keycloak.services.clientregistration.oidc.DescriptionConverter;
import org.keycloak.services.trustchain.OpenIdFederationTrustChainProcessorFactory;
import org.keycloak.services.trustchain.TrustChainProcessor;
import org.keycloak.urls.UrlType;
import org.keycloak.util.TokenUtil;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OpenIdFederationClientRegistrationService extends AbstractClientRegistrationProvider {

    private static final Logger logger = Logger.getLogger(OpenIdFederationClientRegistrationService.class);
    private final TrustChainProcessor trustChainProcessor;

    public OpenIdFederationClientRegistrationService(KeycloakSession session) {
        super(session);
        this.trustChainProcessor = session.getProvider(TrustChainProcessor.class, OpenIdFederationTrustChainProcessorFactory.PROVIDER_ID);
    }

    @POST
    @Consumes({"application/entity-statement+jwt", "application/trust-chain+json"})
    public Response explicitClientRegistration(String body, @Context HttpHeaders headers) {
        RealmModel realm = session.getContext().getRealm();
        OpenIdFederationGeneralConfig config = realm.getOpenIdFederationGeneralConfig();
        //TODO In future, we need to check for being OP and support explicit registration
        //Avoid check now as only posibility is OP with explicit registration
        if (!realm.isOpenIdFederationEnabled() || config.getAuthorityHints().isEmpty()) {
            throw new ErrorResponseException(Errors.INVALID_REQUEST, "Explicit OpenID Federation Client Registration is not supported in this realm", Response.Status.BAD_REQUEST);
        }
        checkSsl();

        if ("application/entity-statement+jwt".equals(headers.getMediaType().toString())) {
            EntityStatement statement = null;
            try {
                statement = trustChainProcessor.parseAndValidateSelfSigned(body);
            } catch (InvalidTrustChainException ex) {
                ex.printStackTrace();
                throw new ErrorResponseException(Errors.INVALID_REQUEST, "Entity statement is not valid", Response.Status.BAD_REQUEST);
            }

            validationRules(statement);

            Set<String> trustAnchorIds = config.getOpenIdFederationList().stream().map(OpenIdFederationConfig::getTrustAnchor).collect(Collectors.toSet());

            logger.info("starting validating trust chains");
            TrustChainResolution validTrustChain = trustChainProcessor.constructTrustChains(statement, trustAnchorIds);

            if (validTrustChain == null) {
                throw new ErrorResponseException(Errors.INVALID_TRUST_ANCHOR, "No trusted trust anchor could be found", Response.Status.NOT_FOUND);
            }
            //TODO EntityFromTA will contains always actual RPMetadata after enforicng policies
            RPMetadata rPMetadata = validTrustChain.getEntityFromTA() != null ?  validTrustChain.getEntityFromTA().getMetadata().getRelyingPartyMetadata() : statement.getMetadata().getRelyingPartyMetadata();
            if (rPMetadata.getJwks() == null && rPMetadata.getJwksUri() == null) {
                rPMetadata.setJwks(statement.getJwks());
            }
            rPMetadata.setClientId(statement.getSubject());

            RPMetadata rPMetadataResponse;
            try {
                if (session.getContext().getRealm().getClientByClientId(rPMetadata.getClientId()) == null) {
                    ClientRepresentation client = createOidcClient(rPMetadata, session, statement.getExp());
                    URI uri = session.getContext().getUri().getAbsolutePathBuilder().path(client.getClientId()).build();
                    rPMetadataResponse = DescriptionConverter.toExternalResponse(session, client, uri, RPMetadata.class);
                } else {
                    ClientRepresentation client = updateOidcClient(rPMetadata.getClientId(), rPMetadata, session, statement.getExp());
                    URI uri = session.getContext().getUri().getAbsolutePathBuilder().path(client.getClientId()).build();
                    rPMetadataResponse = DescriptionConverter.toExternalResponse(session, client, uri, RPMetadata.class);
                }
            } catch (Exception e) {
                logger.error("The following error was thrown during OpenId Federation Client explicit registration", e);
                throw new ErrorResponseException(Errors.INVALID_METADATA, "Client metadata invalid", Response.Status.BAD_REQUEST);
            }

            rPMetadataResponse.setClientRegistrationTypes(Stream.of(ClientRegistrationTypeEnum.EXPLICIT.getValue()).collect(Collectors.toList()));
            rPMetadataResponse.setClientIdIssuedAt(Time.currentTime());
            rPMetadataResponse.setCommonMetadata(rPMetadata.getCommonMetadata());
            EntityStatementExplicitResponse responseStatement = new EntityStatementExplicitResponse(statement, Urls.realmIssuer(session.getContext().getUri(UrlType.FRONTEND).getBaseUri(), session.getContext().getRealm().getName()), rPMetadataResponse, validTrustChain.getTrustAnchorId(), validTrustChain.getParsedChain().get(0).getIssuer());
            responseStatement.type(TokenUtil.EXPLICIT_REGISTRATION_RESPONSE_JWT);
            String token = session.tokens().encodeForOpenIdFederation(responseStatement);
            return Response.ok(token).header("Content-Type", TokenUtil.APPLICATION_EXPLICIT_REGISTRATION_RESPONSE_JWT).build();


        } else {
            // TODO Handle Trust Chain
            throw new ErrorResponseException("not_implemented", "Trust chain handling is not yet implemented", Response.Status.NOT_IMPLEMENTED);
        }
    }

    private void validationRules(EntityStatement statement) {
        if (statement.getIssuer() == null) {
            throw new ErrorResponseException(Errors.INVALID_ISSUER, "No issuer in the request.", Response.Status.NOT_FOUND);
        }
        if (statement.getSubject() == null) {
            throw new ErrorResponseException(Errors.INVALID_SUBJECT, "No issuer in the request.", Response.Status.NOT_FOUND);
        }
        if (statement.getIat() == null && LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) > statement.getIat()) {
            throw new ErrorResponseException(Errors.INVALID_REQUEST, "Iat must exist and be before now.", Response.Status.BAD_REQUEST);
        }
        if (statement.getExp() == null && LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) < statement.getExp()){
            throw new ErrorResponseException(Errors.INVALID_REQUEST, "Exp must exist and be before now.", Response.Status.BAD_REQUEST);
        }
        if (statement.getAuthorityHints() == null || statement.getAuthorityHints().isEmpty()) {
            throw new ErrorResponseException(Errors.INVALID_REQUEST, "No authorityHints in the request.", Response.Status.BAD_REQUEST);
        }
        if (statement.getMetadata() == null || statement.getMetadata().getRelyingPartyMetadata() == null) {
            throw new ErrorResponseException(Errors.INVALID_REQUEST, "No relaying party metadata in the request.", Response.Status.BAD_REQUEST);
        }
        if (!statement.getIssuer().trim().equals(statement.getSubject().trim())) {
            throw new ErrorResponseException(Errors.INVALID_ISSUER, "The registration request issuer differs from the subject.", Response.Status.NOT_FOUND);
        }
        if (statement.getAudience() == null || !statement.getAudience()[0].equals(Urls.realmIssuer(session.getContext().getUri(UrlType.FRONTEND).getBaseUri(), session.getContext().getRealm().getName()))) {
            throw new ErrorResponseException(Errors.INVALID_REQUEST, "Aud must contain OP entity Identifier", Response.Status.BAD_REQUEST);
        }
    }

    private void checkSsl() {
        if (!session.getContext().getUri().getBaseUri().getScheme().equals("https") && session.getContext().getRealm().getSslRequired().isRequired(session.getContext().getConnection())) {
            throw new ErrorResponseException(Errors.INVALID_REQUEST, "HTTPS required", Response.Status.FORBIDDEN);
        }
    }

}
