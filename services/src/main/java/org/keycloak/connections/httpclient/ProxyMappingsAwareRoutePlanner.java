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

import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.conn.DefaultRoutePlanner;
import org.apache.http.impl.conn.DefaultSchemePortResolver;
import org.apache.http.protocol.HttpContext;
import org.jboss.logging.Logger;

import static org.keycloak.connections.httpclient.ProxyMappings.ProxyMapping;

/**
 * A {@link DefaultRoutePlanner} that determines the proxy to use for a given target hostname by consulting
 * the given {@link ProxyMappings}.
 *
 * @author <a href="mailto:thomas.darimont@gmail.com">Thomas Darimont</a>
 * @see ProxyMappings
 */
public class ProxyMappingsAwareRoutePlanner extends DefaultRoutePlanner {

  private static final Logger LOG = Logger.getLogger(ProxyMappingsAwareRoutePlanner.class);

  private final ProxyMappings proxyMappings;

  public ProxyMappingsAwareRoutePlanner(ProxyMappings proxyMappings) {
    super(DefaultSchemePortResolver.INSTANCE);
    this.proxyMappings = proxyMappings;
  }

  @Override
  protected HttpHost determineProxy(HttpHost target, HttpRequest request, HttpContext context) throws HttpException {

    String targetHostName = target.getHostName();
    ProxyMapping proxyMapping = proxyMappings.getProxyFor(targetHostName);
    LOG.debugf("Returning proxyMapping=%s for targetHost=%s", proxyMapping, targetHostName);
    UsernamePasswordCredentials proxyCredentials = proxyMapping.getProxyCredentials();
    HttpHost proxyHost = proxyMapping.getProxyHost();
    if (proxyCredentials != null) {
      CredentialsProvider credsProvider = new BasicCredentialsProvider();
      credsProvider.setCredentials(new AuthScope(proxyHost.getHostName(), proxyHost.getPort()), proxyCredentials);
      context.setAttribute(HttpClientContext.CREDS_PROVIDER, credsProvider);
    }
    return proxyHost;
  }
}
