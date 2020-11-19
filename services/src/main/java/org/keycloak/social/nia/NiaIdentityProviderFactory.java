package org.keycloak.social.nia;

import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;
import org.keycloak.Config;
import org.keycloak.broker.provider.AbstractIdentityProviderFactory;
import org.keycloak.broker.social.SocialIdentityProviderFactory;
import org.keycloak.dom.saml.v2.assertion.AttributeType;
import org.keycloak.dom.saml.v2.metadata.EndpointType;
import org.keycloak.dom.saml.v2.metadata.EntitiesDescriptorType;
import org.keycloak.dom.saml.v2.metadata.EntityDescriptorType;
import org.keycloak.dom.saml.v2.metadata.IDPSSODescriptorType;
import org.keycloak.dom.saml.v2.metadata.KeyDescriptorType;
import org.keycloak.dom.saml.v2.metadata.KeyTypes;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.util.DocumentUtil;
import org.keycloak.saml.processing.core.parsers.saml.SAMLParser;
import org.keycloak.saml.validators.DestinationValidator;
import org.w3c.dom.Element;

public class NiaIdentityProviderFactory extends AbstractIdentityProviderFactory<NiaIdentityProvider>
        implements SocialIdentityProviderFactory<NiaIdentityProvider> {

    public static final String NIA_PROVIDER_ID = "nia";
    public static final String NIA_PROVIDER_NAME = "NIA";
    public static final String[] COMPATIBLE_PROVIDER = new String[]{NIA_PROVIDER_ID};

    private DestinationValidator destinationValidator;

    private static final String MACEDIR_ENTITY_CATEGORY = "http://macedir.org/entity-category";
    private static final String REFEDS_HIDE_FROM_DISCOVERY = "http://refeds.org/category/hide-from-discovery";

    @Override
    public String getName() {
        return NIA_PROVIDER_NAME;
    }

    @Override
    public String getId() {
        return NIA_PROVIDER_ID;
    }

    @Override
    public NiaIdentityProvider create(KeycloakSession session, IdentityProviderModel model) {
        return new NiaIdentityProvider(session, new NiaIdentityProviderConfig(model),
                destinationValidator);
    }

    @Override
    public NiaIdentityProviderConfig createConfig() {
        return new NiaIdentityProviderConfig();

    }

    @Override
    public void init(Config.Scope config) {
        super.init(config);
        this.destinationValidator = DestinationValidator.forProtocolMap(config.getArray("knownProtocols"));
    }

    @Override
    public Map<String, String> parseConfig(KeycloakSession session, InputStream inputStream) {
        try {
            Object parsedObject = SAMLParser.getInstance().parse(inputStream);
            EntityDescriptorType entityType;

            if (EntitiesDescriptorType.class.isInstance(parsedObject)) {
                entityType = (EntityDescriptorType) ((EntitiesDescriptorType) parsedObject).getEntityDescriptor().get(0);
            } else {
                entityType = (EntityDescriptorType) parsedObject;
            }

            List<EntityDescriptorType.EDTChoiceType> choiceType = entityType.getChoiceType();

            if (!choiceType.isEmpty()) {
                IDPSSODescriptorType idpDescriptor = null;

                //Metadata documents can contain multiple Descriptors (See ADFS metadata documents) such as RoleDescriptor, SPSSODescriptor, IDPSSODescriptor.
                //So we need to loop through to find the IDPSSODescriptor.
                for (EntityDescriptorType.EDTChoiceType edtChoiceType : entityType.getChoiceType()) {
                    List<EntityDescriptorType.EDTDescriptorChoiceType> descriptors = edtChoiceType.getDescriptors();

                    if (!descriptors.isEmpty() && descriptors.get(0).getIdpDescriptor() != null) {
                        idpDescriptor = descriptors.get(0).getIdpDescriptor();
                    }
                }

                if (idpDescriptor != null) {
                    NiaIdentityProviderConfig niaIdentityProviderConfig = new NiaIdentityProviderConfig();
                    String singleSignOnServiceUrl = null;
                    boolean postBindingResponse = false;
                    boolean postBindingLogout = false;
                    for (EndpointType endpoint : idpDescriptor.getSingleSignOnService()) {
                        if (endpoint.getBinding().toString().equals(JBossSAMLURIConstants.SAML_HTTP_POST_BINDING.get())) {
                            singleSignOnServiceUrl = endpoint.getLocation().toString();
                            postBindingResponse = true;
                            break;
                        } else if (endpoint.getBinding().toString().equals(JBossSAMLURIConstants.SAML_HTTP_REDIRECT_BINDING.get())) {
                            singleSignOnServiceUrl = endpoint.getLocation().toString();
                        }
                    }
                    String singleLogoutServiceUrl = null;
                    for (EndpointType endpoint : idpDescriptor.getSingleLogoutService()) {
                        if (postBindingResponse && endpoint.getBinding().toString().equals(JBossSAMLURIConstants.SAML_HTTP_POST_BINDING.get())) {
                            singleLogoutServiceUrl = endpoint.getLocation().toString();
                            postBindingLogout = true;
                            break;
                        } else if (!postBindingResponse && endpoint.getBinding().toString().equals(JBossSAMLURIConstants.SAML_HTTP_REDIRECT_BINDING.get())) {
                            singleLogoutServiceUrl = endpoint.getLocation().toString();
                            break;
                        }

                    }
                    niaIdentityProviderConfig.setSingleLogoutServiceUrl(singleLogoutServiceUrl);
                    niaIdentityProviderConfig.setSingleSignOnServiceUrl(singleSignOnServiceUrl);
                    niaIdentityProviderConfig.setWantAuthnRequestsSigned(idpDescriptor.isWantAuthnRequestsSigned());
                    niaIdentityProviderConfig.setAddExtensionsElementWithKeyInfo(false);
                    niaIdentityProviderConfig.setValidateSignature(idpDescriptor.isWantAuthnRequestsSigned());
                    niaIdentityProviderConfig.setPostBindingResponse(postBindingResponse);
                    niaIdentityProviderConfig.setPostBindingAuthnRequest(postBindingResponse);
                    niaIdentityProviderConfig.setPostBindingLogout(postBindingLogout);
                    niaIdentityProviderConfig.setLoginHint(false);

                    List<String> nameIdFormatList = idpDescriptor.getNameIDFormat();
                    if (nameIdFormatList != null && !nameIdFormatList.isEmpty()) {
                        niaIdentityProviderConfig.setNameIDPolicyFormat(nameIdFormatList.get(0));
                    }

                    List<KeyDescriptorType> keyDescriptor = idpDescriptor.getKeyDescriptor();
                    String defaultCertificate = null;

                    if (keyDescriptor != null) {
                        for (KeyDescriptorType keyDescriptorType : keyDescriptor) {
                            Element keyInfo = keyDescriptorType.getKeyInfo();
                            Element x509KeyInfo = DocumentUtil.getChildElement(keyInfo, new QName("dsig", "X509Certificate"));

                            if (KeyTypes.SIGNING.equals(keyDescriptorType.getUse())) {
                                niaIdentityProviderConfig.addSigningCertificate(x509KeyInfo.getTextContent());
                            } else if (KeyTypes.ENCRYPTION.equals(keyDescriptorType.getUse())) {
                                niaIdentityProviderConfig.setEncryptionPublicKey(x509KeyInfo.getTextContent());
                            } else if (keyDescriptorType.getUse() == null) {
                                defaultCertificate = x509KeyInfo.getTextContent();
                            }
                        }
                    }

                    if (defaultCertificate != null) {
                        if (niaIdentityProviderConfig.getSigningCertificates().length == 0) {
                            niaIdentityProviderConfig.addSigningCertificate(defaultCertificate);
                        }

                        if (niaIdentityProviderConfig.getEncryptionPublicKey() == null) {
                            niaIdentityProviderConfig.setEncryptionPublicKey(defaultCertificate);
                        }
                    }

                    niaIdentityProviderConfig.setEnabledFromMetadata(entityType.getValidUntil() == null
                            || entityType.getValidUntil().toGregorianCalendar().getTime().after(new Date(System.currentTimeMillis())));

                    // check for hide on login attribute
                    if (entityType.getExtensions() != null && entityType.getExtensions().getEntityAttributes() != null) {
                        for (AttributeType attribute : entityType.getExtensions().getEntityAttributes().getAttribute()) {
                            if (MACEDIR_ENTITY_CATEGORY.equals(attribute.getName())
                                    && attribute.getAttributeValue().contains(REFEDS_HIDE_FROM_DISCOVERY)) {
                                niaIdentityProviderConfig.setHideOnLogin(true);
                            }
                        }

                    }

                    return niaIdentityProviderConfig.getConfig();
                }
            }
        } catch (ParsingException pe) {
            throw new RuntimeException("Could not parse IdP SAML Metadata", pe);
        }

        return new HashMap<>();
    }

}
