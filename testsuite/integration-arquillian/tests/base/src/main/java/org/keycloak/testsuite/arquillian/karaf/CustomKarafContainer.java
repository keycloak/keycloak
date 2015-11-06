package org.keycloak.testsuite.arquillian.karaf;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import org.jboss.arquillian.container.osgi.jmx.ObjectNameFactory;
import org.jboss.arquillian.container.osgi.karaf.managed.KarafManagedDeployableContainer;
import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.logging.Logger;

/**
 *
 * @author tkyjovsk
 */
public class CustomKarafContainer extends KarafManagedDeployableContainer<CustomKarafContainerConfiguration> {

    protected final Logger log = Logger.getLogger(this.getClass());

    private CustomKarafContainerConfiguration config;

    protected MBeanServerConnection mbeanServer = null;
    protected ObjectName feature;

    @Override
    public void start() throws LifecycleException {
        super.start();
        executePostStartCommands();
    }

    @Override
    public void setup(CustomKarafContainerConfiguration config) {
        super.setup(config);
        this.config = config;
    }

    @Override
    public Class<CustomKarafContainerConfiguration> getConfigurationClass() {
        return CustomKarafContainerConfiguration.class;
    }

    protected void executePostStartCommands() throws LifecycleException {
        try {
            mbeanServer = getMBeanServerConnection(500, TimeUnit.MILLISECONDS);
        } catch (TimeoutException ex) {
            throw new LifecycleException("JMX connection timed out.");
        }

        try {

            feature = ObjectNameFactory.create("org.apache.karaf:type=feature,name=root");
            try {
                mbeanServer.getObjectInstance(feature);
            } catch (InstanceNotFoundException infe) {
                try {
                    feature = ObjectNameFactory.create("org.apache.karaf:type=features,name=root");
                    mbeanServer.getObjectInstance(feature);
                } catch (InstanceNotFoundException infe2) {
                    throw new RuntimeException("Feature MBean not found on server.");
                }
            }

            featureMBean = getMBeanProxy(mbeanServer, feature, FeatureMBean.class, 30, TimeUnit.SECONDS);

            log.info("Executing karaf after-start commands");
            for (String command : config.getCommandsAfterStartAsArray()) {
                String cmd = command.trim().split(" ")[0].trim();
                String param = command.trim().split(" ")[1].trim();
                log.info(String.format("command: %s, param: %s", cmd, param));
                if (cmd.equals("feature:repo-add") || cmd.equals("features:addurl")) {
                    featureMBean.addRepository(param);
                } else if (cmd.equals("feature:repo-remove") || cmd.equals("features:removeurl")) {
                    featureMBean.removeRepository(param);
                } else if (cmd.equals("feature:install") || cmd.equals("features:install")) {
                    featureMBean.installFeature(param);
                } else if (cmd.equals("feature:uninstall") || cmd.equals("features:uninstall")) {
                    featureMBean.uninstallFeature(param);
                } else {
                    throw new RuntimeException(String.format("Unsupported command: '%s'. "
                            + "Supported commands on Karaf: 'feature:repo-add', 'feature:install'\n"
                            + "Supported commands on Fuse: 'features:addurl', 'features:install'", cmd));
                }
            }
        } catch (IOException | RuntimeException | TimeoutException ex) {
            stop();
            throw new LifecycleException("Error when executing karaf post-start commands.", ex);
        }
    }

    FeatureMBean featureMBean;

    public interface FeatureMBean {

        public void addRepository(String repository);

        public void removeRepository(String repository);

        public void installFeature(String feature);

        public void uninstallFeature(String feature);
    }

}
