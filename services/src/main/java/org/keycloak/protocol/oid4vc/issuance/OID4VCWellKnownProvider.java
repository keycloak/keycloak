package org.keycloak.protocol.oid4vc.issuance;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oid4vc.OID4VCAbstractWellKnownProvider;
import org.keycloak.protocol.oidc.OIDCWellKnownProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerEndpoint.GRANT_TYPE_PRE_AUTHORIZED_CODE;

/**
 * Extension of the OIDC Wellknown Provider to also support the pre-authorized grant type
 *
 * TODO: might be removed in the future
 *
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 */
public class OID4VCWellKnownProvider extends OID4VCAbstractWellKnownProvider {

    public OID4VCWellKnownProvider(KeycloakSession keycloakSession, ObjectMapper objectMapper) {
        super(keycloakSession, objectMapper);
    }

    @Override
    public void close() {
        // no-op
    }

    @Override
    public Object getConfig() {
        // some wallets use the openid-config well-known to also gather the issuer metadata. In
        // the future(when everyone uses .well-known/openid-credential-issuer), that can be removed.
        Map<String, Object> configAsMap = objectMapper.convertValue(
                new OIDCWellKnownProvider(keycloakSession, null, false).getConfig(),
                Map.class);

        List<String> supportedGrantTypes = Optional.ofNullable(configAsMap.get("grant_types_supported"))
                .map(grantTypesObject -> objectMapper.convertValue(
                        grantTypesObject, new TypeReference<List<String>>() {
                        })).orElse(new ArrayList<>());
        // newly invented by OID4VCI and supported by this implementation
        supportedGrantTypes.add(GRANT_TYPE_PRE_AUTHORIZED_CODE);
        configAsMap.put("grant_types_supported", supportedGrantTypes);
        configAsMap.put("credential_endpoint", getCredentialsEndpoint(keycloakSession.getContext()));

        return configAsMap;
    }


}