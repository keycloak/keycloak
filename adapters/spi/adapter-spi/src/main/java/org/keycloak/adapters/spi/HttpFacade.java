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

package org.keycloak.adapters.spi;

import javax.security.cert.X509Certificate;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface HttpFacade {
    Request getRequest();

    Response getResponse();

    X509Certificate[] getCertificateChain();

    interface Request {

        String getMethod();
        /**
         * Full request URI with query params
         *
         * @return
         */
        String getURI();

        /**
         * Get the request relative path.
         *
         * @return the request relative path
         */
        String getRelativePath();

        /**
         * HTTPS?
         *
         * @return
         */
        boolean isSecure();

        /**
         * Get first query or form param
         *
         * @param param
         * @return
         */
        String getFirstParam(String param);
        String getQueryParamValue(String param);
        Cookie getCookie(String cookieName);
        String getHeader(String name);
        List<String> getHeaders(String name);
        InputStream getInputStream();
        InputStream getInputStream(boolean buffered);

        String getRemoteAddr();
        void setError(AuthenticationError error);
        void setError(LogoutError error);
    }

    interface Response {
        void setStatus(int status);
        void addHeader(String name, String value);
        void setHeader(String name, String value);
        void resetCookie(String name, String path);
        void setCookie(String name, String value, String path, String domain, int maxAge, boolean secure, boolean httpOnly);
        OutputStream getOutputStream();
        void sendError(int code);
        void sendError(int code, String message);

        /**
         * If the response is finished, end it.
         *
         */
        void end();
    }

    public class Cookie {
        protected String name;
        protected String value;
        protected int version;
        protected String domain;
        protected String path;

        public Cookie(String name, String value, int version, String domain, String path) {
            this.name = name;
            this.value = value;
            this.version = version;
            this.domain = domain;
            this.path = path;
        }

        public String getName() {
            return name;
        }

        public String getValue() {
            return value;
        }

        public int getVersion() {
            return version;
        }

        public String getDomain() {
            return domain;
        }

        public String getPath() {
            return path;
        }
    }
}
