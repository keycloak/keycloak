package org.keycloak.testsuite.broker;

import com.google.common.collect.ImmutableMap;

import org.apache.tools.ant.filters.StringInputStream;
import org.junit.Test;
import org.keycloak.broker.provider.ConfigConstants;
import org.keycloak.broker.saml.SAMLIdentityProviderConfig;
import org.keycloak.broker.saml.mappers.AttributeToRoleMapper;
import org.keycloak.broker.saml.mappers.UserAttributeMapper;
import org.keycloak.dom.saml.v2.metadata.EntityDescriptorType;
import org.keycloak.dom.saml.v2.metadata.SPSSODescriptorType;
import org.keycloak.jose.jwe.JWEConstants;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderMapperSyncMode;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.processing.core.parsers.saml.SAMLParser;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.updaters.IdentityProviderAttributeUpdater;

import java.io.Closeable;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.crypto.dsig.XMLSignature;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

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
            Assert.assertEquals(signingCert, encCert);
        }

        // Enable signing and encryption and set encryption algorithm. Both keys are present and mapped to different realm key (signing to "rsa-generated"m encryption to "rsa-enc-generated")
        try (Closeable idpUpdater = new IdentityProviderAttributeUpdater(identityProviderResource)
                .setAttribute(SAMLIdentityProviderConfig.WANT_AUTHN_REQUESTS_SIGNED, "true")
                .setAttribute(SAMLIdentityProviderConfig.WANT_ASSERTIONS_ENCRYPTED, "true")
                .setAttribute(SAMLIdentityProviderConfig.ENCRYPTION_ALGORITHM, JWEConstants.RSA_OAEP)
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
        }
    }

    private SPSSODescriptorType getExportedSamlProvider() throws ParsingException {
        String spDescriptorString = identityProviderResource.export(null).readEntity(String.class);
        SAMLParser parser = SAMLParser.getInstance();
        EntityDescriptorType o = (EntityDescriptorType) parser.parse(new StringInputStream(spDescriptorString));
        return o.getChoiceType().get(0).getDescriptors().get(0).getSpDescriptor();
    }

    // Key is usage ("signing" or "encryption"), Value is string with X509 certificate
    private Map<String, String> convertCerts(SPSSODescriptorType spDescriptor) {
        return spDescriptor.getKeyDescriptor().stream()
                .collect(Collectors.toMap(
                        keyDescriptor -> keyDescriptor.getUse().value(),
                        keyDescriptor -> keyDescriptor.getKeyInfo().getElementsByTagNameNS(XMLSignature.XMLNS, "X509Certificate").item(0).getTextContent()));
    }

}
