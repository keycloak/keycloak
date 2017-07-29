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

package org.keycloak.policy;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Creates {@link BlacklistPasswordPolicyProvider} instances.
 * <p>
 * Password blacklists are simple text files where every line is a blacklisted password delimited by {@code \n}.
 * Blacklist files are discovered and registered at startup.
 * <p>Blacklists can be configured via the <em>Authentication: Password Policy</em> section in the admin-console.
 * A blacklist is referred to by its name in the policy configuration.
 * <p>Keycloak provides a few password blacklists by default.
 * Note: Names of default blacklists are prefixed with {@code default/}.
 * <p>The following blacklists are currently supported by default.
 * <ul>
 * <li>default/empty-password-blacklist.txt</li>
 * <li>default/test-password-blacklist.txt</li>
 * </ul>
 * <p>Users can provide custom blacklists via the password-policy SPI.
 * Note: Name of custom blacklists are prefixed with {@code custom/}.
 * <p>To register a custom password blacklist, run the following jboss-cli script:
 * <pre>{@code
 * /subsystem=keycloak-server/spi=password-policy:add()
 * /subsystem=keycloak-server/spi=password-policy/provider=passwordBlacklist:add(enabled=true)
 * /subsystem=keycloak-server/spi=password-policy/provider=passwordBlacklist:write-attribute(name=properties.blacklistsFolderUri, value=file:///data/keycloak/blacklists/)
 * }
 * </pre>
 * <p>A password blacklist with the filename {@code 10_million_password_list_top_1000000-password-blacklist.txt}
 * that is located beneath {@code /data/keycloak/blacklists/} can be referred to
 * as {@code custom/10_million_password_list_top_1000000-password-blacklist.txt} in the <em>Authentication: Password Policy</em> configuration.
 *
 *
 * @author <a href="mailto:thomas.darimont@gmail.com">Thomas Darimont</a>
 */
public class BlacklistPasswordPolicyProviderFactory implements PasswordPolicyProviderFactory {

  private static final Logger LOG = Logger.getLogger(BlacklistPasswordPolicyProviderFactory.class);

  public static final String ID = "passwordBlacklist";

  private Map<String, PasswordBlacklist> blacklistRegistry;

  @Override
  public PasswordPolicyProvider create(KeycloakSession session) {
    return new BlacklistPasswordPolicyProvider(session.getContext(), blacklistRegistry);
  }

  @Override
  public void init(Config.Scope config) {
    this.blacklistRegistry = findPasswordBlacklists(config);
  }

  private Map<String, PasswordBlacklist> findPasswordBlacklists(Config.Scope config) {

    Map<String, PasswordBlacklist> blacklists = new HashMap<>();

    try {
      // TODO where to place and how to find default blacklists?
      // currently default backlists reside in server-spi-private/src/main/resources/blacklists
      blacklists.putAll(findPasswordBlacklists("default", //
        getClass().getClassLoader().getResource("blacklists").toURI()));
    } catch (URISyntaxException e) {
      LOG.warn("Could not load default password blacklist", e);
    }

    // TODO should we use a custom SPI for password-blacklist discovery to let users drop-in password blacklists?
    String customBlacklistFolderUri = config.get("blacklistsFolderUri");
    if (customBlacklistFolderUri == null) {
      return blacklists;
    }

    blacklists.putAll(findPasswordBlacklists("custom", URI.create(customBlacklistFolderUri)));

    return blacklists;
  }

  /**
   * Discovers password blacklist files stored beneath the given {@code baseUri}.
   * The name of a found password blacklist is prefixed with {@code prefix} and a {@code '/'}.
   *
   * @param prefix
   * @param baseUri
   * @return a {@link Map} with the found password blacklists
   */
  private Map<String, PasswordBlacklist> findPasswordBlacklists(String prefix, URI baseUri) {

    try {
      return Files.list(Paths.get(baseUri)) //
        .map(p -> new PasswordBlacklist(prefix + "/" + p.getFileName(), p.toUri())) //
        .peek(pbl -> LOG.infof("Found password blacklist name=%s", pbl.getName())) //
        .collect(Collectors.toMap(PasswordBlacklist::getName, Function.identity()));
    } catch (Exception e) {
      LOG.errorf("Error during password blacklist discovery", e);
      return Collections.emptyMap();
    }
  }

  @Override
  public void postInit(KeycloakSessionFactory factory) {
  }

  @Override
  public void close() {
  }

  @Override
  public String getDisplayName() {
    return "Password Blacklist";
  }

  @Override
  public String getConfigType() {
    return PasswordPolicyProvider.STRING_CONFIG_TYPE;
  }

  @Override
  public String getDefaultConfigValue() {
    return "";
  }

  @Override
  public boolean isMultiplSupported() {
    return false;
  }

  @Override
  public String getId() {
    return ID;
  }

  /**
   * Describes a lists of too easy to guess passwords or potentially leaked passwords
   * that users should not be able to use.
   */
  public static class PasswordBlacklist {

    private final String name;

    private final URI resourceUri;

    private Set<String> blacklist;

    public PasswordBlacklist(String name, URI resourceUri) {
      this.name = name;
      this.resourceUri = resourceUri;
    }

    void lazyInit() {

      if (blacklist != null) {
        return;
      }

      this.blacklist = loadBlacklist();
    }

    /**
     * Loads the referenced blacklist into a {@link java.util.Set}.
     *
     * @return the {@link java.util.Set} backing a password blacklist
     */
    private Set<String> loadBlacklist() {

      //TODO use more efficient data structure (e.g. a PatriciaTrie) to support large blacklists.
      /*
       * The initial implementation used a PatriciaTree from apache commons-collections4 but
       * the dependency couldn't be added to the wildfly base.
       */
      try (Scanner scanner = new Scanner(resourceUri.toURL().openStream())) {
        LOG.debugf("Loading blacklist for %s: start", name);
        Set<String> set = new TreeSet<>();
        while (scanner.hasNextLine()) {
          set.add(scanner.nextLine());
        }
        LOG.debugf("Loading blacklist for %s: end", name);
        return set;
      } catch (IOException e) {
        throw new RuntimeException("Could not load password blacklist from uri: " + resourceUri, e);
      }
    }

    public String getName() {
      return name;
    }

    public boolean isEmpty() {
      return blacklist == null || blacklist.isEmpty();
    }

    public boolean contains(String password) {
      return blacklist != null && blacklist.contains(password);
    }
  }
}
