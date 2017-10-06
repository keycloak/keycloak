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

import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * {@link ProxyMapping} describes mapping for hostname regex patterns to a {@link HttpHost} proxy.
 *
 * @author <a href="mailto:thomas.darimont@gmail.com">Thomas Darimont</a>
 */
public class ProxyMapping {

  private static final String DELIMITER = ";";

  private final Map<Pattern, HttpHost> hostPatternToProxyHost;

  /**
   * Creates a new  {@link ProxyMapping} from the provided {@code List} of proxy mapping strings.
   * <p>
   * A proxy mapping string must have the following format: {@code hostnameRegex;www-proxy-uri } with semicolon as a delimiter.
   * This format enables easy configuration via SPI config string in standalone.xml.
   * </p>
   * <p>For example
   * {@code ^.*.(google.com|googleapis.com)$;http://www-proxy.mycorp.local:8080}
   * </p>
   *
   * @param mappings
   */
  public ProxyMapping(List<String> mappings) {
    this(parseProxyMappings(mappings));
  }

  /**
   * Creates a {@link ProxyMapping} from the provided mappings.
   *
   * @param mappings
   */
  public ProxyMapping(Map<Pattern, HttpHost> mappings) {
    this.hostPatternToProxyHost = Collections.unmodifiableMap(mappings);
  }

  private static Map<Pattern, HttpHost> parseProxyMappings(List<String> mapping) {

    if (mapping == null || mapping.isEmpty()) {
      return Collections.emptyMap();
    }

    // Preserve the order provided via mapping
    Map<Pattern, HttpHost> map = new LinkedHashMap<>();

    for (String entry : mapping) {
      String[] hostPatternRegexWithProxyHost = entry.split(DELIMITER);
      String hostPatternRegex = hostPatternRegexWithProxyHost[0];
      String proxyUrl = hostPatternRegexWithProxyHost[1];

      URI uri = URI.create(proxyUrl);
      HttpHost proxy = new HttpHost(uri.getHost(), uri.getPort(), uri.getScheme());

      Pattern hostPattern = Pattern.compile(hostPatternRegex);
      map.put(hostPattern, proxy);
    }

    return map;
  }

  public boolean isEmpty() {
    return this.hostPatternToProxyHost.isEmpty();
  }

  /**
   * @param hostname
   * @return the {@link HttpHost} proxy associated with the first matching hostname {@link Pattern} or {@literal null} if none matches.
   */
  public HttpHost getProxyFor(String hostname) {

    Objects.requireNonNull(hostname, "hostname");

    for (Map.Entry<Pattern, HttpHost> entry : hostPatternToProxyHost.entrySet()) {

      Pattern hostnamePattern = entry.getKey();
      HttpHost proxy = entry.getValue();

      if (hostnamePattern.matcher(hostname).matches()) {
        return proxy;
      }
    }

    return null;
  }
}
