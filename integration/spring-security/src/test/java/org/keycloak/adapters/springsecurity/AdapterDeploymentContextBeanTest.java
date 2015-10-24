package org.keycloak.adapters.springsecurity;


import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.FileNotFoundException;

import static org.junit.Assert.assertNotNull;

public class AdapterDeploymentContextBeanTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private AdapterDeploymentContextBean adapterDeploymentContextBean;

    @Test
    public void should_create_deployment_and_deployment_context() throws Exception {

        //given:
        adapterDeploymentContextBean = new AdapterDeploymentContextBean(getCorrectResource());

        //when:
        adapterDeploymentContextBean.afterPropertiesSet();

        //then
        assertNotNull(adapterDeploymentContextBean.getDeployment());
        assertNotNull(adapterDeploymentContextBean.getDeploymentContext());
    }

    private Resource getCorrectResource() {
        return new ClassPathResource("keycloak.json");
    }

    @Test
    public void should_throw_exception_when_configuration_file_was_not_found() throws Exception {

        //given:
        adapterDeploymentContextBean = new AdapterDeploymentContextBean(getEmptyResource());

        //then:
        expectedException.expect(FileNotFoundException.class);
        expectedException.expectMessage("Unable to locate Keycloak configuration file: no-file.json");

        //when:
        adapterDeploymentContextBean.afterPropertiesSet();
    }

    private Resource getEmptyResource() {
        return new ClassPathResource("no-file.json");
    }
}
