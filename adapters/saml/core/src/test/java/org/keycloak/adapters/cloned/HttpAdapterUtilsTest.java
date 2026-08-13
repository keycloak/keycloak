/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.keycloak.adapters.cloned;

import java.io.InputStream;
import java.security.cert.X509Certificate;
import java.util.List;
import javax.xml.crypto.XMLStructure;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyName;
import javax.xml.crypto.dsig.keyinfo.X509Data;

import org.keycloak.adapters.saml.config.parsers.KeycloakSamlAdapterV1QNames;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.dom.saml.v2.metadata.KeyTypes;
import org.keycloak.saml.common.exceptions.ParsingException;

import org.hamcrest.Matcher;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 *
 * @author hmlnarik
 */
public class HttpAdapterUtilsTest {

    private <T> T getContent(List<?> objects, Class<T> clazz) {
        for (Object o : objects) {
            if (clazz.isInstance(o)) {
                return (T) o;
            }
        }
        return null;
    }

    @Test
    public void testExtractKeysFromSamlDescriptor() throws ParsingException {
        InputStream xmlStream = HttpAdapterUtilsTest.class.getResourceAsStream("saml-descriptor-valid.xml");
        MultivaluedHashMap<String, KeyInfo> res = HttpAdapterUtils.extractKeysFromSamlDescriptor(xmlStream);

        assertThat(res, notNullValue());
        assertThat(res.keySet(), hasItems(KeyTypes.SIGNING.value()));
        assertThat(res.get(KeycloakSamlAdapterV1QNames.ATTR_SIGNING.getQName().getLocalPart()), notNullValue());
        assertThat(res.get(KeycloakSamlAdapterV1QNames.ATTR_SIGNING.getQName().getLocalPart()).size(), equalTo(2));

        KeyInfo ki;
        KeyName keyName;
        X509Data x509data;
        X509Certificate x509certificate;

        Matcher<Iterable<? super XMLStructure>> x509DataMatcher = hasItem(instanceOf(X509Data.class));
        Matcher<Iterable<? super XMLStructure>> keyNameMatcher = hasItem(instanceOf(KeyName.class));

        ki = res.get(KeycloakSamlAdapterV1QNames.ATTR_SIGNING.getQName().getLocalPart()).get(0);
        assertThat(ki.getContent().size(), equalTo(2));

        assertThat((Iterable<? super XMLStructure>) ki.getContent(), x509DataMatcher);
        assertThat((Iterable<? super XMLStructure>) ki.getContent(), keyNameMatcher);

        keyName = getContent(ki.getContent(), KeyName.class);
        assertThat(keyName.getName(), equalTo("rJkJlvowmv1Id74GznieaAC5jU5QQp_ILzuG-GsweTI"));

        x509data = getContent(ki.getContent(), X509Data.class);
        assertThat(x509data, notNullValue());
        x509certificate = getContent(x509data.getContent(), X509Certificate.class);
        assertThat(x509certificate, notNullValue());
        assertThat(x509certificate.getSigAlgName(), equalTo("SHA256withRSA"));

        ki = res.get(KeycloakSamlAdapterV1QNames.ATTR_SIGNING.getQName().getLocalPart()).get(1);
        assertThat(ki.getContent().size(), equalTo(2));
        assertThat((Iterable<? super XMLStructure>) ki.getContent(), x509DataMatcher);
        assertThat((Iterable<? super XMLStructure>) ki.getContent(), keyNameMatcher);

        keyName = getContent(ki.getContent(), KeyName.class);
        assertThat(keyName.getName(), equalTo("BzYc4GwL8HVrAhNyNdp-lTah2DvU9jU03kby9Ynohr4"));

        x509data = getContent(ki.getContent(), X509Data.class);
        assertThat(x509data, notNullValue());
        x509certificate = getContent(x509data.getContent(), X509Certificate.class);
        assertThat(x509certificate, notNullValue());
        assertThat(x509certificate.getSigAlgName(), equalTo("SHA256withRSA"));

    }

}
