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

package org.keycloak.adapters.servlet;

import org.keycloak.adapters.spi.AdapterSessionStore;
import org.keycloak.adapters.spi.HttpFacade;
import org.keycloak.adapters.spi.KeycloakAccount;
import org.keycloak.common.util.Encode;
import org.keycloak.common.util.MultivaluedHashMap;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.Principal;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class FilterSessionStore implements AdapterSessionStore {
    public static final String REDIRECT_URI = "__REDIRECT_URI";
    public static final String SAVED_METHOD = "__SAVED_METHOD";
    public static final String SAVED_HEADERS = "__SAVED_HEADERS";
    public static final String SAVED_BODY = "__SAVED_BODY";
    protected final HttpServletRequest request;
    protected final HttpFacade facade;
    protected final int maxBuffer;
    protected byte[] restoredBuffer = null;
    protected boolean needRequestRestore;

    public FilterSessionStore(HttpServletRequest request, HttpFacade facade, int maxBuffer) {
        this.request = request;
        this.facade = facade;
        this.maxBuffer = maxBuffer;
    }

    public void clearSavedRequest(HttpSession session) {
        session.removeAttribute(REDIRECT_URI);
        session.removeAttribute(SAVED_METHOD);
        session.removeAttribute(SAVED_HEADERS);
        session.removeAttribute(SAVED_BODY);
    }

    public void servletRequestLogout() {

    }

    public static String getCharsetFromContentType(String contentType) {

        if (contentType == null)
            return (null);
        int start = contentType.indexOf("charset=");
        if (start < 0)
            return (null);
        String encoding = contentType.substring(start + 8);
        int end = encoding.indexOf(';');
        if (end >= 0)
            encoding = encoding.substring(0, end);
        encoding = encoding.trim();
        if ((encoding.length() > 2) && (encoding.startsWith("\""))
                && (encoding.endsWith("\"")))
            encoding = encoding.substring(1, encoding.length() - 1);
        return (encoding.trim());

    }


    public HttpServletRequestWrapper buildWrapper(HttpSession session, final KeycloakAccount account) {
        if (needRequestRestore) {
            final String method = (String)session.getAttribute(SAVED_METHOD);
            final byte[] body = (byte[])session.getAttribute(SAVED_BODY);
            final MultivaluedHashMap<String, String> headers = (MultivaluedHashMap<String, String>)session.getAttribute(SAVED_HEADERS);
            clearSavedRequest(session);
            HttpServletRequestWrapper wrapper = new HttpServletRequestWrapper(request) {
                protected MultivaluedHashMap<String, String> parameters;

                MultivaluedHashMap<String, String> getParams() {
                    if (parameters != null) return parameters;

                    if (body == null) return new MultivaluedHashMap<String, String>();

                    String contentType = getContentType();
                    if (contentType != null && contentType.toLowerCase().startsWith("application/x-www-form-urlencoded")) {
                        ByteArrayInputStream is = new ByteArrayInputStream(body);
                        try {
                            parameters = parseForm(is);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    return parameters;

                }
                @Override
                public boolean isUserInRole(String role) {
                    return account.getRoles().contains(role);
                }

                @Override
                public Principal getUserPrincipal() {
                    return account.getPrincipal();
                }

                @Override
                public String getMethod() {
                    if (needRequestRestore) {
                        return method;
                    } else {
                        return super.getMethod();

                    }
                }

                @Override
                public String getHeader(String name) {
                    if (needRequestRestore && headers != null) {
                        return headers.getFirst(name.toLowerCase());
                    }
                    return super.getHeader(name);
                }

                @Override
                public Enumeration<String> getHeaders(String name) {
                    if (needRequestRestore && headers != null) {
                        List<String> values = headers.getList(name.toLowerCase());
                        if (values == null) return Collections.emptyEnumeration();
                        else return Collections.enumeration(values);
                    }
                    return super.getHeaders(name);
                }

                @Override
                public Enumeration<String> getHeaderNames() {
                    if (needRequestRestore && headers != null) {
                        return Collections.enumeration(headers.keySet());
                    }
                    return super.getHeaderNames();
                }

                @Override
                public ServletInputStream getInputStream() throws IOException {

                    if (needRequestRestore && body != null) {
                        final ByteArrayInputStream is = new ByteArrayInputStream(body);
                        return new ServletInputStream() {
                            @Override
                            public int read() throws IOException {
                                return is.read();
                            }
                        };
                    }
                    return super.getInputStream();
                }

                @Override
                public void logout() throws ServletException {
                    servletRequestLogout();
                }

                @Override
                public long getDateHeader(String name) {
                   if (!needRequestRestore) return super.getDateHeader(name);
                   return -1;
                }

                @Override
                public int getIntHeader(String name) {
                    if (!needRequestRestore) return super.getIntHeader(name);
                    String value = getHeader(name);
                    if (value == null) return -1;
                    return Integer.valueOf(value);

                }

                @Override
                public String[] getParameterValues(String name) {
                    if (!needRequestRestore) return super.getParameterValues(name);
                    MultivaluedHashMap<String, String> formParams = getParams();
                    if (formParams == null) {
                        return super.getParameterValues(name);
                    }
                    String[] values = request.getParameterValues(name);
                    List<String> list = new LinkedList<>();
                    if (values != null) {
                        for (String val : values) list.add(val);
                    }
                    List<String> vals = formParams.get(name);
                    if (vals != null) list.addAll(vals);
                    return list.toArray(new String[list.size()]);
                }

                @Override
                public Enumeration<String> getParameterNames() {
                    if (!needRequestRestore) return super.getParameterNames();
                    MultivaluedHashMap<String, String> formParams = getParams();
                    if (formParams == null) {
                        return super.getParameterNames();
                    }
                    Set<String> names = new HashSet<>();
                    Enumeration<String> qnames = super.getParameterNames();
                    while (qnames.hasMoreElements()) names.add(qnames.nextElement());
                    names.addAll(formParams.keySet());
                    return Collections.enumeration(names);

                }

                @Override
                public Map<String, String[]> getParameterMap() {
                    if (!needRequestRestore) return super.getParameterMap();
                    MultivaluedHashMap<String, String> formParams = getParams();
                    if (formParams == null) {
                        return super.getParameterMap();
                    }
                    Map<String, String[]> map = new HashMap<>();
                    Enumeration<String> names = getParameterNames();
                    while (names.hasMoreElements()) {
                        String name = names.nextElement();
                        String[] values = getParameterValues(name);
                        if (values != null) {
                            map.put(name, values);
                        }
                    }
                    return map;
                }

                @Override
                public String getParameter(String name) {
                    if (!needRequestRestore) return super.getParameter(name);
                    String param = super.getParameter(name);
                    if (param != null) return param;
                    MultivaluedHashMap<String, String> formParams = getParams();
                    if (formParams == null) {
                        return null;
                    }
                    return formParams.getFirst(name);

                }

                @Override
                public BufferedReader getReader() throws IOException {
                    if (!needRequestRestore) return super.getReader();
                    return new BufferedReader(new InputStreamReader(getInputStream()));
                }

                @Override
                public int getContentLength() {
                    if (!needRequestRestore) return super.getContentLength();
                    String header = getHeader("content-length");
                    if (header == null) return -1;
                    return Integer.valueOf(header);
                }

                @Override
                public String getContentType() {
                    if (!needRequestRestore) return super.getContentType();
                    return getHeader("content-type");
                }

                @Override
                public String getCharacterEncoding() {
                    if (!needRequestRestore) return super.getCharacterEncoding();
                    return getCharsetFromContentType(getContentType());
                }

            };
            return wrapper;
        } else {
            return new HttpServletRequestWrapper(request) {
                @Override
                public boolean isUserInRole(String role) {
                    return account.getRoles().contains(role);
                }

                @Override
                public Principal getUserPrincipal() {
                    if (account == null) return null;
                    return account.getPrincipal();
                }

                @Override
                public void logout() throws ServletException {
                    servletRequestLogout();
                }


            };
        }
    }

    public String getRedirectUri() {
        HttpSession session = request.getSession(true);
        return (String)session.getAttribute(REDIRECT_URI);
    }

    @Override
    public boolean restoreRequest() {
        HttpSession session = request.getSession(false);
        if (session == null) return false;
        return session.getAttribute(REDIRECT_URI) != null;
    }

    public static MultivaluedHashMap<String, String> parseForm(InputStream entityStream)
            throws IOException
    {
        char[] buffer = new char[100];
        StringBuffer buf = new StringBuffer();
        BufferedReader reader = new BufferedReader(new InputStreamReader(entityStream));

        int wasRead = 0;
        do
        {
            wasRead = reader.read(buffer, 0, 100);
            if (wasRead > 0) buf.append(buffer, 0, wasRead);
        } while (wasRead > -1);

        String form = buf.toString();

        MultivaluedHashMap<String, String> formData = new MultivaluedHashMap<String, String>();
        if ("".equals(form)) return formData;

        String[] params = form.split("&");

        for (String param : params)
        {
            if (param.indexOf('=') >= 0)
            {
                String[] nv = param.split("=");
                String val = nv.length > 1 ? nv[1] : "";
                formData.add(Encode.decode(nv[0]), Encode.decode(val));
            }
            else
            {
                formData.add(Encode.decode(param), "");
            }
        }
        return formData;
    }



    @Override
    public void saveRequest() {
        HttpSession session = request.getSession(true);
        session.setAttribute(REDIRECT_URI, facade.getRequest().getURI());
        session.setAttribute(SAVED_METHOD, request.getMethod());
        MultivaluedHashMap<String, String> headers = new MultivaluedHashMap<>();
        Enumeration<String> names = request.getHeaderNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            Enumeration<String> values = request.getHeaders(name);
            while (values.hasMoreElements()) {
                headers.add(name.toLowerCase(), values.nextElement());
            }
        }
        session.setAttribute(SAVED_HEADERS, headers);
        if (request.getMethod().equalsIgnoreCase("GET")) {
            return;
        }
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        byte[] buffer = new byte[4096];
        int bytesRead;
        int totalRead = 0;
        try {
            InputStream is = request.getInputStream();

            while ( (bytesRead = is.read(buffer) ) >= 0) {
                os.write(buffer, 0, bytesRead);
                totalRead += bytesRead;
                if (totalRead > maxBuffer) {
                    throw new RuntimeException("max buffer reached on a saved request");
                }

            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        byte[] body = os.toByteArray();
        // Only save the request body if there is something to save
        if (body.length > 0) {
            session.setAttribute(SAVED_BODY, body);
        }


    }

}
