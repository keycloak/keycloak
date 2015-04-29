package org.keycloak.adapters.springsecurity.filter;

import org.keycloak.adapters.AuthChallenge;
import org.keycloak.adapters.AuthOutcome;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.springsecurity.AdapterDeploymentContextBean;
import org.keycloak.adapters.springsecurity.authentication.KeycloakAuthenticationEntryPoint;
import org.keycloak.adapters.springsecurity.authentication.SpringSecurityRequestAuthenticator;
import org.keycloak.adapters.springsecurity.facade.SimpleHttpFacade;
import org.keycloak.adapters.springsecurity.token.SpringSecurityTokenStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestHeaderRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.Assert;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Provides a Keycloak authentication processing filter.
 *
 * @author <a href="mailto:srossillo@smartling.com">Scott Rossillo</a>
 * @version $Revision: 1 $
 */
public class KeycloakAuthenticationProcessingFilter extends AbstractAuthenticationProcessingFilter implements ApplicationContextAware {

    /**
     * Request matcher that matches requests to the {@link KeycloakAuthenticationEntryPoint#DEFAULT_LOGIN_URI default login URI}
     * and any request with a <code>Authorization</code> header.
     */
    public static final RequestMatcher DEFAULT_REQUEST_MATCHER =
            new OrRequestMatcher(new AntPathRequestMatcher("/sso/login"), new RequestHeaderRequestMatcher("Authorization"));

    private static final Logger log = LoggerFactory.getLogger(KeycloakAuthenticationProcessingFilter.class);

    private ApplicationContext applicationContext;
    private AdapterDeploymentContextBean adapterDeploymentContextBean;
    private AuthenticationManager authenticationManager;

    /**
     * Creates a new Keycloak authentication processing filter with given {@link AuthenticationManager} and the
     * {@link KeycloakAuthenticationProcessingFilter#DEFAULT_REQUEST_MATCHER default request matcher}.
     *
     * @param authenticationManager the {@link AuthenticationManager} to authenticate requests (cannot be null)
     * @see KeycloakAuthenticationProcessingFilter#DEFAULT_REQUEST_MATCHER
     */
    public KeycloakAuthenticationProcessingFilter(AuthenticationManager authenticationManager) {
        this(authenticationManager, DEFAULT_REQUEST_MATCHER);
    }

    /**
     * Creates a new Keycloak authentication processing filter with given {@link AuthenticationManager} and
     * {@link RequestMatcher}.
     * <p>
     *     Note: the given request matcher must support matching the <code>Authorization</code> header if
     *     bearer token authentication is to be accepted.
     * </p>
     *
     * @param authenticationManager the {@link AuthenticationManager} to authenticate requests (cannot be null)
     * @param requiresAuthenticationRequestMatcher the {@link RequestMatcher} used to determine if authentication
     *  is required (cannot be null)
     *
     *  @see RequestHeaderRequestMatcher
     *  @see OrRequestMatcher
     *
     */
    public KeycloakAuthenticationProcessingFilter(AuthenticationManager authenticationManager, RequestMatcher
                requiresAuthenticationRequestMatcher) {
        super(requiresAuthenticationRequestMatcher);
        Assert.notNull(authenticationManager, "authenticationManager cannot be null");
        this.authenticationManager = authenticationManager;
        super.setAuthenticationManager(authenticationManager);
    }

    @Override
    public void afterPropertiesSet() {
        adapterDeploymentContextBean= applicationContext.getBean(AdapterDeploymentContextBean.class);
        super.afterPropertiesSet();
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException, IOException, ServletException {

        log.debug("Attempting Keycloak authentication");

        KeycloakDeployment deployment = adapterDeploymentContextBean.getDeployment();
        SimpleHttpFacade facade = new SimpleHttpFacade(request, response);
        SpringSecurityTokenStore tokenStore = new SpringSecurityTokenStore(deployment, request);
        SpringSecurityRequestAuthenticator authenticator
                = new SpringSecurityRequestAuthenticator(facade, request, deployment, tokenStore, -1);

        AuthOutcome result = authenticator.authenticate();
        AuthChallenge challenge = authenticator.getChallenge();

        log.debug("Auth outcome: {}", result);

        if (challenge != null) {
            challenge.challenge(facade);
        }

        if (AuthOutcome.AUTHENTICATED.equals(result)) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Assert.notNull(authentication, "Authentication SecurityContextHolder was null");
            return authenticationManager.authenticate(authentication);
        }

        return null;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
