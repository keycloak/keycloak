package org.keycloak.policy;

import org.keycloak.models.KeycloakContext;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.policy.BlacklistPasswordPolicyProviderFactory.PasswordBlacklist;

import java.util.Map;

/**
 * Checks a password against a configured password blacklist.
 *
 * @author <a href="mailto:thomas.darimont@gmail.com">Thomas Darimont</a>
 */
public class BlacklistPasswordPolicyProvider implements PasswordPolicyProvider {

  public static final String ERROR_MESSAGE = "invalidPasswordBlacklistedMessage";

  private final KeycloakContext context;

  private final Map<String, PasswordBlacklist> blacklistRegistry;

  public BlacklistPasswordPolicyProvider(KeycloakContext context, Map<String, PasswordBlacklist> blacklistRegistry) {
    this.context = context;
    this.blacklistRegistry = blacklistRegistry;
  }

  /**
   * Checks whether the provided password is contained in the configured blacklist.
   *
   * @param username
   * @param password
   * @return {@literal null} if the password is not blacklisted otherwise a {@link PolicyError}
   */
  @Override
  public PolicyError validate(String username, String password) {

    Object policyConfig = context.getRealm().getPasswordPolicy().getPolicyConfig(BlacklistPasswordPolicyProviderFactory.ID);
    if (policyConfig == null) {
      return null;
    }

    if (!PasswordBlacklist.class.isInstance(policyConfig)) {
      return null;
    }

    PasswordBlacklist blacklist = (PasswordBlacklist) policyConfig;

    if (blacklist.isEmpty()) {
      return null;
    }

    if (!blacklist.contains(password)) {
      return null;
    }

    return new PolicyError(ERROR_MESSAGE);
  }

  @Override
  public PolicyError validate(RealmModel realm, UserModel user, String password) {
    return validate(user.getUsername(), password);
  }

  @Override
  public Object parseConfig(String blacklistName) {

    if (blacklistName == null) {
      return null;
    }

    if (blacklistName.trim().isEmpty()) {
      return null;
    }

    if (!blacklistRegistry.containsKey(blacklistName)) {
      //TODO this might be too hard since it could prevent the server from starting...
      throw new IllegalArgumentException("Password blacklist " + blacklistName + " not found!");
    }

    return blacklistRegistry.computeIfPresent(blacklistName, (name, pbl) -> {
      pbl.lazyInit();
      return pbl;
    });
  }

  @Override
  public void close() {
    //noop
  }
}
