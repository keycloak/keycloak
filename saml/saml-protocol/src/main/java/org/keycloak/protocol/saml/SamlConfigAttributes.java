package org.keycloak.protocol.saml;

import org.keycloak.services.resources.admin.ClientAttributeCertificateResource;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface SamlConfigAttributes {
    String SAML_SIGNING_PRIVATE_KEY = "saml.signing.private.key";
    String SAML_CANONICALIZATION_METHOD_ATTRIBUTE = "saml_signature_canonicalization_method";
    String SAML_SIGNATURE_ALGORITHM = "saml.signature.algorithm";
    String SAML_NAME_ID_FORMAT_ATTRIBUTE = "saml_name_id_format";
    String SAML_AUTHNSTATEMENT = "saml.authnstatement";
    String SAML_FORCE_NAME_ID_FORMAT_ATTRIBUTE = "saml_force_name_id_format";
    String SAML_SERVER_SIGNATURE = "saml.server.signature";
    String SAML_FORCE_POST_BINDING = "saml.force.post.binding";
    String SAML_ASSERTION_SIGNATURE = "saml.assertion.signature";
    String SAML_ENCRYPT = "saml.encrypt";
    String SAML_CLIENT_SIGNATURE_ATTRIBUTE = "saml.client.signature";
    String SAML_SIGNING_CERTIFICATE_ATTRIBUTE = "saml.signing." + ClientAttributeCertificateResource.X509CERTIFICATE;
}
