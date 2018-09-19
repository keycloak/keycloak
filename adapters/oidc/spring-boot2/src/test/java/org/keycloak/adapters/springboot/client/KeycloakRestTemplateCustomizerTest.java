package org.keycloak.adapters.springboot.client;

import org.junit.Before;
import org.junit.Test;
import org.springframework.web.client.RestTemplate;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class KeycloakRestTemplateCustomizerTest {

    private KeycloakRestTemplateCustomizer customizer;
    private KeycloakSecurityContextClientRequestInterceptor interceptor =
            mock(KeycloakSecurityContextClientRequestInterceptor.class);

    @Before
    public void setup() {
        customizer = new KeycloakRestTemplateCustomizer(interceptor);
    }

    @Test
    public void interceptorIsAddedToRequest() {
        RestTemplate restTemplate = new RestTemplate();
        customizer.customize(restTemplate);
        assertTrue(restTemplate.getInterceptors().contains(interceptor));
    }

}
