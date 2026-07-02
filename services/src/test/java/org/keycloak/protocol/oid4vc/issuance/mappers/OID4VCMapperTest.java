package org.keycloak.protocol.oid4vc.issuance.mappers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.VCFormat;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.protocol.ProtocolMapperConfigException;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class OID4VCMapperTest {

    @Test
    public void shouldUseNamespaceClaimPathForMdoc() {
        OID4VCStaticClaimMapper mapper = new OID4VCStaticClaimMapper();
        mapper.setMapperModel(createStaticClaimMapperModel("given_name", "John", "org.iso.18013.5.1"), VCFormat.MSO_MDOC);

        Map<String, Object> claims = new HashMap<>();
        mapper.setClaim(claims, null);

        assertEquals(List.of("org.iso.18013.5.1", "given_name"), mapper.getMetadataAttributePath());
        assertEquals(Map.of("given_name", "John"), claims);

        Map<String, Object> prefixedClaims = new HashMap<>();
        mapper.setClaimWithMetadataPrefix(claims, prefixedClaims);
        assertEquals(Map.of("org.iso.18013.5.1", Map.of("given_name", "John")), prefixedClaims);
    }

    @Test
    public void shouldCreateNestedMdocClaimPaths() {
        OID4VCStaticClaimMapper mapper = new OID4VCStaticClaimMapper();
        mapper.setMapperModel(createStaticClaimMapperModel("address.street", "Main Street", "org.iso.18013.5.1"), VCFormat.MSO_MDOC);

        Map<String, Object> claims = new HashMap<>();
        mapper.setClaim(claims, null);

        assertEquals(List.of("org.iso.18013.5.1", "address", "street"), mapper.getMetadataAttributePath());
        assertEquals(Map.of("address.street", "Main Street"), claims);

        Map<String, Object> prefixedClaims = new HashMap<>();
        mapper.setClaimWithMetadataPrefix(claims, prefixedClaims);
        assertEquals(Map.of("org.iso.18013.5.1", Map.of("address", Map.of("street", "Main Street"))), prefixedClaims);
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

        assertEquals(Map.of("org.iso.18013.5.1", Map.of(
                "address", Map.of("street", "Main Street"),
                "employer", Map.of("street", "Work Street")
        )), prefixedClaims);
    }

    @Test
    public void shouldKeepNonMdocStaticClaimNamesLiteral() {
        OID4VCStaticClaimMapper mapper = new OID4VCStaticClaimMapper();
        mapper.setMapperModel(createStaticClaimMapperModel("address.street", "Main Street", null), VCFormat.JWT_VC);

        Map<String, Object> claims = new HashMap<>();
        mapper.setClaim(claims, null);

        assertEquals(Map.of("address.street", "Main Street"), claims);

        Map<String, Object> prefixedClaims = new HashMap<>();
        mapper.setClaimWithMetadataPrefix(claims, prefixedClaims);
        assertEquals(Map.of("credentialSubject", Map.of("address.street", "Main Street")), prefixedClaims);
    }

    @Test
    public void shouldNotPrefixClaimsForMapperWithoutClaimLookupPath() {
        // The type mapper has a fixed metadata path but writes no subject claim, so it must not copy the whole
        // claims map into the prefixed map under its metadata key.
        OID4VCTypeMapper mapper = new OID4VCTypeMapper();
        ProtocolMapperModel mapperModel = new ProtocolMapperModel();
        mapperModel.setConfig(new HashMap<>());
        mapper.setMapperModel(mapperModel, VCFormat.JWT_VC);

        Map<String, Object> claims = new HashMap<>();
        claims.put("given_name", "John");

        Map<String, Object> prefixedClaims = new HashMap<>();
        mapper.setClaimWithMetadataPrefix(claims, prefixedClaims);

        assertEquals(Map.of(), prefixedClaims);
    }

    @Test
    public void shouldPreserveNestedUserAttributeClaimPath() {
        OID4VCUserAttributeMapper mapper = new OID4VCUserAttributeMapper();
        mapper.setMapperModel(createUserAttributeMapperModel("address.street", "address"), VCFormat.JWT_VC);

        assertEquals(List.of("credentialSubject", "address", "street"), mapper.getMetadataAttributePath());
    }

    @Test
    public void shouldRejectMdocClaimMapperWithoutNamespace() {
        OID4VCStaticClaimMapper mapper = new OID4VCStaticClaimMapper();

        ProtocolMapperConfigException exception = assertThrows(ProtocolMapperConfigException.class,
                () -> mapper.validateMdocNamespace(VCFormat.MSO_MDOC, createStaticClaimMapperModel("given_name", "John", null)));
        assertEquals("mso_mdoc credential mappers require a non-empty 'mdoc.namespace' configuration.",
                exception.getMessage());
    }

    @Test
    public void shouldRejectMdocClaimMapperWithNullConfig() {
        OID4VCStaticClaimMapper mapper = new OID4VCStaticClaimMapper();

        assertThrows(ProtocolMapperConfigException.class,
                () -> mapper.validateMdocNamespace(VCFormat.MSO_MDOC, new ProtocolMapperModel()));
    }

    @Test
    public void shouldRejectMdocClaimMapperWithBlankNamespace() {
        OID4VCStaticClaimMapper mapper = new OID4VCStaticClaimMapper();

        assertThrows(ProtocolMapperConfigException.class,
                () -> mapper.validateMdocNamespace(VCFormat.MSO_MDOC, createStaticClaimMapperModel("given_name", "John", "  ")));
    }

    @Test
    public void shouldAcceptMdocClaimMapperWithNamespace() throws ProtocolMapperConfigException {
        OID4VCStaticClaimMapper mapper = new OID4VCStaticClaimMapper();

        mapper.validateMdocNamespace(VCFormat.MSO_MDOC, createStaticClaimMapperModel("given_name", "John", "org.iso.18013.5.1"));
    }

    @Test
    public void shouldNotRequireNamespaceForNonMdocFormat() throws ProtocolMapperConfigException {
        OID4VCStaticClaimMapper mapper = new OID4VCStaticClaimMapper();

        mapper.validateMdocNamespace(VCFormat.JWT_VC, createStaticClaimMapperModel("given_name", "John", null));
    }

    @Test
    public void shouldNotRequireNamespaceForMapperNotSupportingMdoc() throws ProtocolMapperConfigException {
        // The type mapper does not contribute a namespaced data element to mdoc, so it must not require a namespace.
        OID4VCTypeMapper mapper = new OID4VCTypeMapper();

        mapper.validateMdocNamespace(VCFormat.MSO_MDOC, createStaticClaimMapperModel("given_name", "John", null));
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
