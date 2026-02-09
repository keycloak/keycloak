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

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.http.HttpHost;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.jboss.logging.Logger;

import static org.keycloak.utils.StringUtil.isBlank;

/**
 * {@link ProxyMappings} describes an ordered mapping for hostname regex patterns to a {@link HttpHost} proxy.
 * <p>
 * Mappings can be created via {@link #valueOf(String...)} or {@link #valueOf(List)}.
 * For a description of the mapping format see {@link ProxyMapping#valueOf(String)}
 *
 * @author <a href="mailto:thomas.darimont@gmail.com">Thomas Darimont</a>
 */
public class ProxyMappings {

  private static final Logger logger = Logger.getLogger(ProxyMappings.class);

  private static final ProxyMappings EMPTY_MAPPING = valueOf(Collections.emptyList());

  private static final String NO_PROXY_DELIMITER = ",";

  private final List<ProxyMapping> entries;

  private static final Map<String, ProxyMapping> hostnameToProxyCache = new ConcurrentHashMap<>();

  /**
   * Creates a {@link ProxyMappings} from the provided {@link ProxyMapping Entries}.
   *
   * @param entries
   */
  public ProxyMappings(List<ProxyMapping> entries) {
    this.entries = Collections.unmodifiableList(entries);
  }

  /**
   * Creates a new  {@link ProxyMappings} from the provided {@code List} of proxy mapping strings.
   * <p>
   *
   * @param proxyMappings
   */
  public static ProxyMappings valueOf(List<String> proxyMappings) {

    if (proxyMappings == null || proxyMappings.isEmpty()) {
      return EMPTY_MAPPING;
    }

    List<ProxyMapping> entries = proxyMappings.stream() //
      .map(ProxyMapping::valueOf) //
      .collect(Collectors.toList());

    return new ProxyMappings(entries);
  }

  /**
   * Creates a new  {@link ProxyMappings} from the provided {@code String[]} of proxy mapping strings.
   *
   * @param proxyMappings
   * @return
   * @see #valueOf(List)
   * @see ProxyMapping#valueOf(String...)
   */
  public static ProxyMappings valueOf(String... proxyMappings) {

    if (proxyMappings == null || proxyMappings.length == 0) {
      return EMPTY_MAPPING;
    }

    return valueOf(Arrays.asList(proxyMappings));
  }

  /**
   * Creates a new {@link ProxyMappings} from provided parameters representing the established {@code HTTP(S)_PROXY}
   * and {@code NO_PROXY} environment variables.
   *
   * @param httpProxy a proxy used for all hosts except the ones specified in {@code noProxy}
   * @param noProxy a list of hosts (separated by comma) that should not use proxy;
   *                all suffixes are matched too (e.g. redhat.com will also match access.redhat.com)
   * @return
   * @see <a href="https://about.gitlab.com/blog/2021/01/27/we-need-to-talk-no-proxy/">https://about.gitlab.com/blog/2021/01/27/we-need-to-talk-no-proxy/</a>
   */
  public static ProxyMappings withFixedProxyMapping(String httpProxy, String noProxy) {
    List<ProxyMapping> proxyMappings = new ArrayList<>();

    if (!isBlank(httpProxy)) {
      // noProxy must be first as it's more specific than .*
      if (!isBlank(noProxy)) {
        for (String host : noProxy.split(NO_PROXY_DELIMITER)) {
          // do not support regex in no_proxy
          proxyMappings.add(new ProxyMapping(Pattern.compile("(?:.+\\.)?" + Pattern.quote(host)), null, null));
        }
      }

      proxyMappings.add(ProxyMapping.valueOf(".*" + ProxyMapping.DELIMITER + httpProxy));
    }

    return proxyMappings.isEmpty() ? EMPTY_MAPPING : new ProxyMappings(proxyMappings);
  }


  public boolean isEmpty() {
    return this.entries.isEmpty();
  }

