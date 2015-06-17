package org.keycloak.testsuite.arquillian;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Before;

/**
 *
 * @author tkyjovsk
 */
public abstract class ContainersController {

    @ArquillianResource
    protected ContainerController controller;

    private static final Logger log = Logger.getLogger(ContainersController.class.getName());

    private Class<? extends ContainerController> getNearestSuperclassWithCCAnnotation(Class clazz) { // TODO review
        log.log(Level.FINE, "Looking for annotation @ControlsContainers at {0}", clazz);
        return clazz.isAnnotationPresent(ControlsContainers.class) ? clazz
                : (clazz.equals(Object.class) || clazz.equals(ContainerController.class) ? null
                        : getNearestSuperclassWithCCAnnotation(clazz.getSuperclass()));
    }

    @Before
    public void startContainers() {

        Class<? extends ContainerController> clazz = getNearestSuperclassWithCCAnnotation(this.getClass());

        if (clazz != null) {
            ControlsContainers containers = clazz.getAnnotation(ControlsContainers.class);
            for (String qualifier : containers.value()) {
                log.log(Level.FINE, "Starting container: {0}", qualifier);
                controller.start(qualifier);
            }
        }
    }

}
