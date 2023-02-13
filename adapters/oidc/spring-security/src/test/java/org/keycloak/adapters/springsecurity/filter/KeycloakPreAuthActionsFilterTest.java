package org.keycloak.adapters.springsecurity.filter;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.NodesRegistrationManagement;
import org.keycloak.adapters.PreAuthActionsHandler;
import org.keycloak.adapters.spi.HttpFacade;
import org.keycloak.adapters.spi.UserSessionManagement;
import org.keycloak.adapters.springsecurity.filter.KeycloakPreAuthActionsFilter.PreAuthActionsHandlerFactory;
import org.mockito.Mock;
import org.springframework.context.ApplicationContext;

public class KeycloakPreAuthActionsFilterTest {

    private KeycloakPreAuthActionsFilter filter;
    
    @Mock
    private NodesRegistrationManagement nodesRegistrationManagement;
    @Mock
    private ApplicationContext applicationContext;
    @Mock
    private AdapterDeploymentContext deploymentContext;
    @Mock
    private PreAuthActionsHandlerFactory preAuthActionsHandlerFactory;
    @Mock
    private UserSessionManagement userSessionManagement;
    @Mock
    private PreAuthActionsHandler preAuthActionsHandler;
    @Mock
    private KeycloakDeployment deployment;

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain chain;
    
    @Before
    public void setUp() throws Exception {
        initMocks(this);
        filter = new KeycloakPreAuthActionsFilter(userSessionManagement);
        filter.setNodesRegistrationManagement(nodesRegistrationManagement);
        filter.setApplicationContext(applicationContext);
        filter.setPreAuthActionsHandlerFactory(preAuthActionsHandlerFactory);
        when(applicationContext.getBean(AdapterDeploymentContext.class)).thenReturn(deploymentContext);
        when(deploymentContext.resolveDeployment(any(HttpFacade.class))).thenReturn(deployment);
        when(preAuthActionsHandlerFactory.createPreAuthActionsHandler(any(HttpFacade.class))).thenReturn(preAuthActionsHandler);
        when(deployment.isConfigured()).thenReturn(true);
        filter.initFilterBean();
    }
    
    @Test
    public void shouldIgnoreChainWhenPreAuthActionHandlerHandled() throws Exception {
        when(preAuthActionsHandler.handleRequest()).thenReturn(true);
        
        filter.doFilter(request, response, chain);
        
        verifyZeroInteractions(chain);
        verify(nodesRegistrationManagement).tryRegister(deployment);
    }
    
    @Test
    public void shouldContinueChainWhenPreAuthActionHandlerDidNotHandle() throws Exception {
        when(preAuthActionsHandler.handleRequest()).thenReturn(false);
        
        filter.doFilter(request, response, chain);
        
        verify(chain).doFilter(request, response);;
        verify(nodesRegistrationManagement).tryRegister(deployment);
    }
    
    @After
    public void tearDown() {
        filter.destroy();
        verify(nodesRegistrationManagement).stop();
    }
}
