package org.keycloak.services.clientpolicy.executor;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.ArrayList;
import java.util.List;

public class RegexRedirectUriExecutorFactory implements ClientPolicyExecutorProviderFactory {
  public static final String PROVIDER_ID = "regex-redirect-uri";
  public static final String REGEX_PATTERNS_CONFIG_FIELD = "redirect-uri-regex-patterns";
  private List<ProviderConfigProperty> configProperties = new ArrayList();

  public RegexRedirectUriExecutorFactory() {
  }

  public ClientPolicyExecutorProvider create(KeycloakSession session) {
    return new RegexRedirectUriExecutor();
  }

  public void init(Config.Scope config) {
  }
  
  public void postInit(KeycloakSessionFactory factory) {
    ProviderConfigProperty regexPatterns = 
        new ProviderConfigProperty(REGEX_PATTERNS_CONFIG_FIELD, "Redirect URI Regex Patterns",
        "Regex-Patterns with which the redirect-URI is checked against", 
            "MultivaluedString", null);
    
    this.configProperties.add(regexPatterns);
  }

  public void close() {
  }

  public String getId() {
    return PROVIDER_ID;
  }

  public String getHelpText() {
    return "Checks if the redirect URI matches a configured regex pattern.";
  }

  public List<ProviderConfigProperty> getConfigProperties() {
    return this.configProperties;
  }
  
}
