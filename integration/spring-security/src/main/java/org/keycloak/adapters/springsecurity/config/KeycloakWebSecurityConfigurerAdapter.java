package org.keycloak.adapters.springsecurity.config;

import org.keycloak.adapters.springsecurity.AdapterDeploymentContextBean;
import org.keycloak.adapters.springsecurity.authentication.KeycloakAuthenticationEntryPoint;
import org.keycloak.adapters.springsecurity.authentication.KeycloakAuthenticationProvider;
import org.keycloak.adapters.springsecurity.authentication.KeycloakLogoutHandler;
import org.keycloak.adapters.springsecurity.filter.KeycloakAuthenticationProcessingFilter;
import org.keycloak.adapters.springsecurity.filter.KeycloakCsrfRequestMatcher;
import org.keycloak.adapters.springsecurity.filter.KeycloakPreAuthActionsFilter;
import org.keycloak.adapters.springsecurity.management.HttpSessionManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.security.config.annotation.web.WebSecurityConfigurer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.servlet.configuration.EnableWebMvcSecurity;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

/**
 * Provides a convenient base class for creating a {@link WebSecurityConfigurer}
 * instance secured by Keycloak. This implementation allows customization by overriding methods.
 *
 * @author <a href="mailto:srossillo@smartling.com">Scott Rossillo</a>
 * @version $Revision: 1 $
 * @see EnableWebSecurity
 * @see EnableWebMvcSecurity
 */
public abstract class KeycloakWebSecurityConfigurerAdapter extends WebSecurityConfigurerAdapter implements WebSecurityConfigurer<WebSecurity> {

    @Value("${keycloak.configurationFile:WEB-INF/keycloak.json}")
    private Resource keycloakConfigFileResource;

    @Bean
    protected AdapterDeploymentContextBean adapterDeploymentContextBean() {
        return new AdapterDeploymentContextBean(keycloakConfigFileResource);
    }

    protected AuthenticationEntryPoint authenticationEntryPoint() {
        return new KeycloakAuthenticationEntryPoint();
    }

    protected KeycloakAuthenticationProvider keycloakAuthenticationProvider() {
        return new KeycloakAuthenticationProvider();
    }

    @Bean
    protected KeycloakAuthenticationProcessingFilter keycloakAuthenticationProcessingFilter() throws Exception {
        KeycloakAuthenticationProcessingFilter filter = new KeycloakAuthenticationProcessingFilter(authenticationManagerBean());
        filter.setSessionAuthenticationStrategy(sessionAuthenticationStrategy());
        return filter;
    }

    @Bean
    protected KeycloakPreAuthActionsFilter keycloakPreAuthActionsFilter() {
        return new KeycloakPreAuthActionsFilter(httpSessionManager());
    }

    protected KeycloakCsrfRequestMatcher keycloakCsrfRequestMatcher() {
        return new KeycloakCsrfRequestMatcher();
    }

    @Bean
    protected HttpSessionManager httpSessionManager() {
        return new HttpSessionManager();
    }

    protected KeycloakLogoutHandler keycloakLogoutHandler() {
        return new KeycloakLogoutHandler(adapterDeploymentContextBean());
    }

    protected abstract SessionAuthenticationStrategy sessionAuthenticationStrategy();

    @Override
    protected void configure(HttpSecurity http) throws Exception {

        http
                .csrf().requireCsrfProtectionMatcher(keycloakCsrfRequestMatcher())
                .and()
                .sessionManagement()
                .sessionAuthenticationStrategy(sessionAuthenticationStrategy())
                .and()
                .addFilterBefore(keycloakPreAuthActionsFilter(), LogoutFilter.class)
                .addFilterBefore(keycloakAuthenticationProcessingFilter(), BasicAuthenticationFilter.class)
                .exceptionHandling().authenticationEntryPoint(authenticationEntryPoint())
                .and()
                .logout()
                .addLogoutHandler(keycloakLogoutHandler())
                .logoutUrl("/sso/logout").permitAll()
                .logoutSuccessUrl("/");
    }
}
