/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.jgroups;

import org.infinispan.commons.configuration.attributes.Attribute;
import org.infinispan.configuration.global.TransportConfigurationBuilder;
import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.jgroups.protocols.TCP_NIO2;
import org.jgroups.protocols.UDP;

import static org.infinispan.configuration.global.TransportConfiguration.STACK;

public final class JGroupsUtil {

    private JGroupsUtil() {
    }

    public static TransportConfigurationBuilder transportOf(ConfigurationBuilderHolder holder) {
        return holder.getGlobalConfigurationBuilder().transport();
    }

    public static Attribute<String> transportStackOf(ConfigurationBuilderHolder holder) {
        var transport = transportOf(holder);
        assert transport != null;
        return transport.attributes().attribute(STACK);
    }

    public static void warnDeprecatedStack(ConfigurationBuilderHolder holder) {
        var stackName = transportStackOf(holder).get();
        switch (stackName) {
            case "jdbc-ping-udp":
            case "tcp":
            case "udp":
            case "azure":
            case "ec2":
            case "google":
                JGroupsConfigurator.logger.warnf("Stack '%s' is deprecated. We recommend to use 'jdbc-ping' instead", stackName);
        }
    }

    public static void validateTlsAvailable(ConfigurationBuilderHolder holder) {
        var stackName = transportStackOf(holder).get();
        if (stackName == null) {
            // unable to validate
            return;
        }
        var config = transportOf(holder).build();
        for (var protocol : config.transport().jgroups().configurator(stackName).getProtocolStack()) {
            var name = protocol.getProtocolName();
            if (name.equals(UDP.class.getSimpleName()) ||
                    name.equals(UDP.class.getName()) ||
                    name.equals(TCP_NIO2.class.getSimpleName()) ||
                    name.equals(TCP_NIO2.class.getName())) {
                throw new RuntimeException("Cache TLS is not available with protocol " + name);
            }
        }

    }

}
