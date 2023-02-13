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

package org.keycloak.adapters.cloned;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;

import java.io.IOException;
import java.io.InputStream;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.keycloak.adapters.saml.descriptor.parsers.SamlDescriptorIDPKeysExtractor;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.saml.common.exceptions.ParsingException;

/**
 * @author <a href="mailto:hmlnarik@redhat.com">Hynek Mlnařík</a>
 */
public class HttpAdapterUtils {

    public static MultivaluedHashMap<String, KeyInfo> downloadKeysFromSamlDescriptor(HttpClient client, String descriptorUrl) throws HttpClientAdapterException {
        try {
            HttpGet httpRequest = new HttpGet(descriptorUrl);
            HttpResponse response = client.execute(httpRequest);
            int status = response.getStatusLine().getStatusCode();
            if (status != HttpStatus.SC_OK) {
                EntityUtils.consumeQuietly(response.getEntity());
                throw new HttpClientAdapterException("Unexpected status = " + status);
            }

            HttpEntity entity = response.getEntity();
            if (entity == null) {
                throw new HttpClientAdapterException("There was no entity.");
            }

            MultivaluedHashMap<String, KeyInfo> res;
            try (InputStream is = entity.getContent()) {
                res = extractKeysFromSamlDescriptor(is);
            }

            EntityUtils.consumeQuietly(entity);

            return res;
        } catch (IOException | ParsingException e) {
            throw new HttpClientAdapterException("IO error", e);
        }
    }

    /**
     * Parses SAML descriptor and extracts keys from it.
     * @param xmlStream
     * @return List of KeyInfo objects containing keys from the descriptor.
     * @throws IOException
     */
    public static MultivaluedHashMap<String, KeyInfo> extractKeysFromSamlDescriptor(InputStream xmlStream) throws ParsingException {
        Object res = new SamlDescriptorIDPKeysExtractor().parse(xmlStream);
        return (MultivaluedHashMap<String, KeyInfo>) res;
    }

}