  /**
   * @param hostname
   * @return the {@link ProxyMapping} associated with the first matching hostname {@link Pattern}
   * or the {@link ProxyMapping} including {@literal null} as {@link HttpHost} if none matches.
   */
  public ProxyMapping getProxyFor(String hostname) {

    Objects.requireNonNull(hostname, "hostname");
    if (hostnameToProxyCache.containsKey(hostname)) {
      return hostnameToProxyCache.get(hostname);
    }
    ProxyMapping proxyMapping = entries.stream() //
            .filter(e -> e.matches(hostname)) //
            .findFirst() //
            .orElse(null);
    if (proxyMapping == null) {
      proxyMapping = new ProxyMapping(null, null, null);
    }
    hostnameToProxyCache.put(hostname, proxyMapping);
    return proxyMapping;
  }

  public static void clearCache() {
    hostnameToProxyCache.clear();
  }

  /**
   * {@link ProxyMapping} describes a Proxy Mapping with a Hostname {@link Pattern}
   * that is mapped to a proxy {@link HttpHost}.
   */
  public static class ProxyMapping {

    public static final String NO_PROXY = "NO_PROXY";
    private static final String DELIMITER = ";";

    private final Pattern hostnamePattern;

    private final HttpHost proxyHost;

    private final UsernamePasswordCredentials proxyCredentials;

    public ProxyMapping(Pattern hostnamePattern, HttpHost proxyHost, UsernamePasswordCredentials proxyCredentials) {
      this.hostnamePattern = hostnamePattern;
      this.proxyHost = proxyHost;
      this.proxyCredentials = proxyCredentials;
    }

    public Pattern getHostnamePattern() {
      return hostnamePattern;
    }

    public HttpHost getProxyHost() {
      return proxyHost;
    }

    public UsernamePasswordCredentials getProxyCredentials() {
      return proxyCredentials;
    }

    public boolean matches(String hostname) {
      return getHostnamePattern().matcher(hostname).matches();
    }

    /**
     * Parses a mapping string into an {@link ProxyMapping}.
     * <p>
     * A proxy mapping string must have the following format: {@code hostnameRegex;www-proxy-uri}
     * with semicolon as a delimiter.</p>
     * <p>
     * If no proxy should be used for a host pattern then use {@code NO_PROXY} as www-proxy-uri.
     * </p>
     * <p>Examples:
     * <pre>
     * {@code
     *
     * .*\.(google\.com|googleapis\.com);http://www-proxy.acme.corp.com:8080
     * .*\.acme\.corp\.com;NO_PROXY
     * .*;http://fallback:8080
     * }
     * </pre>
     * </p>
     *
     * @param mapping
     * @return
     */
    public static ProxyMapping valueOf(String mapping) {

      String[] mappingTokens = mapping.split(DELIMITER);

      String hostPatternRegex = mappingTokens[0];
      String proxyUriString = mappingTokens[1];

      Pattern hostPattern = Pattern.compile(hostPatternRegex);
      if (NO_PROXY.equals(proxyUriString)) {
        return new ProxyMapping(hostPattern, null, null);
      }

      URI uri = URI.create(proxyUriString);
      String userInfo = uri.getUserInfo();
      UsernamePasswordCredentials proxyCredentials = null;
      if (userInfo != null) {
        if (userInfo.indexOf(":") > 0) {
          String[] credencials = userInfo.split(":", 2);
          if (credencials != null && credencials.length == 2) {
            proxyCredentials = new UsernamePasswordCredentials(credencials[0], credencials[1]);
          }
        } else {
          logger.warn("Invalid proxy credentials: " + userInfo);
        }
      }
      return new ProxyMapping(hostPattern, new HttpHost(uri.getHost(), uri.getPort(), uri.getScheme()), proxyCredentials);
    }

    @Override
    public String toString() {
      return "ProxyMapping{" +
        "hostnamePattern=" + hostnamePattern +
        ", proxyHost=" + proxyHost +
        '}';
    }
  }
}
