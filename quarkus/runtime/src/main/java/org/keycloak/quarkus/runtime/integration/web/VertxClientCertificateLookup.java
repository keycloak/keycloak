/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.quarkus.runtime.integration.web;

import java.security.cert.X509Certificate;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.services.x509.X509ClientCertificateLookup;

import io.vertx.ext.web.RoutingContext;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class VertxClientCertificateLookup implements X509ClientCertificateLookup {

    private static final Logger logger = Logger.getLogger(VertxClientCertificateLookup.class);

    public VertxClientCertificateLookup() {
    }

    @Override
    public void close() {

    }

    @Override
    public X509Certificate[] getCertificateChain(HttpRequest httpRequest) {
        Instance<RoutingContext> instances = CDI.current().select(RoutingContext.class);

        if (instances.isResolvable()) {
            RoutingContext context = instances.get();

            try {
                SSLSession sslSession = context.request().sslSession();
                
                if (sslSession == null) {
                    return null;
                }
                
                X509Certificate[] certificates = (X509Certificate[]) sslSession.getPeerCertificates();

                if (logger.isTraceEnabled() && certificates != null) {
                    for (X509Certificate cert : certificates) {
                        logger.tracef("Certificate's SubjectDN => \"%s\"", cert.getSubjectDN().getName());
                    }
                }

                return certificates;
            } catch (SSLPeerUnverifiedException ignore) {
                // client not authenticated
            }
        }

        return null;
    }
}
