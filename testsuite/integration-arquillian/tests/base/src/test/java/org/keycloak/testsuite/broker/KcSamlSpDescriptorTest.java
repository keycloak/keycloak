package org.keycloak.testsuite.broker;

import java.io.Closeable;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.crypto.dsig.XMLSignature;

import org.keycloak.broker.provider.ConfigConstants;
import org.keycloak.broker.saml.SAMLIdentityProviderConfig;
import org.keycloak.broker.saml.mappers.AttributeToRoleMapper;
import org.keycloak.broker.saml.mappers.UserAttributeMapper;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyStatus;
import org.keycloak.crypto.KeyUse;
import org.keycloak.dom.saml.v2.metadata.EntityDescriptorType;
import org.keycloak.dom.saml.v2.metadata.KeyDescriptorType;
import org.keycloak.dom.saml.v2.metadata.KeyTypes;
import org.keycloak.dom.saml.v2.metadata.SPSSODescriptorType;
import org.keycloak.dom.xmlsec.w3.xmlenc.EncryptionMethodType;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderMapperSyncMode;
import org.keycloak.protocol.saml.SAMLEncryptionAlgorithms;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.KeysMetadataRepresentation;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.processing.core.parsers.saml.SAMLParser;
import org.keycloak.testsuite.updaters.IdentityProviderAttributeUpdater;

import com.google.common.collect.ImmutableMap;
import org.apache.tools.ant.filters.StringInputStream;
import org.junit.Assert;
import org.junit.Test;

import static org.keycloak.testsuite.util.KeyUtils.generateNewRealmKey;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertTrue;

