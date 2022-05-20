package org.keycloak.testsuite.arquillian;

import org.jboss.arquillian.container.spi.event.StartContainer;
import org.jboss.arquillian.container.spi.event.StopContainer;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.core.api.Event;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.core.spi.Validate;
import org.jboss.arquillian.test.spi.event.suite.AfterSuite;
import org.jboss.logging.Logger;
import org.jboss.arquillian.container.spi.event.StartSuiteContainers;


public class HotRodStoreTestEnricher {

    private static SuiteContext suiteContext;
    private static final Logger log = Logger.getLogger(HotRodStoreTestEnricher.class);

    @Inject
    private Event<StartContainer> startContainerEvent;

    @Inject
    private Event<StopContainer> stopContainerEvent;

    static void initializeSuiteContext(SuiteContext suiteContext) {
        Validate.notNull(suiteContext, "Suite context cannot be null.");
        HotRodStoreTestEnricher.suiteContext = suiteContext;
    }

    public void beforeContainerStarted(@Observes(precedence = 1) StartSuiteContainers event) {
        if (!AuthServerTestEnricher.HOT_ROD_STORE_ENABLED) return;

        ContainerInfo hotRodContainer = suiteContext.getHotRodStoreInfo();

        if (hotRodContainer != null && !hotRodContainer.isStarted()) {
            log.infof("HotRod store starting: %s", hotRodContainer.getQualifier());
            startContainerEvent.fire(new StartContainer(hotRodContainer.getArquillianContainer()));
        }
    }

    public void afterSuite(@Observes(precedence = 4) AfterSuite event) {
        if (!AuthServerTestEnricher.HOT_ROD_STORE_ENABLED) return;

        ContainerInfo hotRodContainer = suiteContext.getHotRodStoreInfo();

        if (hotRodContainer != null && hotRodContainer.isStarted()) {
            log.infof("HotRod store stopping: %s", hotRodContainer.getQualifier());
            stopContainerEvent.fire(new StopContainer(hotRodContainer.getArquillianContainer()));
        }
    }
}
