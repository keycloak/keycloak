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

package org.keycloak.example;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;
import org.keycloak.adapters.ServerRequest;
import org.keycloak.adapters.spi.AuthenticationError;
import org.keycloak.adapters.spi.HttpFacade;
import org.keycloak.adapters.spi.LogoutError;
import org.keycloak.common.util.StreamUtil;
import org.keycloak.common.util.Time;
import org.keycloak.common.util.UriUtils;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.RefreshToken;
import org.keycloak.util.JsonSerialization;
import org.keycloak.util.TokenUtil;

import javax.security.cert.X509Certificate;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class OfflineAccessPortalServlet extends HttpServlet {


    @Override
    public void init() throws ServletException {
        getServletContext().setAttribute(HttpClient.class.getName(), new DefaultHttpClient());
    }

    @Override
    public void destroy() {
        getHttpClient().getConnectionManager().shutdown();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            if (req.getRequestURI().endsWith("/login")) {
                storeToken(req);
                req.getRequestDispatcher("/WEB-INF/pages/loginCallback.jsp").forward(req, resp);
                return;
            }

            String refreshToken = RefreshTokenDAO.loadToken();
            String refreshTokenInfo;
            boolean savedTokenAvailable;
            if (refreshToken == null) {
                refreshTokenInfo = "No token saved in database. Please login first";
                savedTokenAvailable = false;
            } else {
                RefreshToken refreshTokenDecoded = null;
                    refreshTokenDecoded = TokenUtil.getRefreshToken(refreshToken);
                String exp = (refreshTokenDecoded.getExpiration() == 0) ? "NEVER" : Time.toDate(refreshTokenDecoded.getExpiration()).toString();
                refreshTokenInfo = String.format("<p>Type: %s</p><p>ID: %s</p><p>Expires: %s</p>", refreshTokenDecoded.getType(), refreshTokenDecoded.getId(), exp);
                savedTokenAvailable = true;
            }
            req.setAttribute("tokenInfo", refreshTokenInfo);
            req.setAttribute("savedTokenAvailable", savedTokenAvailable);

            String customers;
            if (req.getRequestURI().endsWith("/loadCustomers")) {
                customers = loadCustomers(req, refreshToken);
            } else {
                customers = "";
            }
            req.setAttribute("customers", customers);

            req.getRequestDispatcher("/WEB-INF/pages/view.jsp").forward(req, resp);
        } catch (JWSInputException e) {
            throw new ServletException(e);
        }
    }

    private void storeToken(HttpServletRequest req) throws IOException, JWSInputException {
        RefreshableKeycloakSecurityContext ctx = (RefreshableKeycloakSecurityContext) req.getAttribute(KeycloakSecurityContext.class.getName());
        String refreshToken = ctx.getRefreshToken();

        RefreshTokenDAO.saveToken(refreshToken);

        RefreshToken refreshTokenDecoded = TokenUtil.getRefreshToken(refreshToken);
        Boolean isOfflineToken = refreshTokenDecoded.getType().equals(TokenUtil.TOKEN_TYPE_OFFLINE);
        req.setAttribute("isOfflineToken", isOfflineToken);
    }

    private String loadCustomers(HttpServletRequest req, String refreshToken) throws ServletException, IOException {
        // Retrieve accessToken first with usage of refresh (offline) token from DB
        String accessToken = null;
        try {
            KeycloakDeployment deployment = getDeployment(req);
            AccessTokenResponse response = ServerRequest.invokeRefresh(deployment, refreshToken);
            accessToken = response.getToken();

            // Uncomment this when you use revokeRefreshToken for realm. In that case each offline token can be used just once. So at this point, you need to
            // save new offline token into DB
            // RefreshTokenDAO.saveToken(response.getRefreshToken());
        } catch (ServerRequest.HttpFailure failure) {
            return "Failed to refresh token. Status from auth-server request: " + failure.getStatus() + ", Error: " + failure.getError();
        }

        // Load customers now
        HttpGet get = new HttpGet(UriUtils.getOrigin(req.getRequestURL().toString()) + "/database/customers");
        get.addHeader("Authorization", "Bearer " + accessToken);

        HttpResponse response = getHttpClient().execute(get);
        InputStream is = response.getEntity().getContent();
        try {
            if (response.getStatusLine().getStatusCode() != 200) {
                return "Error when loading customer. Status: " + response.getStatusLine().getStatusCode() + ", error: " + StreamUtil.readString(is);
            } else {
                List<String> list = JsonSerialization.readValue(is, TypedList.class);
                StringBuilder result = new StringBuilder();
                for (String customer : list) {
                    result.append(customer + "<br />");
                }
                return result.toString();
            }
        } finally {
            is.close();
        }
    }


    private KeycloakDeployment getDeployment(HttpServletRequest servletRequest) throws ServletException {
        // The facade object is needed just if you have relative "auth-server-url" in keycloak.json. Otherwise you can call deploymentContext.resolveDeployment(null)
        HttpFacade facade = getFacade(servletRequest);

        AdapterDeploymentContext deploymentContext = (AdapterDeploymentContext) getServletContext().getAttribute(AdapterDeploymentContext.class.getName());
        if (deploymentContext == null) {
            throw new ServletException("AdapterDeploymentContext not set");
        }
        return deploymentContext.resolveDeployment(facade);
    }

    // TODO: Merge with facade in ServletOAuthClient and move to some common servlet adapter
    private HttpFacade getFacade(final HttpServletRequest servletRequest) {
        return new HttpFacade() {

            @Override
            public Request getRequest() {
                return new Request() {

                    private InputStream inputStream;

                    @Override
                    public String getMethod() {
                        return servletRequest.getMethod();
                    }

                    @Override
                    public String getURI() {
                        return servletRequest.getRequestURL().toString();
                    }

                    @Override
                    public String getRelativePath() {
                        return servletRequest.getServletPath();
                    }

                    @Override
                    public boolean isSecure() {
                        return servletRequest.isSecure();
                    }

                    @Override
                    public String getQueryParamValue(String param) {
                        return servletRequest.getParameter(param);
                    }

                    @Override
                    public String getFirstParam(String param) {
                        return servletRequest.getParameter(param);
                    }

                    @Override
                    public Cookie getCookie(String cookieName) {
                        // not needed
                        return null;
                    }

                    @Override
                    public String getHeader(String name) {
                        return servletRequest.getHeader(name);
                    }

                    @Override
                    public List<String> getHeaders(String name) {
                        // not needed
                        return null;
                    }

                    @Override
                    public InputStream getInputStream() {
                        return getInputStream(false);
                    }

                    @Override
                    public InputStream getInputStream(boolean buffered) {
                        if (inputStream != null) {
                            return inputStream;
                        }

                        if (buffered) {
                            try {
                                return inputStream = new BufferedInputStream(servletRequest.getInputStream());
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }

                        try {
                            return servletRequest.getInputStream();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    @Override
                    public String getRemoteAddr() {
                        return servletRequest.getRemoteAddr();
                    }

                    @Override
                    public void setError(AuthenticationError error) {
                        servletRequest.setAttribute(AuthenticationError.class.getName(), error);

                    }

                    @Override
                    public void setError(LogoutError error) {
                        servletRequest.setAttribute(LogoutError.class.getName(), error);
                    }

                };
            }

            @Override
            public Response getResponse() {
                throw new IllegalStateException("Not yet implemented");
            }

            @Override
            public X509Certificate[] getCertificateChain() {
                throw new IllegalStateException("Not yet implemented");
            }
        };
    }

    private HttpClient getHttpClient() {
        return (HttpClient) getServletContext().getAttribute(HttpClient.class.getName());
    }

    static class TypedList extends ArrayList<String> {
    }
}
