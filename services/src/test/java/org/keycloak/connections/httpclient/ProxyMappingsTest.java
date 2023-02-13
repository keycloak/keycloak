/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.connections.httpclient;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.keycloak.connections.httpclient.ProxyMappings.ProxyMapping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link ProxyMappings}.
 *
 * @author <a href="mailto:thomas.darimont@gmail.com">Thomas Darimont</a>
 */
public class ProxyMappingsTest {

  private static final List<String> DEFAULT_MAPPINGS = Arrays.asList( //
    ".*\\.(google|googleapis)\\.com;http://proxy1:8080", //
    ".*\\.facebook\\.com;http://proxy2:8080" //
  );

  private static final List<String> MAPPINGS_WITH_FALLBACK = new ArrayList<>();

  private static final List<String> MAPPINGS_WITH_FALLBACK_AND_PROXY_EXCEPTION = new ArrayList<>();

  private static final List<String> MAPPINGS_WITH_PROXY_AUTHENTICATION = new ArrayList<>();

  static {
    MAPPINGS_WITH_FALLBACK.addAll(DEFAULT_MAPPINGS);
    MAPPINGS_WITH_FALLBACK.add(".*;http://fallback:8080");
  }

  static {
    MAPPINGS_WITH_FALLBACK_AND_PROXY_EXCEPTION.addAll(DEFAULT_MAPPINGS);
    MAPPINGS_WITH_FALLBACK_AND_PROXY_EXCEPTION.add(".*\\.acme\\.corp\\.com;NO_PROXY");
    MAPPINGS_WITH_FALLBACK_AND_PROXY_EXCEPTION.add(".*;http://fallback:8080");
  }

  static {
    MAPPINGS_WITH_PROXY_AUTHENTICATION.add(".*stackexchange\\.com;http://user01:pas2w0rd@proxy3:88");
    MAPPINGS_WITH_PROXY_AUTHENTICATION.addAll(MAPPINGS_WITH_FALLBACK_AND_PROXY_EXCEPTION);
  }

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  ProxyMappings proxyMappings;

  @Before
  public void setup() {
    ProxyMappings.clearCache();
    proxyMappings = ProxyMappings.valueOf(DEFAULT_MAPPINGS);
  }

  @Test
  public void proxyMappingFromEmptyListShouldBeEmpty() {
    assertThat(new ProxyMappings(new ArrayList<>()).isEmpty(), is(true));
  }

  @Test
  public void shouldReturnProxy1ForConfiguredProxyMapping() {

    ProxyMapping proxy = proxyMappings.getProxyFor("account.google.com");
    assertThat(proxy.getProxyHost(), is(notNullValue()));
    assertThat(proxy.getProxyHost().getHostName(), is("proxy1"));
  }

  @Test
  public void shouldReturnProxy1ForConfiguredProxyMappingAlternative() {

    ProxyMapping proxy = proxyMappings.getProxyFor("www.googleapis.com");
    assertThat(proxy.getProxyHost(), is(notNullValue()));
    assertThat(proxy.getProxyHost().getHostName(), is("proxy1"));
  }

  @Test
  public void shouldReturnProxy1ForConfiguredProxyMappingWithSubDomain() {

    ProxyMapping proxy = proxyMappings.getProxyFor("awesome.account.google.com");
    assertThat(proxy.getProxyHost(), is(notNullValue()));
    assertThat(proxy.getProxyHost().getHostName(), is("proxy1"));
  }

  @Test
  public void shouldReturnProxy2ForConfiguredProxyMapping() {

    ProxyMapping proxy = proxyMappings.getProxyFor("login.facebook.com");
    assertThat(proxy.getProxyHost(), is(notNullValue()));
    assertThat(proxy.getProxyHost().getHostName(), is("proxy2"));
  }

  @Test
  public void shouldReturnNoProxyForUnknownHost() {

    ProxyMapping proxy = proxyMappings.getProxyFor("login.microsoft.com");
    assertThat(proxy.getProxyHost(), is(nullValue()));
  }

  @Test
  public void shouldRejectNull() {

    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("hostname");

    proxyMappings.getProxyFor(null);
  }

  @Test
  public void shouldReturnFallbackForNotExplicitlyMappedHostname() {

    ProxyMappings proxyMappingsWithFallback = ProxyMappings.valueOf(MAPPINGS_WITH_FALLBACK);

    ProxyMapping proxy = proxyMappingsWithFallback.getProxyFor("login.salesforce.com");
    assertThat(proxy.getProxyHost().getHostName(), is("fallback"));
  }

  @Test
  public void shouldReturnCorrectProxyOrFallback() {

    ProxyMappings proxyMappingsWithFallback = ProxyMappings.valueOf(MAPPINGS_WITH_FALLBACK);

    ProxyMapping forGoogle = proxyMappingsWithFallback.getProxyFor("login.google.com");
    assertThat(forGoogle.getProxyHost().getHostName(), is("proxy1"));

    ProxyMapping forFacebook = proxyMappingsWithFallback.getProxyFor("login.facebook.com");
    assertThat(forFacebook.getProxyHost().getHostName(), is("proxy2"));

    ProxyMapping forMicrosoft = proxyMappingsWithFallback.getProxyFor("login.microsoft.com");
    assertThat(forMicrosoft.getProxyHost().getHostName(), is("fallback"));

    ProxyMapping forSalesForce = proxyMappingsWithFallback.getProxyFor("login.salesforce.com");
    assertThat(forSalesForce.getProxyHost().getHostName(), is("fallback"));
  }

