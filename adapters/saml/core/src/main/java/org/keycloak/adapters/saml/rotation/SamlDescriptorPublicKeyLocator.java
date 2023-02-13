/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.adapters.saml.rotation;

import java.security.Key;
import java.security.KeyManagementException;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyName;
import org.apache.http.client.HttpClient;
import org.jboss.logging.Logger;
import org.keycloak.adapters.cloned.HttpAdapterUtils;
import org.keycloak.adapters.cloned.HttpClientAdapterException;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.common.util.Time;
import org.keycloak.dom.saml.v2.metadata.KeyTypes;
import org.keycloak.rotation.KeyLocator;
import org.keycloak.saml.processing.api.util.KeyInfoTools;
import java.security.cert.CertificateException;
import java.util.UUID;
import javax.security.auth.x500.X500Principal;

/**
 * This class defines a {@link KeyLocator} that looks up public keys and certificates in IdP's
 * SAML descriptor (i.e. http://{host}/auth/realms/{realm}/protocol/saml/descriptor).
 *
 * Based on {@code JWKPublicKeyLocator}.
 *
 * @author hmlnarik
 */
public class SamlDescriptorPublicKeyLocator implements KeyLocator, Iterable<PublicKey> {

    private static final Logger LOG = Logger.getLogger(SamlDescriptorPublicKeyLocator.class);

    /**
     * Time between two subsequent requests (in seconds).
     */
    private final int minTimeBetweenDescriptorRequests;

    /**
     * Time to live for cache entries (in seconds).
     */
    private final int cacheEntryTtl;

    /**
     * Target descriptor URL.
     */
    private final String descriptorUrl;

    private final Map<String, PublicKey> publicKeyCache = new ConcurrentHashMap<>();

    private final HttpClient client;

    private volatile int lastRequestTime = 0;

    public SamlDescriptorPublicKeyLocator(String descriptorUrl, int minTimeBetweenDescriptorRequests, int cacheEntryTtl, HttpClient httpClient) {
        this.minTimeBetweenDescriptorRequests = minTimeBetweenDescriptorRequests <= 0
          ? 20
          : minTimeBetweenDescriptorRequests;

        this.descriptorUrl = descriptorUrl;
        this.cacheEntryTtl = cacheEntryTtl;

        this.client = httpClient;
    }

    @Override
    public Key getKey(String kid) throws KeyManagementException {
        if (kid == null) {
            LOG.debugf("Invalid key id: %s", kid);
            return null;
        }

        LOG.tracef("Requested key id: %s", kid);

        int currentTime = Time.currentTime();

        PublicKey res;
        if (currentTime > this.lastRequestTime + this.cacheEntryTtl) {
            LOG.debugf("Performing regular cache cleanup.");
            res = refreshCertificateCacheAndGet(kid);
        } else {
            res = publicKeyCache.get(kid);

            if (res == null) {
                if (currentTime > this.lastRequestTime + this.minTimeBetweenDescriptorRequests) {
                    res = refreshCertificateCacheAndGet(kid);
                } else {
                    LOG.debugf("Won't send request to realm SAML descriptor url, timeout not expired. Last request time was %d", lastRequestTime);
                }
            }
        }

        return res;
    }

    @Override
    public synchronized void refreshKeyCache() {
        LOG.info("Forcing key cache cleanup and refresh.");
        this.publicKeyCache.clear();
        refreshCertificateCacheAndGet(null);
    }

    private synchronized PublicKey refreshCertificateCacheAndGet(String kid) {
        if (this.descriptorUrl == null) {
            return null;
        }

        this.lastRequestTime = Time.currentTime();

        LOG.debugf("Refreshing public key cache from %s", this.descriptorUrl);
        List<KeyInfo> signingCerts;
        try {
            MultivaluedHashMap<String, KeyInfo> certs = HttpAdapterUtils.downloadKeysFromSamlDescriptor(client, this.descriptorUrl);
            signingCerts = certs.get(KeyTypes.SIGNING.value());
        } catch (HttpClientAdapterException ex) {
            LOG.error("Could not refresh certificates from the server", ex);
            return null;
        }

        if (signingCerts == null) {
            return null;
        }

        LOG.debugf("Certificates retrieved from server, filling public key cache");

        // Only clear cache after it is certain that the SAML descriptor has been read successfully
        this.publicKeyCache.clear();

        for (KeyInfo ki : signingCerts) {
            KeyName keyName = KeyInfoTools.getKeyName(ki);
            X509Certificate x509certificate = KeyInfoTools.getX509Certificate(ki);
            if (x509certificate == null) {
                continue;
            }
            try {
                x509certificate.checkValidity();
            } catch (CertificateException ex) {
                continue;
            }

            if (keyName != null) {
                LOG.tracef("Registering signing certificate %s", keyName.getName());
                this.publicKeyCache.put(keyName.getName(), x509certificate.getPublicKey());
            } else {
                final X500Principal principal = x509certificate.getSubjectX500Principal();
                String name = (principal == null ? "unnamed" : principal.getName())
                  + "@" + x509certificate.getSerialNumber() + "$" + UUID.randomUUID();
                this.publicKeyCache.put(name, x509certificate.getPublicKey());
                LOG.tracef("Adding certificate %s without a specific key name: %s", name, x509certificate);
            }
        }

        return (kid == null ? null : this.publicKeyCache.get(kid));
    }

    @Override
    public String toString() {
        return "Keys retrieved from SAML descriptor at " + descriptorUrl;
    }

    @Override
    public Iterator<PublicKey> iterator() {
        if (this.publicKeyCache.isEmpty()) {
            refreshCertificateCacheAndGet(null);
        }

        return this.publicKeyCache.values().iterator();
    }
}
