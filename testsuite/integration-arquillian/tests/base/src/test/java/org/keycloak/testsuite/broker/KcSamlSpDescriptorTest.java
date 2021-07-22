package org.keycloak.testsuite.broker;

import com.google.common.collect.ImmutableMap;

import org.apache.tools.ant.filters.StringInputStream;
import org.junit.Test;
import org.keycloak.broker.saml.SAMLIdentityProviderConfig;
import org.keycloak.broker.saml.mappers.UserAttributeMapper;
import org.keycloak.dom.saml.v2.metadata.EntityDescriptorType;
import org.keycloak.dom.saml.v2.metadata.SPSSODescriptorType;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderMapperSyncMode;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.processing.core.parsers.saml.SAMLParser;
import org.keycloak.testsuite.updaters.IdentityProviderAttributeUpdater;

import java.io.Closeable;
import java.io.IOException;
import java.net.URISyntaxException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class KcSamlSpDescriptorTest extends AbstractBrokerTest {

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return KcSamlBrokerConfiguration.INSTANCE;
    }

    @Test
    public void testAttributeConsumingServiceIndexInSpMetadata() throws IOException, ParsingException, URISyntaxException {
        try (Closeable idpUpdater = new IdentityProviderAttributeUpdater(identityProviderResource)
            .setAttribute(SAMLIdentityProviderConfig.ATTRIBUTE_CONSUMING_SERVICE_INDEX, "15")
            .update())
        {

            String spDescriptorString = identityProviderResource.export(null).readEntity(String.class);
            SAMLParser parser = SAMLParser.getInstance();
            EntityDescriptorType o = (EntityDescriptorType) parser.parse(new StringInputStream(spDescriptorString));
            SPSSODescriptorType spDescriptor = o.getChoiceType().get(0).getDescriptors().get(0).getSpDescriptor();

            assertThat(spDescriptor.getAttributeConsumingService().isEmpty(), is(false));
            assertThat(spDescriptor.getAttributeConsumingService().get(0).getIndex(), is(15));
        }
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

            assertThat(spDescriptor.getAttributeConsumingService().isEmpty(), is(false));
            assertThat(spDescriptor.getAttributeConsumingService().get(0).getServiceName().get(0).getValue(), is("My Attribute Set"));
        }
    }

    @Test
    public void testAttributeConsumingServiceMappersInSpMetadata() throws IOException, ParsingException, URISyntaxException {
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

            assertThat(spDescriptor.getAttributeConsumingService().isEmpty(), is(false));
            assertThat(spDescriptor.getAttributeConsumingService().get(0).getIndex(), is(12));
            assertThat(spDescriptor.getAttributeConsumingService().get(0).getRequestedAttribute() != null, is(true));
            assertThat(spDescriptor.getAttributeConsumingService().get(0).getRequestedAttribute().isEmpty(), is(false));
            assertThat(spDescriptor.getAttributeConsumingService().get(0).getRequestedAttribute().get(0).getName(), is("email_attr_name"));
            assertThat(spDescriptor.getAttributeConsumingService().get(0).getRequestedAttribute().get(0).getFriendlyName(), is("email_attr_friendlyname"));
        }
    }

}
