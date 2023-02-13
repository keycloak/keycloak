package org.keycloak.testsuite.utils.arquillian.tomcat;

import org.jboss.shrinkwrap.descriptor.spi.node.Node;

public class TomcatAppServerConfigurationUtils {

    private static void createChild(Node configuration, String name, String text) {
        configuration.createChild("property").attribute("name", name).text(text);
    }

    /**
     * Original XSL transformation
     * <p>
     * <container qualifier="app-server-${{app.server}}" mode="manual" >
     * <configuration>
     * <property name="enabled">true</property>
     * <property name="adapterImplClass">org.jboss.arquillian.container.tomcat.managed_7.TomcatManagedContainer</property>
     * <property name="catalinaHome">${app.server.home}</property>
     * <property name="catalinaBase">${app.server.home}</property>
     * <property name="bindHttpPort">${app.server.http.port}</property>
     * <property name="jmxPort">${app.server.management.port}</property>
     * <property name="user">manager</property>
     * <property name="pass">arquillian</property>
     * <property name="javaVmArguments">${adapter.test.props}</property>
     * </configuration>
     * </container>
     *
     * @return arquillian configuration for tomcat container
     */
    public static Node getStandaloneConfiguration(Node container, String adapterImplClass,
                                             String catalinaHome, String bindHttpPort, String jmxPort,
                                             String user, String pass, String startupTimeoutInSeconds) {
        Node configuration = container.createChild("configuration");
        createChild(configuration, "enabled", "true");
        createChild(configuration, "adapterImplClass", adapterImplClass);
        createChild(configuration, "catalinaHome", catalinaHome);
        createChild(configuration, "catalinaBase", catalinaHome);
        createChild(configuration, "bindHttpPort", bindHttpPort);
        createChild(configuration, "jmxPort", jmxPort);
        createChild(configuration, "user", user);
        createChild(configuration, "pass", pass);
        createChild(configuration, "javaVmArguments",
                        System.getProperty("adapter.test.props", " ") + " " +
                        System.getProperty("app.server.jboss.jvm.debug.args", " ") + " " +
                        System.getProperty("tomcat.javax.net.ssl.properties", " "));
        createChild(configuration,"startupTimeoutInSeconds", startupTimeoutInSeconds);

        return container;
    }
}
