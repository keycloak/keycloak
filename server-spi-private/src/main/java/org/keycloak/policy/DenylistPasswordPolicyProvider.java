package org.keycloak.policy;

import org.keycloak.models.KeycloakContext;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.policy.DenylistPasswordPolicyProviderFactory.FileBasedPasswordDenylist;
import org.keycloak.policy.DenylistPasswordPolicyProviderFactory.PasswordDenylist;

/**
 * Checks a password against a configured password denylist.
 *
 * @author <a href="mailto:thomas.darimont@gmail.com">Thomas Darimont</a>
 */
public class DenylistPasswordPolicyProvider implements PasswordPolicyProvider {

  public static final String ERROR_MESSAGE = "invalidPasswordBlacklistedMessage";

  private final KeycloakContext context;

  private final DenylistPasswordPolicyProviderFactory factory;

  public DenylistPasswordPolicyProvider(KeycloakContext context, DenylistPasswordPolicyProviderFactory factory) {
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

    Object policyConfig = context.getRealm().getPasswordPolicy().getPolicyConfig(DenylistPasswordPolicyProviderFactory.ID);
    if (policyConfig == null) {
      return null;
    }

    if (!(policyConfig instanceof PasswordDenylist)) {
      return null;
    }

    PasswordDenylist denylist = (FileBasedPasswordDenylist) policyConfig;

    if (!denylist.contains(password.toLowerCase())) {
      return null;
    }

    return new PolicyError(ERROR_MESSAGE);
  }

  @Override
  public PolicyError validate(RealmModel realm, UserModel user, String password) {
    return validate(user.getUsername(), password);
  }

  /**
   * Parses the allowed configuration for a {@link DenylistPasswordPolicyProvider}.
   * Supported syntax is {@¢ode passwordBlacklist(fileName)}
   *
   * Example configurations:
   * <ul>
   *     <li>{@code passwordBlacklist(test-password-blacklist.txt)}</li>
   * </ul>
   *
   * @param denylistName
   * @return
   */
  @Override
  public Object parseConfig(String denylistName) {

    if (denylistName == null) {
      return null;
    }

    return factory.resolvePasswordDenylist(denylistName);
  }

  @Override
  public void close() {
    //noop
  }
}
