package org.keycloak.adapters.springsecurity;

import java.io.FileNotFoundException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.keycloak.adapters.KeycloakConfigResolver;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.spi.HttpFacade;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import static org.junit.Assert.assertNotNull;

public class AdapterDeploymentContextFactoryBeanTest {
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private AdapterDeploymentContextFactoryBean adapterDeploymentContextFactoryBean;

    @Test
    public void should_create_adapter_deployment_context_from_configuration_file() throws Exception {
        // given:
        adapterDeploymentContextFactoryBean = new AdapterDeploymentContextFactoryBean(getCorrectResource());

        // when:
        adapterDeploymentContextFactoryBean.afterPropertiesSet();

        // then
        assertNotNull(adapterDeploymentContextFactoryBean.getObject());
    }

    private Resource getCorrectResource() {
        return new ClassPathResource("keycloak.json");
    }

    @Test
    public void should_throw_exception_when_configuration_file_was_not_found() throws Exception {
        // given:
        adapterDeploymentContextFactoryBean = new AdapterDeploymentContextFactoryBean(getEmptyResource());

        // then:
        expectedException.expect(FileNotFoundException.class);
        expectedException.expectMessage("Unable to locate Keycloak configuration file: no-file.json");

        // when:
        adapterDeploymentContextFactoryBean.afterPropertiesSet();
    }

    private Resource getEmptyResource() {
        return new ClassPathResource("no-file.json");
    }

    @Test
    public void should_create_adapter_deployment_context_from_keycloak_config_resolver() throws Exception {
        // given:
        adapterDeploymentContextFactoryBean = new AdapterDeploymentContextFactoryBean(getKeycloakConfigResolver());

        // when:
        adapterDeploymentContextFactoryBean.afterPropertiesSet();

        // then:
        assertNotNull(adapterDeploymentContextFactoryBean.getObject());
    }

    private KeycloakConfigResolver getKeycloakConfigResolver() {
        return new KeycloakConfigResolver() {
            @Override
            public KeycloakDeployment resolve(HttpFacade.Request facade) {
                return null;
            }
        };
    }
}
