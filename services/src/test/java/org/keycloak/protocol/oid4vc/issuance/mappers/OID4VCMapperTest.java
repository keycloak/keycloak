package org.keycloak.protocol.oid4vc.issuance.mappers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.VCFormat;
import org.keycloak.models.ProtocolMapperModel;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class OID4VCMapperTest {

    @Test
    public void shouldUseNamespaceClaimPathForMdoc() {
        OID4VCStaticClaimMapper mapper = new OID4VCStaticClaimMapper();
        mapper.setMapperModel(createStaticClaimMapperModel("given_name", "John", "org.iso.18013.5.1"), VCFormat.MSO_MDOC);

        Map<String, Object> claims = new HashMap<>();
        mapper.setClaim(claims, null);

        assertThat(mapper.getMetadataAttributePath(), equalTo(List.of("org.iso.18013.5.1", "given_name")));
        assertThat(claims, equalTo(Map.of("given_name", "John")));

        Map<String, Object> prefixedClaims = new HashMap<>();
        mapper.setClaimWithMetadataPrefix(claims, prefixedClaims);
        assertThat(prefixedClaims, equalTo(Map.of("org.iso.18013.5.1", Map.of("given_name", "John"))));
    }

    @Test
    public void shouldCreateNestedMdocClaimPaths() {
        OID4VCStaticClaimMapper mapper = new OID4VCStaticClaimMapper();
        mapper.setMapperModel(createStaticClaimMapperModel("address.street", "Main Street", "org.iso.18013.5.1"), VCFormat.MSO_MDOC);

        Map<String, Object> claims = new HashMap<>();
        mapper.setClaim(claims, null);

        assertThat(mapper.getMetadataAttributePath(), equalTo(List.of("org.iso.18013.5.1", "address", "street")));
        assertThat(claims, equalTo(Map.of("address.street", "Main Street")));

        Map<String, Object> prefixedClaims = new HashMap<>();
        mapper.setClaimWithMetadataPrefix(claims, prefixedClaims);
        assertThat(prefixedClaims, equalTo(Map.of("org.iso.18013.5.1", Map.of("address", Map.of("street", "Main Street")))));
    }

    @Test
    public void shouldKeepMdocClaimPathsSeparateWhenLeafNamesMatch() {
        OID4VCStaticClaimMapper addressMapper = new OID4VCStaticClaimMapper();
        addressMapper.setMapperModel(createStaticClaimMapperModel("address.street", "Main Street", "org.iso.18013.5.1"), VCFormat.MSO_MDOC);

        OID4VCStaticClaimMapper employerMapper = new OID4VCStaticClaimMapper();
        employerMapper.setMapperModel(createStaticClaimMapperModel("employer.street", "Work Street", "org.iso.18013.5.1"), VCFormat.MSO_MDOC);

        Map<String, Object> claims = new HashMap<>();
        addressMapper.setClaim(claims, null);
        employerMapper.setClaim(claims, null);

        Map<String, Object> prefixedClaims = new HashMap<>();
        addressMapper.setClaimWithMetadataPrefix(claims, prefixedClaims);
        employerMapper.setClaimWithMetadataPrefix(claims, prefixedClaims);

        assertThat(prefixedClaims, equalTo(Map.of("org.iso.18013.5.1", Map.of(
                "address", Map.of("street", "Main Street"),
                "employer", Map.of("street", "Work Street")
        ))));
    }

    @Test
    public void shouldKeepNonMdocStaticClaimNamesLiteral() {
        OID4VCStaticClaimMapper mapper = new OID4VCStaticClaimMapper();
        mapper.setMapperModel(createStaticClaimMapperModel("address.street", "Main Street", null), VCFormat.JWT_VC);

        Map<String, Object> claims = new HashMap<>();
        mapper.setClaim(claims, null);

        assertThat(claims, equalTo(Map.of("address.street", "Main Street")));

        Map<String, Object> prefixedClaims = new HashMap<>();
        mapper.setClaimWithMetadataPrefix(claims, prefixedClaims);
        assertThat(prefixedClaims, equalTo(Map.of("credentialSubject", Map.of("address.street", "Main Street"))));
    }

    @Test
    public void shouldPreserveNestedUserAttributeClaimPath() {
        OID4VCUserAttributeMapper mapper = new OID4VCUserAttributeMapper();
        mapper.setMapperModel(createUserAttributeMapperModel("address.street", "address"), VCFormat.JWT_VC);

        assertThat(mapper.getMetadataAttributePath(), equalTo(List.of("credentialSubject", "address", "street")));
    }

    private static ProtocolMapperModel createStaticClaimMapperModel(String claimName, String claimValue, String mdocNamespace) {
        ProtocolMapperModel mapperModel = new ProtocolMapperModel();
        Map<String, String> config = new HashMap<>();
        config.put(OID4VCMapper.CLAIM_NAME, claimName);
        config.put(OID4VCStaticClaimMapper.STATIC_CLAIM_KEY, claimValue);
        if (mdocNamespace != null) {
            config.put(OID4VCMapper.MDOC_NAMESPACE, mdocNamespace);
        }
        mapperModel.setConfig(config);
        return mapperModel;
    }

    private static ProtocolMapperModel createUserAttributeMapperModel(String claimName, String userAttribute) {
        ProtocolMapperModel mapperModel = new ProtocolMapperModel();
        Map<String, String> config = new HashMap<>();
        config.put(OID4VCMapper.CLAIM_NAME, claimName);
        config.put(OID4VCMapper.USER_ATTRIBUTE_KEY, userAttribute);
        mapperModel.setConfig(config);
        return mapperModel;
    }
}
