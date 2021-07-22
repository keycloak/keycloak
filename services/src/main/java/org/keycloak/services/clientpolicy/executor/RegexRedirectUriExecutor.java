package org.keycloak.services.clientpolicy.executor;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jboss.logging.Logger;
import org.keycloak.representations.idm.ClientPolicyExecutorConfigurationRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyEvent;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.AuthorizationRequestContext;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.keycloak.OAuthErrorException.INVALID_REDIRECT_URI;
import static org.keycloak.services.clientpolicy.executor.RegexRedirectUriExecutorFactory.REGEX_PATTERNS_CONFIG_FIELD;

public class RegexRedirectUriExecutor 
    implements ClientPolicyExecutorProvider<RegexRedirectUriExecutor.Configuration> {

  private static final Logger logger = Logger.getLogger(RegexRedirectUriExecutor.class);

  private Configuration configuration;

  public RegexRedirectUriExecutor() {
  }

  public void setupConfiguration(Configuration config) {
    this.configuration = config;
  }

  public Class<Configuration> getExecutorConfigurationClass() {
    return Configuration.class;
  }

  public String getProviderId() {
    return "regex-redirect-uri";
  }
  
  public void executeOnEvent(ClientPolicyContext context) throws ClientPolicyException {
    if (context.getEvent().equals(ClientPolicyEvent.AUTHORIZATION_REQUEST)) {
      this.checkRedirectUri(((AuthorizationRequestContext) context).getRedirectUri());
    }
  }

  private void checkRedirectUri(String redirectUri) throws ClientPolicyException {
    if (redirectUri != null && !redirectUri.isEmpty()) {
      logger.tracev("Redirect URI = {0}", redirectUri);

      List<String> patterns = this.configuration.getRedirectUriRegexPatterns();
      
      for (String pattern : patterns) {
        Pattern regexPattern = Pattern.compile(pattern);
        Matcher matcher = regexPattern.matcher(redirectUri);
        if (matcher.matches()) {
          return;
        }
      }
      
      throw new ClientPolicyException(INVALID_REDIRECT_URI, "Invalid redirect_uri");

    } else {
      throw new ClientPolicyException(INVALID_REDIRECT_URI, "no redirect_uri specified.");
    }
  }

  public static class Configuration extends ClientPolicyExecutorConfigurationRepresentation {
    @JsonProperty(REGEX_PATTERNS_CONFIG_FIELD) 
    protected List<String> redirectUriRegexPatterns;

    public Configuration() {
    }

    public List<String> getRedirectUriRegexPatterns() {
      return redirectUriRegexPatterns;
    }

    public void setRedirectUriRegexPatterns(List<String> redirectUriRegexPatterns) {
      this.redirectUriRegexPatterns = redirectUriRegexPatterns;
    }
  }

}
