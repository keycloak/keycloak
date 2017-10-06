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

import org.apache.http.HttpHost;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * @author <a href="mailto:thomas.darimont@gmail.com">Thomas Darimont</a>
 */
public class ProxyMappingTest {

  private static final List<String> DEFAULT_MAPPINGS = Arrays.asList( //
    "^.*.(google.com|googleapis.com)$;http://proxy1:8080", //
    "^.*.(facebook.com)$;http://proxy2:8080" //
  );

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  ProxyMapping proxyMapping;

  @Before
  public void setup() {
    proxyMapping = new ProxyMapping(DEFAULT_MAPPINGS);
  }

  @Test
  public void proxyMappingFromEmptyMapShouldBeEmpty() {
    assertThat(new ProxyMapping(Collections.emptyMap()).isEmpty(), is(true));
  }

  @Test
  public void proxyMappingFromEmptyListShouldBeEmpty() {
    assertThat(new ProxyMapping(new ArrayList<>()).isEmpty(), is(true));
  }

  @Test
  public void shouldReturnProxy1ForConfiguredProxyMapping() {

    HttpHost proxy = proxyMapping.getProxyFor("account.google.com");
    assertThat(proxy, is(notNullValue()));
    assertThat(proxy.getHostName(), is("proxy1"));
  }

  @Test
  public void shouldReturnProxy1ForConfiguredProxyMappingWithSubDomain() {

    HttpHost proxy = proxyMapping.getProxyFor("awesome.account.google.com");
    assertThat(proxy, is(notNullValue()));
    assertThat(proxy.getHostName(), is("proxy1"));
  }

  @Test
  public void shouldReturnProxy2ForConfiguredProxyMapping() {

    HttpHost proxy = proxyMapping.getProxyFor("login.facebook.com");
    assertThat(proxy, is(notNullValue()));
    assertThat(proxy.getHostName(), is("proxy2"));
  }

  @Test
  public void shouldReturnNoProxyForUnknownHost() {

    HttpHost proxy = proxyMapping.getProxyFor("login.microsoft.com");
    assertThat(proxy, is(nullValue()));
  }

  @Test
  public void shouldRejectNull() {

    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("hostname");

    proxyMapping.getProxyFor(null);
  }
}