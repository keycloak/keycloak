/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.sessions.infinispan.remotestore;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.infinispan.client.hotrod.configuration.Configuration;
import org.infinispan.client.hotrod.configuration.ServerConfiguration;
import org.infinispan.client.hotrod.event.ClientListenerNotifier;
import org.infinispan.client.hotrod.impl.protocol.Codec;
import org.infinispan.client.hotrod.impl.transport.tcp.TcpTransportFactory;
import org.jboss.logging.Logger;
import org.keycloak.common.util.reflections.Reflections;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class KeycloakTcpTransportFactory extends TcpTransportFactory {

    protected static final Logger logger = Logger.getLogger(KeycloakTcpTransportFactory.class);

    private Collection<SocketAddress> kcInitialServers;

    @Override
    public void start(Codec codec, Configuration configuration, AtomicInteger defaultCacheTopologyId, ClientListenerNotifier listenerNotifier) {
        kcInitialServers = new HashSet<>();

        for (ServerConfiguration server : configuration.servers()) {
            InetSocketAddress hostnameAddress = new InetSocketAddress(server.host(), server.port());
            kcInitialServers.add(hostnameAddress);

            // Retrieve servers by IP addresses too, as we need to compare by IP addresses
            try {
                String ip = InetAddress.getByName(server.host()).getHostAddress();
                InetSocketAddress ipAddress = new InetSocketAddress(ip, server.port());
                kcInitialServers.add(ipAddress);

                InetSocketAddress unresolved = InetSocketAddress.createUnresolved(ip, server.port());
                kcInitialServers.add(unresolved);
            } catch (UnknownHostException uhe) {
                logger.warnf(uhe, "Wasn't able to retrieve IP address for host '%s'", server.host());
            }

        }

        logger.debugf("Keycloak initial servers: %s", kcInitialServers);

        super.start(codec, configuration, defaultCacheTopologyId, listenerNotifier);
    }


    @Override
    public void updateServers(Collection<SocketAddress> newServers, byte[] cacheName, boolean quiet) {
        try {
            logger.debugf("Update servers called: %s, cacheName: %s", newServers, new String(cacheName, "UTF-8"));

            Collection<SocketAddress> filteredServers = getFilteredNewServers(newServers);

            logger.debugf("Update servers after filter: %s, cacheName: %s", filteredServers, new String(cacheName, "UTF-8"));

            super.updateServers(filteredServers, cacheName, quiet);

        } catch (UnsupportedEncodingException uee) {
            throw new RuntimeException(uee);
        }
    }


    // Return just those servers, which are part of the originally configured "kcInitialServers".
    // Assume that the other JDG servers are part of same cluster, but are in different DC. Hence don't include them in the topology view
    private Collection<SocketAddress> getFilteredNewServers(Collection<SocketAddress> newServers) {
        Collection<SocketAddress> initialServers = getInitialServers();
        Collection<SocketAddress> filteredServers = newServers.stream().filter((SocketAddress newAddress) -> {

            boolean presentInInitialServers = initialServers.contains(newAddress);

            if (!presentInInitialServers) {
                logger.debugf("Server'%s' not present in initial servers. Probably server from different DC. Will filter it from the view", newAddress);
            }

            return presentInInitialServers;

        }).collect(Collectors.toList());

        return filteredServers;
    }


    protected Collection<SocketAddress> getInitialServers() {
       return kcInitialServers;
    }



}
