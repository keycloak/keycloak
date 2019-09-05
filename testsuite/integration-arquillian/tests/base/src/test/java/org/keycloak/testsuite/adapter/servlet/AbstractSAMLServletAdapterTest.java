package org.keycloak.testsuite.adapter.servlet;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.adapter.AbstractServletsAdapterTest;
import org.keycloak.testsuite.adapter.filter.AdapterActionsFilter;
import org.keycloak.testsuite.utils.io.IOUtil;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.keycloak.testsuite.auth.page.AuthRealm.SAMLSERVLETDEMO;

public abstract class AbstractSAMLServletAdapterTest extends AbstractServletsAdapterTest {

    public static final String WEB_XML_WITH_ACTION_FILTER = "web-with-action-filter.xml";

    @Override
    public void setDefaultPageUriParameters() {
        super.setDefaultPageUriParameters();
        testRealmPage.setAuthRealm(SAMLSERVLETDEMO);
        testRealmSAMLRedirectLoginPage.setAuthRealm(SAMLSERVLETDEMO);
        testRealmSAMLPostLoginPage.setAuthRealm(SAMLSERVLETDEMO);
    }

    @Override
    public void addAdapterTestRealms(List<RealmRepresentation> testRealms) {
        testRealms.add(IOUtil.loadRealm("/adapter-test/keycloak-saml/testsaml.json"));
        testRealms.add(IOUtil.loadRealm("/adapter-test/keycloak-saml/tenant1-realm.json"));
        testRealms.add(IOUtil.loadRealm("/adapter-test/keycloak-saml/tenant2-realm.json"));
    }

    protected void setAdapterAndServerTimeOffset(int timeOffset, String... servletUris) {
        setTimeOffset(timeOffset);

        Arrays.stream(servletUris)
                .map(url -> url += "unsecured")
                .forEach(servletUri -> {
                    String url = UriBuilder.fromUri(servletUri)
                            .queryParam(AdapterActionsFilter.TIME_OFFSET_PARAM, timeOffset)
                            .build().toString();
                    try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
                        HttpUriRequest request = new HttpGet(url);
                        CloseableHttpResponse httpResponse = client.execute(request);

                        System.out.println(EntityUtils.toString(httpResponse.getEntity()));
                        httpResponse.close();
                    } catch (IOException e) {
                        throw new RuntimeException("Cannot change time on url " + url, e);
                    }
                });
    }
}
