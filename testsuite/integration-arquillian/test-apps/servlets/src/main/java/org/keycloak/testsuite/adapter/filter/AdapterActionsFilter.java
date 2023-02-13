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

package org.keycloak.testsuite.adapter.filter;


import org.jboss.logging.Logger;
import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.rotation.JWKPublicKeyLocator;
import org.keycloak.common.util.Time;
import org.keycloak.common.util.reflections.Reflections;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;

/**
 * Filter to handle "special" requests to perform actions on adapter side (for example setting time offset )
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class AdapterActionsFilter implements Filter {

    public static final String TIME_OFFSET_PARAM = "timeOffset";
    public static final String RESET_DEPLOYMENT_PARAM = "resetDeployment";

    private static final Logger log = Logger.getLogger(AdapterActionsFilter.class);

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest servletReq = (HttpServletRequest) request;
        HttpServletResponse servletResp = (HttpServletResponse) response;

        //Accept timeOffset as argument to enforce timeouts
        String timeOffsetParam = request.getParameter(TIME_OFFSET_PARAM);
        String resetDeploymentParam = request.getParameter(RESET_DEPLOYMENT_PARAM);

        if (timeOffsetParam != null && !timeOffsetParam.isEmpty()) {
            int timeOffset = Integer.parseInt(timeOffsetParam);
            log.infof("Time offset updated to %d for application %s", timeOffset, servletReq.getRequestURI());
            Time.setOffset(timeOffset);
            writeResponse(servletResp, "Offset set successfully");
        } else if (resetDeploymentParam != null && !resetDeploymentParam.isEmpty()) {
            AdapterDeploymentContext deploymentContext = (AdapterDeploymentContext) request.getServletContext().getAttribute(AdapterDeploymentContext.class.getName());

            Field field = Reflections.findDeclaredField(AdapterDeploymentContext.class, "deployment");
            Reflections.setAccessible(field);
            KeycloakDeployment deployment = (KeycloakDeployment) Reflections.getFieldValue(field, deploymentContext);

            Time.setOffset(0);
            deployment.setNotBefore(0);
            if (deployment.getPublicKeyLocator() instanceof JWKPublicKeyLocator) {
                deployment.setPublicKeyLocator(new JWKPublicKeyLocator());
            }

            log.infof("Restarted PublicKeyLocator, notBefore and timeOffset for application %s", servletReq.getRequestURI());
            writeResponse(servletResp, "Restarted PublicKeyLocator, notBefore and timeOffset successfully");
        } else {
            // Continue request
            chain.doFilter(request, response);
        }

    }

    @Override
    public void destroy() {

    }

    private void writeResponse(HttpServletResponse response, String responseText) throws IOException {
        response.setContentType("text/html");
        PrintWriter writer = response.getWriter();
        writer.println("<html><body>" + responseText + "</body></html>");
        writer.flush();
    }
}
