package org.keycloak.testsuite.broker;

import com.google.common.collect.ImmutableMap;

import org.apache.tools.ant.filters.StringInputStream;
import org.junit.Test;
import org.keycloak.broker.provider.ConfigConstants;
import org.keycloak.broker.saml.SAMLIdentityProviderConfig;
import org.keycloak.broker.saml.mappers.AttributeToRoleMapper;
import org.keycloak.broker.saml.mappers.UserAttributeMapper;
import org.keycloak.crypto.KeyStatus;
import org.keycloak.crypto.KeyUse;
import org.keycloak.dom.saml.v2.metadata.EntityDescriptorType;
import org.keycloak.dom.saml.v2.metadata.KeyDescriptorType;
import org.keycloak.dom.saml.v2.metadata.KeyTypes;
import org.keycloak.dom.saml.v2.metadata.SPSSODescriptorType;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderMapperSyncMode;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.representations.idm.KeysMetadataRepresentation;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.processing.core.parsers.saml.SAMLParser;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.updaters.IdentityProviderAttributeUpdater;

import java.io.Closeable;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.empty;
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

            String spDescriptorString = identityProviderResource.export(null).readEntity(String.class);
            SAMLParser parser = SAMLParser.getInstance();
            EntityDescriptorType o = (EntityDescriptorType) parser.parse(new StringInputStream(spDescriptorString));
            SPSSODescriptorType spDescriptor = o.getChoiceType().get(0).getDescriptors().get(0).getSpDescriptor();

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

            String spDescriptorString = identityProviderResource.export(null).readEntity(String.class);
            SAMLParser parser = SAMLParser.getInstance();
            EntityDescriptorType o = (EntityDescriptorType) parser.parse(new StringInputStream(spDescriptorString));
            SPSSODescriptorType spDescriptor = o.getChoiceType().get(0).getDescriptors().get(0).getSpDescriptor();

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

            String spDescriptorString = identityProviderResource.export(null).readEntity(String.class);
            SAMLParser parser = SAMLParser.getInstance();
            EntityDescriptorType o = (EntityDescriptorType) parser.parse(new StringInputStream(spDescriptorString));
            SPSSODescriptorType spDescriptor = o.getChoiceType().get(0).getDescriptors().get(0).getSpDescriptor();

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

            String spDescriptorString = identityProviderResource.export(null).readEntity(String.class);
            SAMLParser parser = SAMLParser.getInstance();
            EntityDescriptorType o = (EntityDescriptorType) parser.parse(new StringInputStream(spDescriptorString));
            SPSSODescriptorType spDescriptor = o.getChoiceType().get(0).getDescriptors().get(0).getSpDescriptor();

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

            String spDescriptorString = identityProviderResource.export(null).readEntity(String.class);
            SAMLParser parser = SAMLParser.getInstance();
            EntityDescriptorType o = (EntityDescriptorType) parser.parse(new StringInputStream(spDescriptorString));
            SPSSODescriptorType spDescriptor = o.getChoiceType().get(0).getDescriptors().get(0).getSpDescriptor();

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

}
