package org.keycloak.policy;

import org.keycloak.models.KeycloakContext;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.policy.BlacklistPasswordPolicyProviderFactory.FileBasedPasswordBlacklist;
import org.keycloak.policy.BlacklistPasswordPolicyProviderFactory.PasswordBlacklist;

/**
 * Checks a password against a configured password blacklist.
 *
 * @author <a href="mailto:thomas.darimont@gmail.com">Thomas Darimont</a>
 */
public class BlacklistPasswordPolicyProvider implements PasswordPolicyProvider {

  public static final String ERROR_MESSAGE = "invalidPasswordBlacklistedMessage";

  private final KeycloakContext context;

  private final BlacklistPasswordPolicyProviderFactory factory;

  public BlacklistPasswordPolicyProvider(KeycloakContext context, BlacklistPasswordPolicyProviderFactory factory) {
    this.context = context;
    this.factory = factory;
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

    if (!(policyConfig instanceof PasswordBlacklist)) {
      return null;
    }

    PasswordBlacklist blacklist = (FileBasedPasswordBlacklist) policyConfig;

    if (!blacklist.contains(password.toLowerCase())) {
      return null;
    }

    return new PolicyError(ERROR_MESSAGE);
  }

  @Override
  public PolicyError validate(RealmModel realm, UserModel user, String password) {
    return validate(user.getUsername(), password);
  }

  /**
   * Parses the allowed configuration for a {@link BlacklistPasswordPolicyProvider}.
   * Supported syntax is {@¢ode passwordBlacklist(fileName)}
   *
   * Example configurations:
   * <ul>
   *     <li>{@code passwordBlacklist(test-password-blacklist.txt)}</li>
   * </ul>
   *
   * @param blacklistName
   * @return
   */
  @Override
  public Object parseConfig(String blacklistName) {

    if (blacklistName == null) {
      return null;
    }

    return factory.resolvePasswordBlacklist(blacklistName);
  }

  @Override
  public void close() {
    //noop
  }
}
