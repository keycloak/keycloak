package org.keycloak.protocol.saml;

import org.keycloak.Config;
import org.keycloak.dom.saml.v2.metadata.*;
import org.keycloak.exportimport.ClientDescriptionConverter;
import org.keycloak.exportimport.ClientDescriptionConverterFactory;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.saml.SignatureAlgorithm;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.processing.core.parsers.saml.SAMLParser;
import org.keycloak.saml.processing.core.saml.v2.util.SAMLMetadataUtil;
import org.keycloak.saml.processing.core.util.CoreConfigUtil;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class EntityDescriptorDescriptionConverter implements ClientDescriptionConverter, ClientDescriptionConverterFactory {

    @Override
    public boolean isSupported(String description) {
        description = description.trim();
        return (description.startsWith("<") && description.endsWith(">") && description.contains("EntityDescriptor"));
    }

    @Override
    public ClientRepresentation convertToInternal(String description) {
        return loadEntityDescriptors(new ByteArrayInputStream(description.getBytes()));
    }

    private static ClientRepresentation loadEntityDescriptors(InputStream is) {
        Object metadata;
        try {
            metadata = new SAMLParser().parse(is);
        } catch (ParsingException e) {
            throw new RuntimeException(e);
        }
        EntitiesDescriptorType entities;

        if (EntitiesDescriptorType.class.isInstance(metadata)) {
            entities = (EntitiesDescriptorType) metadata;
        } else {
            entities = new EntitiesDescriptorType();
            entities.addEntityDescriptor(metadata);
        }

        if (entities.getEntityDescriptor().size() != 1) {
            throw new RuntimeException("Expected one entity descriptor");
        }

        EntityDescriptorType entity = (EntityDescriptorType) entities.getEntityDescriptor().get(0);
        String entityId = entity.getEntityID();

        ClientRepresentation app = new ClientRepresentation();
        app.setClientId(entityId);

        Map<String, String> attributes = new HashMap<>();
        app.setAttributes(attributes);

        List<String> redirectUris = new LinkedList<>();
        app.setRedirectUris(redirectUris);

        app.setFullScopeAllowed(true);
        app.setProtocol(SamlProtocol.LOGIN_PROTOCOL);
        attributes.put(SamlProtocol.SAML_SERVER_SIGNATURE, SamlProtocol.ATTRIBUTE_TRUE_VALUE); // default to true
        attributes.put(SamlProtocol.SAML_SIGNATURE_ALGORITHM, SignatureAlgorithm.RSA_SHA256.toString());
        attributes.put(SamlProtocol.SAML_AUTHNSTATEMENT, SamlProtocol.ATTRIBUTE_TRUE_VALUE);
        SPSSODescriptorType spDescriptorType = CoreConfigUtil.getSPDescriptor(entity);
        if (spDescriptorType.isWantAssertionsSigned()) {
            attributes.put(SamlProtocol.SAML_ASSERTION_SIGNATURE, SamlProtocol.ATTRIBUTE_TRUE_VALUE);
        }
        String logoutPost = getLogoutLocation(spDescriptorType, JBossSAMLURIConstants.SAML_HTTP_POST_BINDING.get());
        if (logoutPost != null) attributes.put(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_POST_ATTRIBUTE, logoutPost);
        String logoutRedirect = getLogoutLocation(spDescriptorType, JBossSAMLURIConstants.SAML_HTTP_REDIRECT_BINDING.get());
        if (logoutPost != null) attributes.put(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_REDIRECT_ATTRIBUTE, logoutRedirect);

        String assertionConsumerServicePostBinding = CoreConfigUtil.getServiceURL(spDescriptorType, JBossSAMLURIConstants.SAML_HTTP_POST_BINDING.get());
        if (assertionConsumerServicePostBinding != null) {
            attributes.put(SamlProtocol.SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE, assertionConsumerServicePostBinding);
            redirectUris.add(assertionConsumerServicePostBinding);
        }
        String assertionConsumerServiceRedirectBinding = CoreConfigUtil.getServiceURL(spDescriptorType, JBossSAMLURIConstants.SAML_HTTP_REDIRECT_BINDING.get());
        if (assertionConsumerServiceRedirectBinding != null) {
            attributes.put(SamlProtocol.SAML_ASSERTION_CONSUMER_URL_REDIRECT_ATTRIBUTE, assertionConsumerServiceRedirectBinding);
            redirectUris.add(assertionConsumerServiceRedirectBinding);
        }

        for (KeyDescriptorType keyDescriptor : spDescriptorType.getKeyDescriptor()) {
            X509Certificate cert = null;
            try {
                cert = SAMLMetadataUtil.getCertificate(keyDescriptor);
            } catch (ConfigurationException e) {
                throw new RuntimeException(e);
            } catch (ProcessingException e) {
                throw new RuntimeException(e);
            }
            String certPem = KeycloakModelUtils.getPemFromCertificate(cert);
            if (keyDescriptor.getUse() == KeyTypes.SIGNING) {
                attributes.put(SamlProtocol.SAML_CLIENT_SIGNATURE_ATTRIBUTE, SamlProtocol.ATTRIBUTE_TRUE_VALUE);
                attributes.put(SamlProtocol.SAML_SIGNING_CERTIFICATE_ATTRIBUTE, certPem);
            } else if (keyDescriptor.getUse() == KeyTypes.ENCRYPTION) {
                attributes.put(SamlProtocol.SAML_ENCRYPT, SamlProtocol.ATTRIBUTE_TRUE_VALUE);
                attributes.put(SamlProtocol.SAML_ENCRYPTION_CERTIFICATE_ATTRIBUTE, certPem);
            }
        }

        return app;
    }

    private static String getLogoutLocation(SPSSODescriptorType idp, String bindingURI) {
        String logoutResponseLocation = null;

        List<EndpointType> endpoints = idp.getSingleLogoutService();
        for (EndpointType endpoint : endpoints) {
            if (endpoint.getBinding().toString().equals(bindingURI)) {
                if (endpoint.getLocation() != null) {
                    logoutResponseLocation = endpoint.getLocation().toString();
                } else {
                    logoutResponseLocation = null;
                }

                break;
            }

        }
        return logoutResponseLocation;
    }

    @Override
    public ClientDescriptionConverter create(KeycloakSession session) {
        return this;
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return "saml2-entity-descriptor";
    }

}
