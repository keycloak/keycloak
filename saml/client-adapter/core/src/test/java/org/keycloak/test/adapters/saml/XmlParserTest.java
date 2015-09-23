package org.keycloak.test.adapters.saml;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.adapters.saml.config.IDP;
import org.keycloak.adapters.saml.config.Key;
import org.keycloak.adapters.saml.config.KeycloakSamlAdapter;
import org.keycloak.adapters.saml.config.SP;
import org.keycloak.adapters.saml.config.parsers.KeycloakSamlAdapterXMLParser;

import java.io.InputStream;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class XmlParserTest {

    @Test
    public void testXmlParser() throws Exception {
        InputStream is = getClass().getResourceAsStream("/keycloak-saml.xml");
        Assert.assertNotNull(is);
        KeycloakSamlAdapterXMLParser parser = new KeycloakSamlAdapterXMLParser();
        KeycloakSamlAdapter config = (KeycloakSamlAdapter)parser.parse(is);
        Assert.assertNotNull(config);
        Assert.assertEquals(1, config.getSps().size());
        SP sp = config.getSps().get(0);
        Assert.assertEquals("sp", sp.getEntityID());
        Assert.assertEquals("ssl", sp.getSslPolicy());
        Assert.assertEquals("format", sp.getNameIDPolicyFormat());
        Assert.assertTrue(sp.isForceAuthentication());
        Assert.assertEquals(2, sp.getKeys().size());
        Key signing = sp.getKeys().get(0);
        Assert.assertTrue(signing.isSigning());
        Key.KeyStoreConfig keystore = signing.getKeystore();
        Assert.assertNotNull(keystore);
        Assert.assertEquals("file", keystore.getFile());
        Assert.assertEquals("cp", keystore.getResource());
        Assert.assertEquals("pw", keystore.getPassword());
        Assert.assertEquals("private alias", keystore.getPrivateKeyAlias());
        Assert.assertEquals("private pw", keystore.getPrivateKeyPassword());
        Assert.assertEquals("cert alias", keystore.getCertificateAlias());
        Key encryption = sp.getKeys().get(1);
        Assert.assertTrue(encryption.isEncryption());
        Assert.assertEquals("private pem", encryption.getPrivateKeyPem());
        Assert.assertEquals("public pem", encryption.getPublicKeyPem());
        Assert.assertEquals("policy", sp.getPrincipalNameMapping().getPolicy());
        Assert.assertEquals("attribute", sp.getPrincipalNameMapping().getAttributeName());
        Assert.assertTrue(sp.getRoleAttributes().size() == 1);
        Assert.assertTrue(sp.getRoleAttributes().contains("member"));
        Assert.assertTrue(sp.getRoleFriendlyAttributes().size() == 1);
        Assert.assertTrue(sp.getRoleFriendlyAttributes().contains("memberOf"));

        IDP idp = sp.getIdp();
        Assert.assertEquals("idp", idp.getEntityID());
        Assert.assertTrue(idp.getSingleSignOnService().isSignRequest());
        Assert.assertTrue(idp.getSingleSignOnService().isValidateResponseSignature());
        Assert.assertEquals("post", idp.getSingleSignOnService().getRequestBinding());
        Assert.assertEquals("url", idp.getSingleSignOnService().getBindingUrl());

        Assert.assertTrue(idp.getSingleLogoutService().isSignRequest());
        Assert.assertTrue(idp.getSingleLogoutService().isSignResponse());
        Assert.assertTrue(idp.getSingleLogoutService().isValidateRequestSignature());
        Assert.assertTrue(idp.getSingleLogoutService().isValidateResponseSignature());
        Assert.assertEquals("redirect", idp.getSingleLogoutService().getRequestBinding());
        Assert.assertEquals("post", idp.getSingleLogoutService().getResponseBinding());
        Assert.assertEquals("posturl", idp.getSingleLogoutService().getPostBindingUrl());
        Assert.assertEquals("redirecturl", idp.getSingleLogoutService().getRedirectBindingUrl());

        Assert.assertTrue(idp.getKeys().size() == 1);
        Assert.assertTrue(idp.getKeys().get(0).isSigning());
        Assert.assertEquals("cert pem", idp.getKeys().get(0).getCertificatePem());




    }
}
