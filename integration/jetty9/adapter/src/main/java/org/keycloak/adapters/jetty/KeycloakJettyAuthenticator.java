/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.adapters.jetty;

import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.security.DefaultUserIdentity;
import org.eclipse.jetty.security.ServerAuthException;
import org.eclipse.jetty.security.UserAuthentication;
import org.eclipse.jetty.security.authentication.FormAuthenticator;
import org.eclipse.jetty.server.Authentication;
import org.eclipse.jetty.server.HttpChannel;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.util.MultiMap;
import org.eclipse.jetty.util.URIUtil;
import org.keycloak.adapters.AdapterConstants;
import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.adapters.KeycloakConfigResolver;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.adapters.NodesRegistrationManagement;
import org.w3c.dom.Document;

import javax.security.auth.Subject;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionListener;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.Principal;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class KeycloakJettyAuthenticator extends FormAuthenticator {
    private final static Logger log = Logger.getLogger(""+KeycloakJettyAuthenticator.class);

    protected ServletContext theServletContext = null;

    protected AdapterDeploymentContext deploymentContext;
    protected NodesRegistrationManagement nodesRegistrationManagement;
    protected int timerInterval = -1;

    protected Timer timer = null;

    public static final String EMPTY_PASSWORD = "EMPTY_STR";

    protected boolean enableAudit = false;

    public static final String FORM_PRINCIPAL_NOTE = "picketlink.form.principal";
    public static final String FORM_ROLES_NOTE = "picketlink.form.roles";
    public static final String FORM_REQUEST_NOTE = "picketlink.REQUEST";

    public static final String logoutPage = "/logout.html"; // get from configuration

    protected String serviceURL = null;
    protected String identityURL = null;
    protected String issuerID = null;
    protected String configFile;

    // Whether the authenticator has to to save and restore request
    protected boolean saveRestoreRequest = true;

    /**
     * A Lock for Handler operations in the chain
     */
    protected Lock chainLock = new ReentrantLock();
    protected String canonicalizationMethod = CanonicalizationMethod.EXCLUSIVE_WITH_COMMENTS;


    public KeycloakJettyAuthenticator() {
    }

    public KeycloakJettyAuthenticator(String login, String error, boolean dispatch) {
        super(login, error, dispatch);
    }

    @Override
    public void setConfiguration(AuthConfiguration configuration) {
        super.setConfiguration(configuration);
        initializeKeycloak();
    }

    @SuppressWarnings("UseSpecificCatch")
    @Override
    public void initializeKeycloak() {
        String contextPath = ContextHandler.getCurrentContext().getContextPath();
        ServletContext theServletContext = ContextHandler.getCurrentContext().getContext(contextPath);
        // Possible scenarios:
        // 1) The deployment has a keycloak.config.resolver specified and it exists:
        //    Outcome: adapter uses the resolver
        // 2) The deployment has a keycloak.config.resolver and isn't valid (doesn't exists, isn't a resolver, ...) :
        //    Outcome: adapter is left unconfigured
        // 3) The deployment doesn't have a keycloak.config.resolver , but has a keycloak.json (or equivalent)
        //    Outcome: adapter uses it
        // 4) The deployment doesn't have a keycloak.config.resolver nor keycloak.json (or equivalent)
        //    Outcome: adapter is left unconfigured

        String configResolverClass = theServletContext.getInitParameter("keycloak.config.resolver");
        if (configResolverClass != null) {
            try {
                KeycloakConfigResolver configResolver = (KeycloakConfigResolver) ContextHandler.getCurrentContext().getClassLoader().loadClass(configResolverClass).newInstance();
                deploymentContext = new AdapterDeploymentContext(configResolver);
                log.log(Level.INFO, "Using {0} to resolve Keycloak configuration on a per-request basis.", configResolverClass);
            } catch (Exception ex) {
                log.log(Level.FINE, "The specified resolver {0} could NOT be loaded. Keycloak is unconfigured and will deny all requests. Reason: {1}", new Object[]{configResolverClass, ex.getMessage()});
                deploymentContext = new AdapterDeploymentContext(new KeycloakDeployment());
            }
        } else {
            InputStream configInputStream = getConfigInputStream(theServletContext);
            KeycloakDeployment kd;
            if (configInputStream == null) {
                log.fine("No adapter configuration. Keycloak is unconfigured and will deny all requests.");
                kd = new KeycloakDeployment();
            } else {
                kd = KeycloakDeploymentBuilder.build(configInputStream);
            }
            deploymentContext = new AdapterDeploymentContext(kd);
            log.fine("Keycloak is using a per-deployment configuration.");
        }

        theServletContext.setAttribute(AdapterDeploymentContext.class.getName(), deploymentContext);
        AuthenticatedActionsValve actions = new AuthenticatedActionsValve(deploymentContext, getNext(), getContainer());
        setNext(actions);

        nodesRegistrationManagement = new NodesRegistrationManagement();
    }

    private static InputStream getJSONFromServletContext(ServletContext servletContext) {
        String json = servletContext.getInitParameter(AdapterConstants.AUTH_DATA_PARAM_NAME);
        if (json == null) {
            return null;
        }
        log.finest("**** using " + AdapterConstants.AUTH_DATA_PARAM_NAME);
        log.finest(json);
        return new ByteArrayInputStream(json.getBytes());
    }


    private InputStream getConfigInputStream(ServletContext servletContext) {
        InputStream is = getJSONFromServletContext(servletContext);
        if (is == null) {
            String path = servletContext.getInitParameter("keycloak.config.file");
            if (path == null) {
                log.finest("**** using /WEB-INF/keycloak.json");
                is = servletContext.getResourceAsStream("/WEB-INF/keycloak.json");
            } else {
                try {
                    is = new FileInputStream(path);
                } catch (FileNotFoundException e) {
                    log.severe("NOT FOUND /WEB-INF/keycloak.json");
                    throw new RuntimeException(e);
                }
            }
        }
        return is;
    }



    @Override
    public Authentication validateRequest(ServletRequest servletRequest, ServletResponse servletResponse, boolean mandatory)
            throws ServerAuthException {
        // TODO: Deal with character encoding
        // request.setCharacterEncoding(xyz)

        String contextPath = ContextHandler.getCurrentContext().getContextPath();
        theServletContext = ContextHandler.getCurrentContext().getContext(contextPath);

        // Get the session
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        HttpSession session = request.getSession();

        System.out.println("Request ID=" + servletRequest.toString());
        System.out.println("Session ID=" + session.getId());

        // check if this call is resulting from the redirect after successful authentication.
        // if so, make the authentication successful and continue the original request
        if (saveRestoreRequest && matchRequest(request)) {
            Principal savedPrincipal = (Principal) session.getAttribute(FORM_PRINCIPAL_NOTE);
            List<String> savedRoles = (List<String>) session.getAttribute(FORM_ROLES_NOTE);
            Authentication registeredAuthentication = register(request, savedPrincipal, savedRoles);

            // try to restore the original request (including post data, etc...)
            if (restoreRequest(request, session)) {
                // success! user is authenticated; continue processing original request
                return registeredAuthentication;
            } else {
                // no saved request found...
                return Authentication.UNAUTHENTICATED;
            }
        }
        ServiceProviderSAMLWorkflow serviceProviderSAMLWorkflow = new ServiceProviderSAMLWorkflow();
        serviceProviderSAMLWorkflow.setRedirectionHandler(new JettyRedirectionHandler());

        // Eagerly look for Local LogOut
        boolean localLogout = serviceProviderSAMLWorkflow.isLocalLogoutRequest(request);

        if (localLogout) {
            try {
                serviceProviderSAMLWorkflow.sendToLogoutPage(request, response, session, theServletContext, logoutPage);
            } catch (ServletException e) {
                logger.samlLogoutError(e);
                throw new RuntimeException(e);
            } catch (IOException e1) {
                logger.samlLogoutError(e1);
                throw new RuntimeException(e1);
            }
            return Authentication.UNAUTHENTICATED;
        }

        String samlRequest = request.getParameter(GeneralConstants.SAML_REQUEST_KEY);
        String samlResponse = request.getParameter(GeneralConstants.SAML_RESPONSE_KEY);

        Principal principal = request.getUserPrincipal();

        try {
            // If we have already authenticated the user and there is no request from IDP or logout from user
            if (principal != null
                    && !(serviceProviderSAMLWorkflow.isLocalLogoutRequest(request) || isNotNull(samlRequest) || isNotNull(samlResponse)))
                return Authentication.SEND_SUCCESS;

            // General User Request
            if (!isNotNull(samlRequest) && !isNotNull(samlResponse)) {
                return generalUserRequest(servletRequest, servletResponse, mandatory);
            }

            // Handle a SAML Response from IDP
            if (isNotNull(samlResponse)) {
                return handleSAMLResponse(servletRequest, servletResponse, mandatory);
            }

            // Handle SAML Requests from IDP
            if (isNotNull(samlRequest)) {
                return handleSAMLRequest(servletRequest, servletResponse, mandatory);
            }// end if

            // local authentication
            return localAuthentication(servletRequest, servletResponse, mandatory);
        } catch (IOException e) {
            if (StringUtil.isNotNull(spConfiguration.getErrorPage())) {
                try {
                    request.getRequestDispatcher(spConfiguration.getErrorPage()).forward(request, response);
                } catch (ServletException e1) {
                    logger.samlErrorPageForwardError(spConfiguration.getErrorPage(), e1);
                } catch (IOException e1) {
                    logger.samlErrorPageForwardError(spConfiguration.getErrorPage(), e1);
                }
                return Authentication.UNAUTHENTICATED;
            } else {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Handle the user invocation for the first time
     *
     * @param servletRequest
     * @param servletResponse
     * @param mandatory
     * @return
     * @throws java.io.IOException
     */
    private Authentication generalUserRequest(ServletRequest servletRequest, ServletResponse servletResponse, boolean mandatory)
            throws IOException, ServerAuthException {
        //only perform SAML Authentication if it is mandatory
        if(!mandatory){
            Request request = (Request) servletRequest;
            return request.getAuthentication();
        }
        ServiceProviderSAMLWorkflow serviceProviderSAMLWorkflow = new ServiceProviderSAMLWorkflow();
        serviceProviderSAMLWorkflow.setRedirectionHandler(new JettyRedirectionHandler());

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        HttpSession session = request.getSession(false);
        boolean willSendRequest = false;

        HTTPContext httpContext = new HTTPContext(request, response, theServletContext);
        Set<SAML2Handler> handlers = chain.handlers();

        boolean postBinding = spConfiguration.getBindingType().equals("POST");

        // Neither saml request nor response from IDP
        // So this is a user request
        SAML2HandlerResponse saml2HandlerResponse = null;
        try {
            ServiceProviderBaseProcessor baseProcessor = new ServiceProviderBaseProcessor(postBinding, serviceURL,
                    this.picketLinkConfiguration);
            if (issuerID != null)
                baseProcessor.setIssuer(issuerID);

            baseProcessor.setIdentityURL(identityURL);
            baseProcessor.setAuditHelper(auditHelper);

            saml2HandlerResponse = baseProcessor.process(httpContext, handlers, chainLock);
        } catch (ProcessingException pe) {
            logger.samlSPHandleRequestError(pe);
            throw new RuntimeException(pe);
        } catch (ParsingException pe) {
            logger.samlSPHandleRequestError(pe);
            throw new RuntimeException(pe);
        } catch (ConfigurationException pe) {
            logger.samlSPHandleRequestError(pe);
            throw new RuntimeException(pe);
        }

        willSendRequest = saml2HandlerResponse.getSendRequest();

        Document samlResponseDocument = saml2HandlerResponse.getResultingDocument();
        String relayState = saml2HandlerResponse.getRelayState();

        String destination = saml2HandlerResponse.getDestination();
        String destinationQueryStringWithSignature = saml2HandlerResponse.getDestinationQueryStringWithSignature();

        if (destination != null && samlResponseDocument != null) {
            try {
                if (saveRestoreRequest) {
                    this.saveRequest(request, session);
                }
                if (enableAudit) {
                    PicketLinkAuditEvent auditEvent = new PicketLinkAuditEvent(AuditLevel.INFO);
                    auditEvent.setType(PicketLinkAuditEventType.REQUEST_TO_IDP);
                    auditEvent.setWhoIsAuditing(theServletContext.getContextPath());
                    auditHelper.audit(auditEvent);
                }
                serviceProviderSAMLWorkflow.sendRequestToIDP(destination, samlResponseDocument, relayState, response,
                        willSendRequest, destinationQueryStringWithSignature, isHttpPostBinding());
                return Authentication.SEND_CONTINUE;
            } catch (Exception e) {
                logger.samlSPHandleRequestError(e);
                throw logger.samlSPProcessingExceptionError(e);
            }
        }

        return localAuthentication(servletRequest, servletResponse, mandatory);
    }

    protected boolean matchRequest(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        synchronized (session) {
            String j_uri = (String) session.getAttribute(__J_URI);
            if (j_uri != null) {
                // check if the request is for the same url as the original and restore
                // params if it was a post
                StringBuffer buf = request.getRequestURL();
                if (request.getQueryString() != null)
                    buf.append("?").append(request.getQueryString());

                if (j_uri.equals(buf.toString())) {
                    return true;
                }
            }
            return false;
        }
    }

    protected Authentication register(HttpServletRequest httpServletRequest, Principal principal, List<String> roles) {
        if (roles == null) {
            roles = new ArrayList<String>();
        }
        HttpSession session = httpServletRequest.getSession(false);
        session.setAttribute(FORM_PRINCIPAL_NOTE, principal);
        session.setAttribute(FORM_ROLES_NOTE, roles);
        Request request = (Request) httpServletRequest;
        Authentication authentication = request.getAuthentication();
        if (!(authentication instanceof UserAuthentication)) {
            Subject theSubject = new Subject();
            String[] theRoles = new String[roles.size()];
            roles.toArray(theRoles);

            UserIdentity userIdentity = new DefaultUserIdentity(theSubject, principal, theRoles);
            authentication = new UserAuthentication(getAuthMethod(), userIdentity);
            request.setAuthentication(authentication);
        }
        return authentication;
    }

    protected boolean restoreRequest(HttpServletRequest request, HttpSession session) {
        synchronized (session) {
            String j_uri = (String) session.getAttribute(__J_URI);
            if (j_uri != null) {
                // check if the request is for the same url as the original and restore
                // params if it was a post
                StringBuffer buf = request.getRequestURL();
                if (request.getQueryString() != null)
                    buf.append("?").append(request.getQueryString());

                /*
                 * if (j_uri.equals(buf.toString())) {
                 */
                MultiMap<String> j_post = (MultiMap<String>) session.getAttribute(__J_POST);
                if (j_post != null) {
                    Request base_request = HttpChannel.getCurrentHttpChannel().getRequest();
                    base_request.setParameters(j_post);
                }
                session.removeAttribute(__J_URI);
                session.removeAttribute(__J_METHOD);
                session.removeAttribute(__J_POST);
                // }
                return true;
            }
            return false;
        }
    }

    protected void saveRequest(HttpServletRequest request, HttpSession session) {
        // remember the current URI
        synchronized (session) {
            // But only if it is not set already, or we save every uri that leads to a login form redirect
            if (session.getAttribute(__J_URI) == null) {
                StringBuffer buf = request.getRequestURL();
                if (request.getQueryString() != null)
                    buf.append("?").append(request.getQueryString());
                session.setAttribute(__J_URI, buf.toString());
                session.setAttribute(__J_METHOD, request.getMethod());

                if (MimeTypes.Type.FORM_ENCODED.is(request.getContentType()) && HttpMethod.POST.is(request.getMethod())) {
                    Request base_request = (request instanceof Request) ? (Request) request : HttpChannel
                            .getCurrentHttpChannel().getRequest();
                    base_request.extractParameters();
                    session.setAttribute(__J_POST, new MultiMap<String>(base_request.getParameters()));
                }
            }
        }
    }

    /**
     * Fall back on local authentication at the service provider side
     *
     * @param servletRequest
     * @param servletRequest
     * @param mandatory
     * @return
     * @throws java.io.IOException
     */
    protected Authentication localAuthentication(ServletRequest servletRequest, ServletResponse servletResponse,
            boolean mandatory) throws IOException, ServerAuthException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        if (request.getUserPrincipal() == null) {
            logger.samlSPFallingBackToLocalFormAuthentication();// fallback
            try {
                return super.validateRequest(servletRequest, servletResponse, mandatory);
            } catch (NoSuchMethodError e) {
                /*
                 * // Use Reflection try { Method method = super.getClass().getMethod("authenticate", new Class[] {
                 * HttpServletRequest.class, HttpServletResponse.class, LoginConfig.class }); return (Boolean)
                 * method.invoke(this, new Object[] { request.getRequest(), response.getResponse(), loginConfig }); } catch
                 * (Exception ex) { throw logger.unableLocalAuthentication(ex); }
                 */
            }
        } else {
            return Authentication.SEND_SUCCESS;
        }
        return Authentication.UNAUTHENTICATED;
    }

    protected boolean sessionIsValid(HttpSession session) {
        try {
            long sessionTime = session.getCreationTime();
        } catch (IllegalStateException ise) {
            return false;
        }
        return true;
    }

    protected String savedRequestURL(HttpSession session) {
        StringBuilder builder = new StringBuilder();
        HttpServletRequest request = (HttpServletRequest) session.getAttribute(FORM_REQUEST_NOTE);
        if (request != null) {
            builder.append(request.getRequestURI());
            if (request.getQueryString() != null) {
                builder.append("?").append(request.getQueryString());
            }
        }
        return builder.toString();
    }


     /**
     * An instance of {@link org.picketlink.identity.federation.core.saml.workflow.ServiceProviderSAMLWorkflow.RedirectionHandler}
     * that performs JETTY specific redirection and post workflows
     */
    public class JettyRedirectionHandler extends ServiceProviderSAMLWorkflow.RedirectionHandler {
        @Override
        public void sendRedirectForRequestor(String destination, HttpServletResponse response) throws IOException {
            common(destination, response);
            response.setHeader("Cache-Control", "no-cache, no-store");
            sendRedirect(response, destination);
        }

        @Override
        public void sendRedirectForResponder(String destination, HttpServletResponse response) throws IOException {
            common(destination, response);
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate,private");
            sendRedirect(response, destination);
        }

        private void common(String destination, HttpServletResponse response) {
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Location", destination);
            response.setHeader("Pragma", "no-cache");
        }

        private void sendRedirect(HttpServletResponse response, String destination) throws IOException {
            // response.reset();
            response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
            response.sendRedirect(destination);
        }
    }
}
