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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpServer;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHost;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.Config;
import org.keycloak.services.util.JsonConfigProvider;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Tests for {@link DefaultHttpClientFactory}
 */
public class DefaultHttpClientFactoryTest {

  private static final ObjectMapper OM = new ObjectMapper();

  private static final String USER_AGENT_CONFIG_PROPERTY = "user-agent";

  private static final String USER_AGENT_HEADER = "User-agent";

  private static final String ECHO_ENDPOINT = "/echo";

  private HttpServer httpServer;

  private HttpHost localhost;

  private DefaultHttpClientFactory factory;

  @Before
  public void setup() throws Exception {

    this.factory = new DefaultHttpClientFactory();
    this.factory.init(createScopeFrom(Collections.emptyMap()));

    this.httpServer = startEchoWebServerAtFreePort();
    this.localhost = new HttpHost("localhost", httpServer.getAddress().getPort());
  }

  @After
  public void destroy() {
    httpServer.stop(0);
  }

  private Config.Scope createScopeFrom(Map<String, Object> config) throws IOException {
    return new JsonConfigProvider(OM.readTree(OM.writeValueAsString(config)), new Properties()).scope();
  }

  @Test
  public void httpClientShouldUseDefaultUserAgent() throws Exception {

    try (CloseableHttpClient httpClient = factory.createHttpClient(null);
         CloseableHttpResponse response = httpClient.execute(localhost, new HttpGet(ECHO_ENDPOINT))) {

      EchoResponse echo = OM.readValue(response.getEntity().getContent(), EchoResponse.class);

      Assert.assertEquals(DefaultHttpClientFactory.DEFAULT_USER_AGENT, echo.getHeaders().getFirst(USER_AGENT_HEADER));
    }
  }

  @Test
  public void httpClientShouldUseProvidedUserAgent() throws Exception {

    String customUserAgent = "custom-user-agent";

    this.factory.init(createScopeFrom(Collections.singletonMap(USER_AGENT_CONFIG_PROPERTY, customUserAgent)));

    try (CloseableHttpClient httpClient = factory.createHttpClient(null);
         CloseableHttpResponse response = httpClient.execute(localhost, new HttpGet("/echo"))
    ) {

      EchoResponse echo = OM.readValue(response.getEntity().getContent(), EchoResponse.class);

      Assert.assertEquals(customUserAgent, echo.getHeaders().getFirst(USER_AGENT_HEADER));
    }
  }

  private HttpServer startEchoWebServerAtFreePort() throws IOException {

    HttpServer httpServer = HttpServer.create(new InetSocketAddress(0), 0);
    httpServer.createContext(ECHO_ENDPOINT, exchange -> {

      Map<String, Object> echoResponse = new HashMap<>();
      echoResponse.put("headers", exchange.getRequestHeaders());
      echoResponse.put("body", IOUtils.toString(exchange.getRequestBody(), "UTF-8"));

      byte[] response = OM.writeValueAsString(echoResponse).getBytes();

      exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
      exchange.getResponseBody().write(response);
      exchange.close();

    });
    httpServer.start();

    return httpServer;
  }

  private static class EchoResponse {

    String body;

    Headers headers;

    public String getBody() {
      return body;
    }

    public void setBody(String body) {
      this.body = body;
    }

    public Headers getHeaders() {
      return headers;
    }

    public void setHeaders(Headers headers) {
      this.headers = headers;
    }
  }
}