jboss-client.jar is a combined client jar for WildFly, for use in non-maven environments. This jar should be used
with standalone clients only, not with deployments are that deployed to a WildFly instance.

This jar contains the classes required for remote JMS and EJB usage, and consists of the following shaded artifacts:

org.jboss.spec.javax.ejb:jboss-ejb-api_3.2_spec
org.jboss.spec.javax.jms:jboss-jms-api_2.0_spec
org.jboss.spec.javax.transaction:jboss-transaction-api_1.2_spec

com.google.guava:guava
commons-beanutils:commons-beanutils
commons-collections:commons-collections
io.netty:netty-all
org.apache.activemq:artemis-commons
org.apache.activemq:artemis-core-client
org.apache.activemq:artemis-hqclient-protocol
org.apache.activemq:artemis-jms-client
org.jboss:jboss-ejb-client
org.jboss:jboss-remote-naming
org.jboss.logging:jboss-logging
org.jboss.marshalling:jboss-marshalling
org.jboss.marshalling:jboss-marshalling-river
org.jboss.remoting:jboss-remoting
org.jboss.remotingjmx:remoting-jmx
org.jboss.sasl:jboss-sasl
org.jboss.xnio:xnio-api
org.jboss.xnio:xnio-nio
org.jgroups:jgroups
org.slf4j:slf4j-api
org.slf4j:jcl-over-slf4j


Maven users should not use this jar, but should use the following BOM dependencies instead

    <dependencies>
        <dependency>
            <groupId>org.wildfly</groupId>
            <artifactId>wildfly-ejb-client-bom</artifactId>
            <type>pom</type>
        </dependency>
        <dependency>
            <groupId>org.wildfly</groupId>
            <artifactId>wildfly-jms-client-bom</artifactId>
            <type>pom</type>
        </dependency>
    </dependencies>

This is because using maven with a shaded jar has a very high chance of causing class version conflicts, which is why
we do not publish this jar to the maven repository.