  @Test
  public void shouldReturnFallbackForNotExplicitlyMappedHostnameAndHonorProxyExceptions() {

    ProxyMappings proxyMappingsWithFallbackAndProxyException = ProxyMappings.valueOf(MAPPINGS_WITH_FALLBACK_AND_PROXY_EXCEPTION);

    ProxyMapping forGoogle = proxyMappingsWithFallbackAndProxyException.getProxyFor("login.google.com");
    assertThat(forGoogle.getProxyHost().getHostName(), is("proxy1"));

    ProxyMapping forFacebook = proxyMappingsWithFallbackAndProxyException.getProxyFor("login.facebook.com");
    assertThat(forFacebook.getProxyHost().getHostName(), is("proxy2"));

    ProxyMapping forAcmeCorp = proxyMappingsWithFallbackAndProxyException.getProxyFor("myapp.acme.corp.com");
    assertThat(forAcmeCorp.getProxyHost(), is(nullValue()));

    ProxyMapping forMicrosoft = proxyMappingsWithFallbackAndProxyException.getProxyFor("login.microsoft.com");
    assertThat(forMicrosoft.getProxyHost().getHostName(), is("fallback"));

    ProxyMapping forSalesForce = proxyMappingsWithFallbackAndProxyException.getProxyFor("login.salesforce.com");
    assertThat(forSalesForce.getProxyHost().getHostName(), is("fallback"));
  }

  @Test
  public void shouldReturnProxyAuthentication() {

    ProxyMappings proxyMappingsWithProxyAuthen = ProxyMappings.valueOf(MAPPINGS_WITH_PROXY_AUTHENTICATION);

    ProxyMapping forGoogle = proxyMappingsWithProxyAuthen.getProxyFor("login.google.com");
    assertThat(forGoogle.getProxyHost().getHostName(), is("proxy1"));

    ProxyMapping forFacebook = proxyMappingsWithProxyAuthen.getProxyFor("login.facebook.com");
    assertThat(forFacebook.getProxyHost().getHostName(), is("proxy2"));

    ProxyMapping forStackOverflow = proxyMappingsWithProxyAuthen.getProxyFor("stackexchange.com");
    assertThat(forStackOverflow.getProxyHost().getHostName(), is("proxy3"));
    assertThat(forStackOverflow.getProxyHost().getPort(), is(88));
    assertThat(forStackOverflow.getProxyCredentials().getUserName(), is("user01"));
    assertThat(forStackOverflow.getProxyCredentials().getPassword(), is("pas2w0rd"));

    ProxyMapping forAcmeCorp = proxyMappingsWithProxyAuthen.getProxyFor("myapp.acme.corp.com");
    assertThat(forAcmeCorp.getProxyHost(), is(nullValue()));

    ProxyMapping forMicrosoft = proxyMappingsWithProxyAuthen.getProxyFor("login.microsoft.com");
    assertThat(forMicrosoft.getProxyHost().getHostName(), is("fallback"));

    ProxyMapping forSalesForce = proxyMappingsWithProxyAuthen.getProxyFor("login.salesforce.com");
    assertThat(forSalesForce.getProxyHost().getHostName(), is("fallback"));
  }

  @Test
  public void shouldReturnMappingForHttpProxy() {
    ProxyMappings proxyMappings = ProxyMappings.withFixedProxyMapping("https://some-proxy.redhat.com:8080", null);

    ProxyMapping forGoogle = proxyMappings.getProxyFor("login.google.com");
    assertEquals("some-proxy.redhat.com", forGoogle.getProxyHost().getHostName());
  }

  @Test
  public void shouldReturnMappingForHttpProxyWithNoProxy() {
    ProxyMappings proxyMappings = ProxyMappings.withFixedProxyMapping("https://some-proxy.redhat.com:8080", "login.facebook.com");

    assertEquals("some-proxy.redhat.com", proxyMappings.getProxyFor("login.google.com").getProxyHost().getHostName());
    assertEquals("some-proxy.redhat.com", proxyMappings.getProxyFor("facebook.com").getProxyHost().getHostName());

    assertNull(proxyMappings.getProxyFor("login.facebook.com").getProxyHost());
    assertNull(proxyMappings.getProxyFor("auth.login.facebook.com").getProxyHost());
  }

  @Test
  public void shouldReturnMappingForHttpProxyWithMultipleNoProxy() {
    ProxyMappings proxyMappings = ProxyMappings.withFixedProxyMapping("https://some-proxy.redhat.com:8080", "login.facebook.com,corp.com");

    assertEquals("some-proxy.redhat.com", proxyMappings.getProxyFor("login.google.com").getProxyHost().getHostName());
    assertEquals("some-proxy.redhat.com", proxyMappings.getProxyFor("facebook.com").getProxyHost().getHostName());

    assertNull(proxyMappings.getProxyFor("login.facebook.com").getProxyHost());
    assertNull(proxyMappings.getProxyFor("auth.login.facebook.com").getProxyHost());
    assertNull(proxyMappings.getProxyFor("myapp.acme.corp.com").getProxyHost());
  }

  @Test
  public void shouldReturnMappingForNoProxyWithInvalidChars() {
    ProxyMappings proxyMappings = ProxyMappings.withFixedProxyMapping("https://some-proxy.redhat.com:8080", "[lj]ogin.facebook.com");

    assertEquals("some-proxy.redhat.com", proxyMappings.getProxyFor("login.facebook.com").getProxyHost().getHostName());
    assertEquals("some-proxy.redhat.com", proxyMappings.getProxyFor("jogin.facebook.com").getProxyHost().getHostName());
  }

  @Test
  public void shouldReturnEmptyMappingForEmptyHttpProxy() {
    assertNull(ProxyMappings.withFixedProxyMapping(null, "facebook.com"));
  }
}