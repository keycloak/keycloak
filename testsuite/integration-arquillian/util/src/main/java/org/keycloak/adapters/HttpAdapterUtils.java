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

package org.keycloak.adapters;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class HttpAdapterUtils {


    public static <T> T sendJsonHttpRequest(KeycloakDeployment deployment, HttpRequestBase httpRequest, Class<T> clazz) throws HttpClientAdapterException {
        try {
            HttpResponse response = deployment.getClient().execute(httpRequest);
            int status = response.getStatusLine().getStatusCode();
            if (status != 200) {
                close(response);
                throw new HttpClientAdapterException("Unexpected status = " + status);
            }
            HttpEntity entity = response.getEntity();
            if (entity == null) {
                throw new HttpClientAdapterException("There was no entity.");
            }
            InputStream is = entity.getContent();
            try {
                return JsonSerialization.readValue(is, clazz);
            } finally {
                try {
                    is.close();
                } catch (IOException ignored) {

                }
            }
        } catch (IOException e) {
            throw new HttpClientAdapterException("IO error", e);
        }
    }


    private static void close(HttpResponse response) {
        if (response.getEntity() != null) {
            try {
                response.getEntity().getContent().close();
            } catch (IOException e) {

            }
        }
    }
}
