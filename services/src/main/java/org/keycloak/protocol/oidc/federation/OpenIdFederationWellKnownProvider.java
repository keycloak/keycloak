package org.keycloak.protocol.oidc.federation;

import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.UriInfo;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OpenIdFederationGeneralConfig;
import org.keycloak.models.RealmModel;
import org.keycloak.models.enums.ClientRegistrationTypeEnum;
import org.keycloak.models.enums.EntityTypeEnum;
import org.keycloak.protocol.oidc.OIDCWellKnownProvider;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.representations.openid_federation.CommonMetadata;
import org.keycloak.representations.openid_federation.EntityStatement;
import org.keycloak.representations.openid_federation.OpenIdFederationEntity;
import org.keycloak.representations.openid_federation.Metadata;
import org.keycloak.representations.openid_federation.OPMetadata;
import org.keycloak.services.Urls;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.services.trustchain.OpenIdFederationTrustChainProcessorFactory;
import org.keycloak.services.trustchain.TrustChainProcessor;
import org.keycloak.urls.UrlType;
import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.OpenIdFederationUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.stream.Collectors;

public class OpenIdFederationWellKnownProvider extends OIDCWellKnownProvider {

    private final TrustChainProcessor trustChainProcessor;

    public OpenIdFederationWellKnownProvider(KeycloakSession session) {
        super(session);
        this.trustChainProcessor = session.getProvider(TrustChainProcessor.class, OpenIdFederationTrustChainProcessorFactory.PROVIDER_ID);
    }

    @Override
    public Object getConfig() {

        RealmModel realm = session.getContext().getRealm();
        OpenIdFederationGeneralConfig openIdFederationConfig = realm.getOpenIdFederationGeneralConfig();

        if (openIdFederationConfig ==  null || openIdFederationConfig.getOpenIdFederationList() == null || openIdFederationConfig.getOpenIdFederationList().isEmpty())
            throw new NotFoundException();

        UriInfo frontendUriInfo = session.getContext().getUri(UrlType.FRONTEND);
        UriInfo backendUriInfo = session.getContext().getUri(UrlType.BACKEND);
        Metadata metadata = new Metadata();
        CommonMetadata common = OpenIdFederationUtils.commonMetadata(openIdFederationConfig);

        if (openIdFederationConfig.getOpenIdFederationList().stream().flatMap(x -> x.getEntityTypes().stream()).collect(Collectors.toSet()).contains(EntityTypeEnum.OPENID_PROVIDER)) {
            OPMetadata opMetadata;
            try {
                opMetadata = from(((OIDCConfigurationRepresentation) super.getConfig()));
            } catch (IOException e) {
                throw new InternalServerErrorException("Could not form the configuration response");
            }

            Set<ClientRegistrationTypeEnum> registrationTypes = openIdFederationConfig.getOpenIdFederationList().stream().flatMap(x -> x.getClientRegistrationTypesSupported().stream()).collect(Collectors.toSet());
            if (registrationTypes.contains(ClientRegistrationTypeEnum.EXPLICIT)) {
                opMetadata.setFederationRegistrationEndpoint(backendUriInfo.getBaseUriBuilder().clone().path(RealmsResource.class).path(RealmsResource.class, "getOpenIdFederationClientsService").build(realm.getName()).toString());
            }
            opMetadata.setClientRegistrationTypes(registrationTypes.stream().map(ClientRegistrationTypeEnum::getValue).collect(Collectors.toList()));
            opMetadata.setContacts(openIdFederationConfig.getContacts());
            opMetadata.setLogoUri(openIdFederationConfig.getLogoUri());
            opMetadata.setPolicyUri(openIdFederationConfig.getPolicyUri());
            opMetadata.setCommonMetadata(common);
            metadata.setOpenIdProviderMetadata(opMetadata);
        }

        if (openIdFederationConfig.getFederationResolveEndpoint() != null || openIdFederationConfig.getFederationHistoricalKeysEndpoint() != null ||
                openIdFederationConfig.getOrganizationName() != null || ! openIdFederationConfig.getContacts().isEmpty() ||
                openIdFederationConfig.getOrganizationUri() != null || openIdFederationConfig.getPolicyUri() != null || openIdFederationConfig.getLogoUri() != null) {
            OpenIdFederationEntity federationEntity = getOpenIdFederationEntity(openIdFederationConfig, common);
            metadata.setFederationEntity(federationEntity);
        }

        EntityStatement entityStatement = new EntityStatement(Urls.realmIssuer(frontendUriInfo.getBaseUri(), realm.getName()), Long.valueOf(openIdFederationConfig.getLifespan()), new ArrayList<>(openIdFederationConfig.getAuthorityHints()), trustChainProcessor.getKeySet(), metadata);

        return session.tokens().encodeForOpenIdFederation(entityStatement);
    }

    private static OPMetadata from(OIDCConfigurationRepresentation representation) throws IOException {
        return JsonSerialization.readValue(JsonSerialization.writeValueAsString(representation), OPMetadata.class);
    }

    private static OpenIdFederationEntity getOpenIdFederationEntity(OpenIdFederationGeneralConfig openIdFederationConfig, CommonMetadata common) {
        OpenIdFederationEntity federationEntity = new OpenIdFederationEntity();
        federationEntity.setFederationResolveEndpoint(openIdFederationConfig.getFederationResolveEndpoint());
        federationEntity.setFederationHistoricalKeysEndpoint(openIdFederationConfig.getFederationHistoricalKeysEndpoint());
        federationEntity.setContacts(openIdFederationConfig.getContacts());
        federationEntity.setLogoUri(openIdFederationConfig.getLogoUri());
        federationEntity.setPolicyUri(openIdFederationConfig.getPolicyUri());
        federationEntity.setCommonMetadata(common);
        return federationEntity;
    }

}
