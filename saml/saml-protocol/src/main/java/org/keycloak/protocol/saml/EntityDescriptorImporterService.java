package org.keycloak.protocol.saml;

import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.services.resources.admin.RealmAuth;
import org.picketlink.common.constants.JBossSAMLURIConstants;
import org.picketlink.common.exceptions.ConfigurationException;
import org.picketlink.common.exceptions.ParsingException;
import org.picketlink.common.exceptions.ProcessingException;
import org.picketlink.identity.federation.core.parsers.saml.SAMLParser;
import org.picketlink.identity.federation.core.saml.v2.util.SAMLMetadataUtil;
import org.picketlink.identity.federation.core.util.CoreConfigUtil;
import org.picketlink.identity.federation.saml.v2.metadata.EndpointType;
import org.picketlink.identity.federation.saml.v2.metadata.EntitiesDescriptorType;
import org.picketlink.identity.federation.saml.v2.metadata.EntityDescriptorType;
import org.picketlink.identity.federation.saml.v2.metadata.KeyDescriptorType;
import org.picketlink.identity.federation.saml.v2.metadata.KeyTypes;
import org.picketlink.identity.federation.saml.v2.metadata.SPSSODescriptorType;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class EntityDescriptorImporterService {
    protected RealmModel realm;
    protected RealmAuth auth;

    public EntityDescriptorImporterService(RealmModel realm, RealmAuth auth) {
        this.realm = realm;
        this.auth = auth;
    }

    @POST
    @Path("upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public void updateEntityDescriptor(@Context final UriInfo uriInfo, MultipartFormDataInput input) throws IOException {
        auth.requireManage();

        Map<String, List<InputPart>> uploadForm = input.getFormDataMap();
        List<InputPart> inputParts = uploadForm.get("file");

        InputStream is = inputParts.get(0).getBody(InputStream.class, null);

        loadEntityDescriptors(is, realm);

    }

    public static void loadEntityDescriptors(InputStream is, RealmModel realm) {
        Object metadata = null;
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

        for (Object o : entities.getEntityDescriptor()) {
            EntityDescriptorType entity = (EntityDescriptorType)o;
            String entityId = entity.getEntityID();
            ApplicationModel app = realm.addApplication(entityId);
            app.setFullScopeAllowed(true);
            app.setProtocol(SamlProtocol.LOGIN_PROTOCOL);
            app.setAttribute(SamlProtocol.SAML_SERVER_SIGNATURE, SamlProtocol.ATTRIBUTE_TRUE_VALUE); // default to true
            app.setAttribute(SamlProtocol.SAML_SIGNATURE_ALGORITHM, SignatureAlgorithm.RSA_SHA256.toString());
            app.setAttribute(SamlProtocol.SAML_AUTHNSTATEMENT, SamlProtocol.ATTRIBUTE_TRUE_VALUE);
            SPSSODescriptorType spDescriptorType = CoreConfigUtil.getSPDescriptor(entity);
            if (spDescriptorType.isWantAssertionsSigned()) {
                app.setAttribute(SamlProtocol.SAML_ASSERTION_SIGNATURE, SamlProtocol.ATTRIBUTE_TRUE_VALUE);
            }
            String adminUrl = getLogoutLocation(spDescriptorType, JBossSAMLURIConstants.SAML_HTTP_POST_BINDING.get());
            if (adminUrl != null) app.setManagementUrl(adminUrl);

            String urlPattern = CoreConfigUtil.getServiceURL(spDescriptorType, JBossSAMLURIConstants.SAML_HTTP_POST_BINDING.get());
            if (urlPattern == null) {
                urlPattern = CoreConfigUtil.getServiceURL(spDescriptorType, JBossSAMLURIConstants.SAML_HTTP_REDIRECT_BINDING.get());
            }
            if (urlPattern != null) {
                app.addRedirectUri(urlPattern);
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
                    app.setAttribute(SamlProtocol.SAML_CLIENT_SIGNATURE_ATTRIBUTE, SamlProtocol.ATTRIBUTE_TRUE_VALUE);
                    app.setAttribute(SamlProtocol.SAML_SIGNING_CERTIFICATE_ATTRIBUTE, certPem);
                } else if (keyDescriptor.getUse() == KeyTypes.ENCRYPTION) {
                    app.setAttribute(SamlProtocol.SAML_ENCRYPT, SamlProtocol.ATTRIBUTE_TRUE_VALUE);
                    app.setAttribute(SamlProtocol.SAML_ENCRYPTION_CERTIFICATE_ATTRIBUTE, certPem);
                }
            }
        }
    }

    public static String getLogoutLocation(SPSSODescriptorType idp, String bindingURI) {
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


}