public class KcSamlSpDescriptorTest extends AbstractBrokerTest {

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return KcSamlBrokerConfiguration.INSTANCE;
    }

    @Test
    public void testAttributeConsumingServiceNameInSpMetadata() throws IOException, ParsingException, URISyntaxException {
        try (Closeable idpUpdater = new IdentityProviderAttributeUpdater(identityProviderResource)
            .setAttribute(SAMLIdentityProviderConfig.ATTRIBUTE_CONSUMING_SERVICE_NAME, "My Attribute Set")
            .update())
        {

            SPSSODescriptorType spDescriptor = getExportedSamlProvider();

            //attribute mappers do not exist- no AttributeConsumingService
            assertThat(spDescriptor.getAttributeConsumingService(), empty());
        }
    }

    @Test
    public void testAttributeConsumingServiceMappersInSpMetadataWithoutServiceName() throws IOException, ParsingException, URISyntaxException {
        try (Closeable idpUpdater = new IdentityProviderAttributeUpdater(identityProviderResource)
            .setAttribute(SAMLIdentityProviderConfig.ATTRIBUTE_CONSUMING_SERVICE_INDEX, "12")
            .update())
        {
            IdentityProviderMapperRepresentation attrMapperEmail = new IdentityProviderMapperRepresentation();
            attrMapperEmail.setName("attribute-mapper-email");
            attrMapperEmail.setIdentityProviderMapper(UserAttributeMapper.PROVIDER_ID);
            attrMapperEmail.setConfig(ImmutableMap.<String,String>builder()
              .put(IdentityProviderMapperModel.SYNC_MODE, IdentityProviderMapperSyncMode.INHERIT.toString())
              .put(UserAttributeMapper.ATTRIBUTE_NAME, "email_attr_name")
              .put(UserAttributeMapper.ATTRIBUTE_FRIENDLY_NAME, "email_attr_friendlyname")
              .put(UserAttributeMapper.USER_ATTRIBUTE, "email")
              .build());
            attrMapperEmail.setIdentityProviderAlias(bc.getIDPAlias());

            identityProviderResource.addMapper(attrMapperEmail);

            SPSSODescriptorType spDescriptor = getExportedSamlProvider();

            assertThat(spDescriptor.getAttributeConsumingService(), not(empty()));
            assertThat(spDescriptor.getAttributeConsumingService().get(0).getIndex(), is(12));
            assertThat(spDescriptor.getAttributeConsumingService().get(0).getRequestedAttribute(), notNullValue());
            assertThat(spDescriptor.getAttributeConsumingService().get(0).getRequestedAttribute(), not(empty()));
            assertThat(spDescriptor.getAttributeConsumingService().get(0).getRequestedAttribute().get(0).getName(), is("email_attr_name"));
            assertThat(spDescriptor.getAttributeConsumingService().get(0).getRequestedAttribute().get(0).getFriendlyName(), is("email_attr_friendlyname"));
            assertThat(spDescriptor.getAttributeConsumingService().get(0).getRequestedAttribute().get(0).getNameFormat(), is(JBossSAMLURIConstants.ATTRIBUTE_FORMAT_BASIC.getUri().toString()));
            assertThat(spDescriptor.getAttributeConsumingService().get(0).getServiceName(), notNullValue());
            assertThat(spDescriptor.getAttributeConsumingService().get(0).getServiceName().get(0).getValue(), is(bc.consumerRealmName()));
        }
    }

    @Test
    public void testAttributeConsumingServiceMappersInSpMetadataWithServiceName() throws IOException, ParsingException, URISyntaxException {
        try (Closeable idpUpdater = new IdentityProviderAttributeUpdater(identityProviderResource)
                .setAttribute(SAMLIdentityProviderConfig.ATTRIBUTE_CONSUMING_SERVICE_INDEX, "12").setAttribute(SAMLIdentityProviderConfig.ATTRIBUTE_CONSUMING_SERVICE_NAME, "My Attribute Set")
                .update())
        {
            IdentityProviderMapperRepresentation attrMapperEmail = new IdentityProviderMapperRepresentation();
            attrMapperEmail.setName("attribute-mapper-email");
            attrMapperEmail.setIdentityProviderMapper(UserAttributeMapper.PROVIDER_ID);
            attrMapperEmail.setConfig(ImmutableMap.<String,String>builder()
                    .put(IdentityProviderMapperModel.SYNC_MODE, IdentityProviderMapperSyncMode.INHERIT.toString())
                    .put(UserAttributeMapper.ATTRIBUTE_NAME, "email_attr_name")
                    .put(UserAttributeMapper.ATTRIBUTE_FRIENDLY_NAME, "email_attr_friendlyname")
                    .put(UserAttributeMapper.USER_ATTRIBUTE, "email")
                    .build());
            attrMapperEmail.setIdentityProviderAlias(bc.getIDPAlias());

            identityProviderResource.addMapper(attrMapperEmail);

            SPSSODescriptorType spDescriptor = getExportedSamlProvider();

            assertThat(spDescriptor.getAttributeConsumingService(), not(empty()));
            assertThat(spDescriptor.getAttributeConsumingService().get(0).getIndex(), is(12));
            assertThat(spDescriptor.getAttributeConsumingService().get(0).getRequestedAttribute(), notNullValue());
            assertThat(spDescriptor.getAttributeConsumingService().get(0).getRequestedAttribute(), not(empty()));
            assertThat(spDescriptor.getAttributeConsumingService().get(0).getRequestedAttribute().get(0).getName(), is("email_attr_name"));
            assertThat(spDescriptor.getAttributeConsumingService().get(0).getRequestedAttribute().get(0).getFriendlyName(), is("email_attr_friendlyname"));
            assertThat(spDescriptor.getAttributeConsumingService().get(0).getRequestedAttribute().get(0).getNameFormat(), is(JBossSAMLURIConstants.ATTRIBUTE_FORMAT_BASIC.getUri().toString()));
            assertThat(spDescriptor.getAttributeConsumingService().get(0).getServiceName(), notNullValue());
            assertThat(spDescriptor.getAttributeConsumingService().get(0).getServiceName().get(0).getValue(), is("My Attribute Set"));
        }
    }

    @Test
    public void testAttributeConsumingServiceNameInSpMetadataWithDifferentFormatName() throws IOException, ParsingException, URISyntaxException {
        try (Closeable idpUpdater = new IdentityProviderAttributeUpdater(identityProviderResource)
                .setAttribute(SAMLIdentityProviderConfig.ATTRIBUTE_CONSUMING_SERVICE_INDEX, "12")
                .update())
        {
            IdentityProviderMapperRepresentation attrMapperEmail = new IdentityProviderMapperRepresentation();
            attrMapperEmail.setName("attribute-mapper-email");
            attrMapperEmail.setIdentityProviderMapper(UserAttributeMapper.PROVIDER_ID);
            attrMapperEmail.setConfig(ImmutableMap.<String,String>builder()
                    .put(IdentityProviderMapperModel.SYNC_MODE, IdentityProviderMapperSyncMode.INHERIT.toString())
                    .put(UserAttributeMapper.ATTRIBUTE_NAME, "email_attr_name")
                    .put(UserAttributeMapper.ATTRIBUTE_FRIENDLY_NAME, "email_attr_friendlyname")
                    .put(UserAttributeMapper.USER_ATTRIBUTE, "email")
                    .put(UserAttributeMapper.ATTRIBUTE_NAME_FORMAT, JBossSAMLURIConstants.ATTRIBUTE_FORMAT_URI.name())
                    .build());
            attrMapperEmail.setIdentityProviderAlias(bc.getIDPAlias());

            identityProviderResource.addMapper(attrMapperEmail);

            SPSSODescriptorType spDescriptor = getExportedSamlProvider();

            assertThat(spDescriptor.getAttributeConsumingService(), not(empty()));
            assertThat(spDescriptor.getAttributeConsumingService().get(0).getIndex(), is(12));
            assertThat(spDescriptor.getAttributeConsumingService().get(0).getRequestedAttribute(), notNullValue());
            assertThat(spDescriptor.getAttributeConsumingService().get(0).getRequestedAttribute(), not(empty()));
            assertThat(spDescriptor.getAttributeConsumingService().get(0).getRequestedAttribute().get(0).getName(), is("email_attr_name"));
            assertThat(spDescriptor.getAttributeConsumingService().get(0).getRequestedAttribute().get(0).getFriendlyName(), is("email_attr_friendlyname"));
            assertThat(spDescriptor.getAttributeConsumingService().get(0).getRequestedAttribute().get(0).getNameFormat(), is(JBossSAMLURIConstants.ATTRIBUTE_FORMAT_URI.getUri().toString()));
            assertThat(spDescriptor.getAttributeConsumingService().get(0).getServiceName(), notNullValue());
            assertThat(spDescriptor.getAttributeConsumingService().get(0).getServiceName().get(0).getValue(), is(bc.consumerRealmName()));
        }
    }

    @Test
    public void testAttributeConsumingServiceAttributeRoleMapperInSpMetadataWithServiceName() throws IOException, ParsingException, URISyntaxException {
        try (Closeable idpUpdater = new IdentityProviderAttributeUpdater(identityProviderResource)
            .setAttribute(SAMLIdentityProviderConfig.ATTRIBUTE_CONSUMING_SERVICE_INDEX, "9").setAttribute(SAMLIdentityProviderConfig.ATTRIBUTE_CONSUMING_SERVICE_NAME, "My Attribute Set")
            .update())
        {
            IdentityProviderMapperRepresentation attrMapperRole = new IdentityProviderMapperRepresentation();
            attrMapperRole.setName("attribute-mapper-someroleattribute");
            attrMapperRole.setIdentityProviderMapper(AttributeToRoleMapper.PROVIDER_ID);
            attrMapperRole.setConfig(ImmutableMap.<String,String>builder()
              .put(IdentityProviderMapperModel.SYNC_MODE, IdentityProviderMapperSyncMode.INHERIT.toString())
              .put(AttributeToRoleMapper.ATTRIBUTE_NAME, "role_attr_name")
              .put(AttributeToRoleMapper.ATTRIBUTE_FRIENDLY_NAME, "role_attr_friendlyname")
              .put(ConfigConstants.ROLE, "somerole")
              .build());
            attrMapperRole.setIdentityProviderAlias(bc.getIDPAlias());

            identityProviderResource.addMapper(attrMapperRole);

            SPSSODescriptorType spDescriptor = getExportedSamlProvider();

            assertThat(spDescriptor.getAttributeConsumingService(), not(empty()));
            assertThat(spDescriptor.getAttributeConsumingService().get(0).getIndex(), is(9));
            assertThat(spDescriptor.getAttributeConsumingService().get(0).getRequestedAttribute(), notNullValue());
            assertThat(spDescriptor.getAttributeConsumingService().get(0).getRequestedAttribute(), not(empty()));
            assertThat(spDescriptor.getAttributeConsumingService().get(0).getRequestedAttribute().get(0).getName(), is("role_attr_name"));
            assertThat(spDescriptor.getAttributeConsumingService().get(0).getRequestedAttribute().get(0).getFriendlyName(), is("role_attr_friendlyname"));
            assertThat(spDescriptor.getAttributeConsumingService().get(0).getServiceName(), notNullValue());
            assertThat(spDescriptor.getAttributeConsumingService().get(0).getServiceName().get(0).getValue(), is("My Attribute Set"));
        }
    }

    @Test
    public void testKeysDescriptors() throws IOException, ParsingException, URISyntaxException {
        // No keys by default
        SPSSODescriptorType spDescriptor = getExportedSamlProvider();
        Assert.assertNotNull("KeyDescriptor is null", spDescriptor.getKeyDescriptor());
        Assert.assertEquals("KeyDescriptor.size", 0, spDescriptor.getKeyDescriptor().size());

        // Enable signing for IDP. Only signing key is present
        try (Closeable idpUpdater = new IdentityProviderAttributeUpdater(identityProviderResource)
                .setAttribute(SAMLIdentityProviderConfig.WANT_AUTHN_REQUESTS_SIGNED, "true")
                .update())
        {
            spDescriptor = getExportedSamlProvider();
            Assert.assertEquals("KeyDescriptor.size", 1, spDescriptor.getKeyDescriptor().size());
            Map<String, String> certs = convertCerts(spDescriptor);
            Assert.assertEquals(1, certs.size());
            Assert.assertNotNull(certs.get("signing"));
        }

        // Enable signing and encryption. Both keys are present and mapped to same realm key
        try (Closeable idpUpdater = new IdentityProviderAttributeUpdater(identityProviderResource)
                .setAttribute(SAMLIdentityProviderConfig.WANT_AUTHN_REQUESTS_SIGNED, "true")
                .setAttribute(SAMLIdentityProviderConfig.WANT_ASSERTIONS_ENCRYPTED, "true")
                .update())
        {
            spDescriptor = getExportedSamlProvider();
            Assert.assertEquals("KeyDescriptor.size", 2, spDescriptor.getKeyDescriptor().size());
            Map<String, String> certs = convertCerts(spDescriptor);
            Assert.assertEquals(2, certs.size());
            String signingCert = certs.get("signing");
            String encCert = certs.get("encryption");
            Assert.assertNotNull(signingCert);
            Assert.assertNotNull(encCert);
            Assert.assertNotEquals(signingCert, encCert);
            hasEncAlgorithms(spDescriptor, SAMLEncryptionAlgorithms.RSA_OAEP.getXmlEncIdentifiers());
        }

        // Enable signing and encryption and set encryption algorithm. Both keys are present and mapped to different realm key (signing to "rsa-generated"m encryption to "rsa-enc-generated")
        try (Closeable idpUpdater = new IdentityProviderAttributeUpdater(identityProviderResource)
                .setAttribute(SAMLIdentityProviderConfig.WANT_AUTHN_REQUESTS_SIGNED, "true")
                .setAttribute(SAMLIdentityProviderConfig.WANT_ASSERTIONS_ENCRYPTED, "true")
                .setAttribute(SAMLIdentityProviderConfig.ENCRYPTION_ALGORITHM, Algorithm.RSA_OAEP)
                .update())
        {
            spDescriptor = getExportedSamlProvider();
            Assert.assertEquals("KeyDescriptor.size", 2, spDescriptor.getKeyDescriptor().size());
            Map<String, String> certs = convertCerts(spDescriptor);
            Assert.assertEquals(2, certs.size());
            String signingCert = certs.get("signing");
            String encCert = certs.get("encryption");
            Assert.assertNotNull(signingCert);
            Assert.assertNotNull(encCert);
            Assert.assertNotEquals(signingCert, encCert);
            hasEncAlgorithms(spDescriptor, SAMLEncryptionAlgorithms.RSA_OAEP.getXmlEncIdentifiers());
        }
    }

    @Test
    public void testEncKeyDescriptors() throws Exception {
        SPSSODescriptorType spDescriptor;

        try (AutoCloseable ac1 = generateNewRealmKey(adminClient.realm(bc.consumerRealmName()), KeyUse.ENC, Algorithm.RSA1_5);
             AutoCloseable ac2 = generateNewRealmKey(adminClient.realm(bc.consumerRealmName()), KeyUse.ENC, Algorithm.RSA_OAEP_256)) {

            // Test all enc keys are present in metadata
            try (Closeable idpUpdater = new IdentityProviderAttributeUpdater(identityProviderResource)
                    .setAttribute(SAMLIdentityProviderConfig.WANT_ASSERTIONS_ENCRYPTED, "true")
                    .update()) {
                spDescriptor = getExportedSamlProvider();
                hasEncAlgorithms(spDescriptor,
                        Stream.concat(Arrays.stream(SAMLEncryptionAlgorithms.RSA1_5.getXmlEncIdentifiers()),
                                Arrays.stream(SAMLEncryptionAlgorithms.RSA_OAEP.getXmlEncIdentifiers())).toArray(String[]::new)
                );
            }

            // Specify algorithms for IDP
            try (Closeable idpUpdater = new IdentityProviderAttributeUpdater(identityProviderResource)
                    .setAttribute(SAMLIdentityProviderConfig.WANT_ASSERTIONS_ENCRYPTED, "true")
                    .setAttribute(SAMLIdentityProviderConfig.ENCRYPTION_ALGORITHM, Algorithm.RSA_OAEP)
                    .update()) {
                spDescriptor = getExportedSamlProvider();
                hasEncAlgorithms(spDescriptor,
                        SAMLEncryptionAlgorithms.RSA_OAEP.getXmlEncIdentifiers()
                );
            }

            try (Closeable idpUpdater = new IdentityProviderAttributeUpdater(identityProviderResource)
                    .setAttribute(SAMLIdentityProviderConfig.WANT_ASSERTIONS_ENCRYPTED, "true")
                    .setAttribute(SAMLIdentityProviderConfig.ENCRYPTION_ALGORITHM, Algorithm.RSA1_5)
                    .update()) {
                spDescriptor = getExportedSamlProvider();
                hasEncAlgorithms(spDescriptor,
                        SAMLEncryptionAlgorithms.RSA1_5.getXmlEncIdentifiers()
                );
            }
        }
    }

    private SPSSODescriptorType getExportedSamlProvider() throws ParsingException {
        String spDescriptorString = identityProviderResource.export(null).readEntity(String.class);
        SAMLParser parser = SAMLParser.getInstance();
        EntityDescriptorType o = (EntityDescriptorType) parser.parse(new StringInputStream(spDescriptorString));
        return o.getChoiceType().get(0).getDescriptors().get(0).getSpDescriptor();
    }

    private void hasEncAlgorithms(SPSSODescriptorType spDescriptor, String... expectedAlgorithms) {
        List<String> algorithms = spDescriptor.getKeyDescriptor().stream()
                .filter(key -> key.getUse() == KeyTypes.ENCRYPTION)
                .map(KeyDescriptorType::getEncryptionMethod)
                .flatMap(list -> list.stream().map(EncryptionMethodType::getAlgorithm))
                .collect(Collectors.toList());

        assertThat(algorithms, containsInAnyOrder(expectedAlgorithms));
    }

    // Key is usage ("signing" or "encryption"), Value is string with X509 certificate
    private Map<String, String> convertCerts(SPSSODescriptorType spDescriptor) {
        return spDescriptor.getKeyDescriptor().stream()
                .collect(Collectors.toMap(
                        keyDescriptor -> keyDescriptor.getUse().value(),
                        keyDescriptor -> keyDescriptor.getKeyInfo().getElementsByTagNameNS(XMLSignature.XMLNS, "X509Certificate").item(0).getTextContent()));
    }



    //KEYCLOAK-18909
    @Test
    public void testKeysExistenceInSpMetadata() throws IOException, ParsingException, URISyntaxException {
        try (Closeable idpUpdater = new IdentityProviderAttributeUpdater(identityProviderResource)
                .setAttribute(SAMLIdentityProviderConfig.WANT_AUTHN_REQUESTS_SIGNED, "true")
                .setAttribute(SAMLIdentityProviderConfig.WANT_ASSERTIONS_SIGNED, "true")
                .setAttribute(SAMLIdentityProviderConfig.WANT_ASSERTIONS_ENCRYPTED, "true")
                .update())
        {

            String spDescriptorString = identityProviderResource.export(null).readEntity(String.class);
            SAMLParser parser = SAMLParser.getInstance();
            EntityDescriptorType o = (EntityDescriptorType) parser.parse(new StringInputStream(spDescriptorString));
            SPSSODescriptorType spDescriptor = o.getChoiceType().get(0).getDescriptors().get(0).getSpDescriptor();

            //the SPSSODescriptor should have at least one KeyDescriptor for encryption and one for Signing
            List<KeyDescriptorType> encKeyDescr = spDescriptor.getKeyDescriptor().stream().filter(k -> KeyTypes.ENCRYPTION.equals(k.getUse())).collect(Collectors.toList());
            List<KeyDescriptorType> sigKeyDescr = spDescriptor.getKeyDescriptor().stream().filter(k -> KeyTypes.SIGNING.equals(k.getUse())).collect(Collectors.toList());

            assertTrue(encKeyDescr.size() > 0);
            assertTrue(sigKeyDescr.size() > 0);

            //also, the keys should match the realm's dedicated keys for enc and sig

            Set<String> encKeyDescNames = encKeyDescr.stream()
                    .map(k-> k.getKeyInfo().getElementsByTagName("ds:KeyName").item(0).getTextContent().trim())
                    .collect(Collectors.toCollection(HashSet::new));

            Set<String> sigKeyDescNames = sigKeyDescr.stream()
                    .map(k-> k.getKeyInfo().getElementsByTagName("ds:KeyName").item(0).getTextContent().trim())
                    .collect(Collectors.toCollection(HashSet::new));

            KeysMetadataRepresentation realmKeysMetadata = adminClient.realm(getBrokerConfiguration().consumerRealmName()).keys().getKeyMetadata();

            long encMatches = realmKeysMetadata.getKeys().stream()
                    .filter(k -> KeyStatus.valueOf(k.getStatus()).isActive())
                    //.filter(k -> "RSA".equals(k.getType().trim()))
                    .filter(k -> KeyUse.ENC.equals(k.getUse()))
                    .filter(k -> encKeyDescNames.contains(k.getKid().trim()))
                    .count();

            long sigMatches = realmKeysMetadata.getKeys().stream()
                    .filter(k -> KeyStatus.valueOf(k.getStatus()).isActive())
                    //.filter(k -> "RSA".equals(k.getType().trim()))
                    .filter(k -> KeyUse.SIG.equals(k.getUse()))
                    .filter(k -> sigKeyDescNames.contains(k.getKid().trim()))
                    .count();

            assertTrue(encMatches > 0);
            assertTrue(sigMatches > 0);
        }
    }

    @Test
    public void testMetadataBindingEqualsKeycloakPOSTBindingSettingsOn()
            throws IOException, ParsingException, URISyntaxException {
    try (Closeable idpUpdater = new IdentityProviderAttributeUpdater(identityProviderResource)
            .setAttribute(SAMLIdentityProviderConfig.POST_BINDING_AUTHN_REQUEST, "true")
            .setAttribute(SAMLIdentityProviderConfig.POST_BINDING_LOGOUT, "true")
            //To ensure that backward compatibility is maintained, the value is intentionally reversed from isPostBindingAuthnRequest.
            .setAttribute(SAMLIdentityProviderConfig.POST_BINDING_RESPONSE, "false")
            .update())
        {

            String spDescriptorString = identityProviderResource.export(null).readEntity(String.class);
            SAMLParser parser = SAMLParser.getInstance();
            EntityDescriptorType o = (EntityDescriptorType) parser.parse(new StringInputStream(spDescriptorString));
            SPSSODescriptorType spDescriptor = o.getChoiceType().get(0).getDescriptors().get(0).getSpDescriptor();

            assertThat(spDescriptor.getSingleLogoutService().size(), is(2));
            assertThat(spDescriptor.getSingleLogoutService().get(0).getBinding().toString(),
                    is(JBossSAMLURIConstants.SAML_HTTP_POST_BINDING.get()));
            assertThat(spDescriptor.getSingleLogoutService().get(1).getBinding().toString(),
                    is(JBossSAMLURIConstants.SAML_HTTP_REDIRECT_BINDING.get()));

            assertThat(spDescriptor.getAssertionConsumerService().size(), is(3));
            assertThat(spDescriptor.getAssertionConsumerService().get(0).getBinding().toString(),
                    is(JBossSAMLURIConstants.SAML_HTTP_POST_BINDING.get()));
            assertThat(spDescriptor.getAssertionConsumerService().get(1).getBinding().toString(),
                    is(JBossSAMLURIConstants.SAML_HTTP_REDIRECT_BINDING.get()));
            assertThat(spDescriptor.getAssertionConsumerService().get(2).getBinding().toString(),
                    is(JBossSAMLURIConstants.SAML_HTTP_ARTIFACT_BINDING.get()));
        }
    }

    @Test
    public void testMetadataBindingEqualsKeycloakPOSTBindingSettingsOff()
            throws IOException, ParsingException, URISyntaxException {
        try (Closeable idpUpdater = new IdentityProviderAttributeUpdater(identityProviderResource)
                .setAttribute(SAMLIdentityProviderConfig.POST_BINDING_AUTHN_REQUEST, "false")
                .setAttribute(SAMLIdentityProviderConfig.POST_BINDING_LOGOUT, "false")
                //To ensure that backward compatibility is maintained, the value is intentionally reversed from isPostBindingAuthnRequest.
                .setAttribute(SAMLIdentityProviderConfig.POST_BINDING_RESPONSE, "true")
                .update()) {

            String spDescriptorString = identityProviderResource.export(null).readEntity(String.class);
            SAMLParser parser = SAMLParser.getInstance();
            EntityDescriptorType o = (EntityDescriptorType) parser.parse(new StringInputStream(spDescriptorString));
            SPSSODescriptorType spDescriptor = o.getChoiceType().get(0).getDescriptors().get(0).getSpDescriptor();

            assertThat(spDescriptor.getSingleLogoutService().size(), is(2));
            assertThat(spDescriptor.getSingleLogoutService().get(0).getBinding().toString(),
                    is(JBossSAMLURIConstants.SAML_HTTP_REDIRECT_BINDING.get()));
            assertThat(spDescriptor.getSingleLogoutService().get(1).getBinding().toString(),
                    is(JBossSAMLURIConstants.SAML_HTTP_POST_BINDING.get()));

            assertThat(spDescriptor.getAssertionConsumerService().size(), is(3));
            assertThat(spDescriptor.getAssertionConsumerService().get(0).getBinding().toString(),
                    is(JBossSAMLURIConstants.SAML_HTTP_REDIRECT_BINDING.get()));
            assertThat(spDescriptor.getAssertionConsumerService().get(1).getBinding().toString(),
                    is(JBossSAMLURIConstants.SAML_HTTP_POST_BINDING.get()));
            assertThat(spDescriptor.getAssertionConsumerService().get(2).getBinding().toString(),
                    is(JBossSAMLURIConstants.SAML_HTTP_ARTIFACT_BINDING.get()));
        }
    }

    @Test
    public void testMetadataBindingEqualsKeycloakSLOBindingSettingsIsDefault()
            throws IOException, ParsingException, URISyntaxException {
        try (Closeable idpUpdater = new IdentityProviderAttributeUpdater(identityProviderResource).update()){

            String spDescriptorString = identityProviderResource.export(null).readEntity(String.class);
            SAMLParser parser = SAMLParser.getInstance();
            EntityDescriptorType o = (EntityDescriptorType) parser.parse(new StringInputStream(spDescriptorString));
            SPSSODescriptorType spDescriptor = o.getChoiceType().get(0).getDescriptors().get(0).getSpDescriptor();

            assertThat(spDescriptor.getSingleLogoutService().size(), is(2));
            assertThat(spDescriptor.getAssertionConsumerService().size(), is(3));
            assertThat(spDescriptor.getSingleLogoutService().get(0).getBinding().toString(),
                    is(spDescriptor.getAssertionConsumerService().get(0).getBinding().toString()));

        }
    }
}
