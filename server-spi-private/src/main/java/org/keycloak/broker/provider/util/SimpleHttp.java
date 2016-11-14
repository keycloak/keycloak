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

package org.keycloak.broker.provider.util;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class SimpleHttp {


    private String url;
    private String method;
    private Map<String, String> headers;
    private Map<String, String> params;

    private SSLSocketFactory sslFactory;
    private HostnameVerifier hostnameVerifier;

    protected SimpleHttp(String url, String method) {
        this.url = url;
        this.method = method;
    }

    public static SimpleHttp doGet(String url) {
        return new SimpleHttp(url, "GET");
    }

    public static SimpleHttp doPost(String url) {
        return new SimpleHttp(url, "POST");
    }

    public SimpleHttp header(String name, String value) {
        if (headers == null) {
            headers = new HashMap<String, String>();
        }
        headers.put(name, value);
        return this;
    }

    public SimpleHttp param(String name, String value) {
        if (params == null) {
            params = new HashMap<String, String>();
        }
        params.put(name, value);
        return this;
    }

    public SimpleHttp sslFactory(SSLSocketFactory factory) {
        sslFactory = factory;
        return this;
    }

    public SimpleHttp hostnameVerifier(HostnameVerifier verifier) {
        hostnameVerifier = verifier;
        return this;
    }

    public String asString() throws IOException {
        boolean get = method.equals("GET");
        boolean post = method.equals("POST");

        StringBuilder sb = new StringBuilder();
        if (get) {
            sb.append(url);
        }

        if (params != null) {
            boolean f = true;
            for (Map.Entry<String, String> p : params.entrySet()) {
                if (f) {
                    f = false;
                    if (get) {
                        sb.append("?");
                    }
                } else {
                    sb.append("&");
                }
                sb.append(URLEncoder.encode(p.getKey(), "UTF-8"));
                sb.append("=");
                sb.append(URLEncoder.encode(p.getValue(), "UTF-8"));
            }
        }

        if (get) {
            url = sb.toString();
        }

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        setupTruststoreIfApplicable(connection);
        OutputStream os = null;
        InputStream is = null;

        try {
            connection.setRequestMethod(method);

            if (headers != null) {
                for (Map.Entry<String, String> h : headers.entrySet()) {
                    connection.setRequestProperty(h.getKey(), h.getValue());
                }
            }

            if (post) {
                String data = sb.toString();

                connection.setDoOutput(true);
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                connection.setRequestProperty("Content-Length", String.valueOf(data.length()));

                os = connection.getOutputStream();
                os.write(data.getBytes());
            } else {
                connection.setDoOutput(false);
            }

            String ce = connection.getHeaderField("Content-Encoding");
            is = connection.getInputStream();
            if ("gzip".equals(ce)) {
              is = new GZIPInputStream(is);
	          }
            return toString(is);
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                }
            }

            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                }
            }
            if (connection != null) {
                try {
                    connection.disconnect();
                } catch (Exception e) {
                }
            }
        }
    }

    public int asStatus() throws IOException {
        boolean get = method.equals("GET");
        boolean post = method.equals("POST");

        StringBuilder sb = new StringBuilder();
        if (get) {
            sb.append(url);
        }

        if (params != null) {
            boolean f = true;
            for (Map.Entry<String, String> p : params.entrySet()) {
                if (f) {
                    f = false;
                    if (get) {
                        sb.append("?");
                    }
                } else {
                    sb.append("&");
                }
                sb.append(URLEncoder.encode(p.getKey(), "UTF-8"));
                sb.append("=");
                sb.append(URLEncoder.encode(p.getValue(), "UTF-8"));
            }
        }

        if (get) {
            url = sb.toString();
        }

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        setupTruststoreIfApplicable(connection);
        OutputStream os = null;
        InputStream is = null;

        try {
            connection.setRequestMethod(method);

            if (headers != null) {
                for (Map.Entry<String, String> h : headers.entrySet()) {
                    connection.setRequestProperty(h.getKey(), h.getValue());
                }
            }

            if (post) {
                String data = sb.toString();

                connection.setDoOutput(true);
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                connection.setRequestProperty("Content-Length", String.valueOf(data.length()));

                os = connection.getOutputStream();
                os.write(data.getBytes());
            } else {
                connection.setDoOutput(false);
            }

            is = connection.getInputStream();
            return connection.getResponseCode();
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                }
            }

            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                }
            }
            if (connection != null) {
                try {
                    connection.disconnect();
                } catch (Exception e) {
                }
            }
        }
    }

    private String toString(InputStream is) throws IOException {
        InputStreamReader reader = new InputStreamReader(is);

        StringWriter writer = new StringWriter();

        char[] buffer = new char[1024 * 4];
        for (int n = reader.read(buffer); n != -1; n = reader.read(buffer)) {
            writer.write(buffer, 0, n);
        }

        return writer.toString();
    }

    private void setupTruststoreIfApplicable(HttpURLConnection connection) {
        if (connection instanceof HttpsURLConnection && sslFactory != null) {
            HttpsURLConnection con = (HttpsURLConnection) connection;
            con.setSSLSocketFactory(sslFactory);
            if (hostnameVerifier != null) {
                con.setHostnameVerifier(hostnameVerifier);
            }
        }
    }
}
