package org.keycloak.examples.authn.delegate.authenticator;

import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.AuthenticationFlowException;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.authenticators.browser.AbstractUsernameFormAuthenticator;
import org.keycloak.examples.authn.delegate.util.httpclient.BackChannelHttpResponse;
import org.keycloak.examples.authn.delegate.util.httpclient.HttpClientProviderWrapper;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.ErrorPageException;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.messages.Messages;
import org.keycloak.theme.BrowserSecurityHeaderSetup;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.EnvUtil;

import org.jboss.resteasy.spi.HttpRequest;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import java.io.InputStream;
import java.io.IOException;
import java.io.FileInputStream;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Properties;

public class AuthnDelegationAuthenticator implements Authenticator  {
    protected static ServicesLogger log = ServicesLogger.LOGGER;
    
    @Override
    public void close() {
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        // terminate authenticate challenge on login timeout in order to forwarding appropriate parameters of end user's user agent.
        if (context.getClientSession().getNote(AuthnDelegationAuthenticatorFactory.LOGIN_CHALLENGE_MARKER) != null) {
            handleAuthenticationFlowException(AuthenticationFlowError.EXPIRED_CODE, "terminate authenticate challenge on login timeout");
        }

        // TODO: need to verify value of each items of configuration
        
        // extract code and execution for this browser login session
        // those binds all HTTP connections by keycloak's login session and external authentication server's login session
        String code = context.generateAccessCode();
        String execution = context.getExecution().getId();
        
        // extract header fields, query parameters, and client_id from HTTP request
        // for picking up forwarding parameters toward external authentication server
        HttpRequest httpRequest = context.getHttpRequest();
        MultivaluedMap<String, String> headerFields = httpRequest.getHttpHeaders().getRequestHeaders();
        MultivaluedMap<String, String> queryParameters = httpRequest.getUri().getQueryParameters();
        log.debug("headerFields = " + headerFields);
        log.debug("queryParameters = " + queryParameters);

        // Load authenticator configuration
        AuthenticatorConfigModel config = context.getAuthenticatorConfig();
        if (config == null) {
            handleRuntimeException("Unable to load authenticator config");
        }
        Map<String, String> configMap = config.getConfig();
        String baseUri = configMap.get(AuthnDelegationAuthenticatorFactory.AS_AUTHN_URI);

        checkSslForFrontEndCommunication(context, baseUri);

        // Determine whether conducting HTTP FORM POST or HTTP redirect.
        boolean isFormPost = Boolean.valueOf(configMap.get(AuthnDelegationAuthenticatorFactory.IS_HTTP_FORM_POST));
                
        // Read forwarding query parameters
        String path = configMap.get(AuthnDelegationAuthenticatorFactory.FW_QUERY_PARAMS);
        path = EnvUtil.replace(path);
        Properties propsQueryParams = new Properties();
        try {
            InputStream is = new FileInputStream(path);
            propsQueryParams.load(is);
            is.close();
        } catch (IOException ioe) {
            // ignore forwarding query parameters intentionally
            log.warn("failed to load forwarding query parameters from property file so that ignore forwarding query parameters intentionally. summary=" + ioe.toString() + " detail=" + ioe.getMessage());
        }
        
        // Read forwarding http header fields
        path = configMap.get(AuthnDelegationAuthenticatorFactory.FW_HTTP_HEADERS);
        path = EnvUtil.replace(path);
        Properties propsHeaders = new Properties();
        try {
            InputStream is = new FileInputStream(path);
            propsHeaders.load(is);
            is.close();
        } catch (IOException ioe) {
            // ignore forwarding header fields intentionally
            log.warn("failed to load forwarding header fields from property file so that ignore forwarding header fields intentionally. summary=" + ioe.toString() + " detail=" + ioe.getMessage());
        }
        
        Response challengeResponse;
        
        // Only supporting either HTTP Redirect Binding or POST Binding for accessing external authentication server
        URI externalAuthenticationServerUri = null;
                
        if(isFormPost) {
            // HTTP POST Binding
            try {
                externalAuthenticationServerUri = new URI(baseUri);
            } catch (URISyntaxException e) {
                handleRuntimeException("Unexpected exception occured when instantiating URI for external authentication server.", e);
            }
            String postBindBody = getPostBindBody(externalAuthenticationServerUri.toString(), code, execution, propsQueryParams, queryParameters, propsHeaders, headerFields);
            // Secure Header Settings by Security Defenses - Headers
            Response.ResponseBuilder builder = Response.status(200).type(MediaType.TEXT_HTML_TYPE).entity(postBindBody);
            BrowserSecurityHeaderSetup.headers(builder, context.getRealm());
            anthnSecureHeaders(builder);
            challengeResponse = builder.build();
        } else {
            // HTTP Redirect Binding
            try {
                externalAuthenticationServerUri = new URI(baseUri + getRedirectBindQueryString(code, execution, propsQueryParams, queryParameters, propsHeaders, headerFields));
            } catch (Exception e) {
                handleRuntimeException("Unexpected exception occured when instantiating URI for external authentication server.", e);
            }
            challengeResponse = Response.status(302).location(externalAuthenticationServerUri).build();
        }
        // mark for preventing restart of this authentication challenge due to forwarding appropriate parameters of end user's user agent.
        context.getClientSession().setNote(AuthnDelegationAuthenticatorFactory.LOGIN_CHALLENGE_MARKER, "ON");
        
        context.getClientSession().setNote(AuthenticationProcessor.CURRENT_AUTHENTICATION_EXECUTION, context.getExecution().getId());
        context.challenge(challengeResponse);
    }
    
    @Override
    public void action(AuthenticationFlowContext context) {
        
        // TODO: need to verify value of each items of configuration
        
        // receive HTTP POST request from external authentication server
        // carrying artifact for querying user id in back channel of external authentication server
        HttpRequest httpRequest = context.getHttpRequest();
        
        // get URI for obtaining authenticated user id from external authentication server
        String baseUri = null;
        AuthenticatorConfigModel config = context.getAuthenticatorConfig();
        if (config != null) {
            baseUri = config.getConfig().get(AuthnDelegationAuthenticatorFactory.AS_USERID_URI);
        } else {
            handleRuntimeException("Unable to load authenticator config");
        }

        // record event and raise error. please consult Authorization Endpoint - checkSsl(), checkClient(), et al.
        checkSslForBackEndCommunication(Boolean.valueOf(config.getConfig().get(AuthnDelegationAuthenticatorFactory.IS_BACKEND_COMM_SSL_REQUIRED)), baseUri);

        String artifactValue = null;
        MultivaluedMap<String, String> formData = httpRequest.getDecodedFormParameters();
        if (formData.containsKey("cancel")) {
        	log.debug("user cancel authentication in authn server.");
            context.cancelLogin();
            return;
        }
        if (formData.containsKey("artifact")) {
        	artifactValue = formData.getFirst("artifact");
        } else {
        	log.warn("authn server send no artifact.");
            context.cancelLogin();
            return;
        }
        if (!verifyArtifact(artifactValue)) {
            handleAuthenticationFlowException(AuthenticationFlowError.INTERNAL_ERROR, "invalid artifact received from the external authentication server.");
        }
        
        String postText = "artifact=" + artifactValue;
        log.debug("postText = " + postText);
        
        // get authenticated user ID from external authentication server
        // Conventionally, use the term "username" for user ID which both keycloak and external authentication server use to refer unique user in this context.
        HttpClientProviderWrapper httpClientProviderWrapper = new HttpClientProviderWrapper(context.getSession());
        BackChannelHttpResponse backChannelHttpResponse = null;
        try {
            backChannelHttpResponse = httpClientProviderWrapper.doPost(baseUri, postText);
        } catch (IOException ioe) {
            handleAuthenticationFlowException(AuthenticationFlowError.INTERNAL_ERROR, "unable to communicate with the external authentication server in the back channel. summary=" + ioe.toString() + " detail=" + ioe.getMessage());
        }
        int status = backChannelHttpResponse.getStatusCode();
        if (status != 204 && status != 200) {
            handleAuthenticationFlowException(AuthenticationFlowError.INTERNAL_ERROR, "The external authentication server returned the error status: " + status);
        }
        String username = backChannelHttpResponse.getBody().split("=", 0)[1];
        
        context.getEvent().detail(Details.USERNAME, username);
        context.getClientSession().setNote(AbstractUsernameFormAuthenticator.ATTEMPTED_USERNAME, username);

        UserModel user = null;
          // here, it relies on DelegatedUserStorageProvider as User Storage SPI's provider to get user info
           // from external authentication server and convert it onto userModel by using adapter class.
        user = context.getSession().users().getUserByUsername(username, context.getRealm());
        if (user == null) {
            handleAuthenticationFlowException(AuthenticationFlowError.INTERNAL_ERROR, "failed to get userinfo from the external authentication server in backchannel communitacions");
        }
        log.debug("usermodel username=" + user.getUsername());
        context.setUser(user);
        context.success();
    }
    
    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return false;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
    }

    private String getRedirectBindQueryString(String code, String execution,
                Properties propsQueryParams, MultivaluedMap<String, String> queryParameters,
                Properties propsHeaders, MultivaluedMap<String, String> headerFields) {

        StringBuilder builder = new StringBuilder();
        
        // needed for binding external authentication server login session and keycloak browser login session
        builder.append("?code=" + code + "&execution=" + execution);
        
        String clientId = queryParameters.getFirst(OAuth2Constants.CLIENT_ID);
        if (clientId != null) builder.append("&client_id=" + clientId);
        
        // forwarding http header fields
        // TODO: how about checking whether each query parameter name is URL safe or not, and if not, do what?
        for (String propertyName : propsHeaders.stringPropertyNames()) {
            String propertyValue = propsHeaders.getProperty(propertyName);
            log.debug(propertyName + " = " + propertyValue);
            if (headerFields.containsKey(propertyName)) {
                builder.append("&" + propertyValue + "=" + Base64Url.encode(headerFields.getFirst(propertyName).getBytes()));
                log.debug("forwarding header " + propertyValue + " = " + headerFields.getFirst(propertyName) + " base64url enc -> " + Base64Url.encode(headerFields.getFirst(propertyName).getBytes()));
            }
        }
        
        // forwarding query parameters
        // TODO: how about checking whether each query parameter name is URL safe or not, and if not, do what?
        for (String propertyName : propsQueryParams.stringPropertyNames()) {
            String propertyValue = propsQueryParams.getProperty(propertyName);
            log.debug(propertyName + " = " + propertyValue);
            if (queryParameters.containsKey(propertyName)) {
                builder.append("&" + propertyValue + "=" + Base64Url.encode(queryParameters.getFirst(propertyName).getBytes()));
                log.debug("forwarding parameter " + propertyValue + " = " + queryParameters.getFirst(propertyName) + " base64url enc -> " + Base64Url.encode(queryParameters.getFirst(propertyName).getBytes()));
            }
        }

        return builder.toString();
    }

    private String getPostBindBody(String redirect, String code, String execution, 
            Properties propsQueryParams, MultivaluedMap<String, String> queryParameters,
            Properties propsHeaders, MultivaluedMap<String, String> headerFields) {
        
        StringBuilder builder = new StringBuilder();
        
        builder.append("<HTML>");
        builder.append("<HEAD>");
        builder.append("<TITLE>Submit This Form</TITLE>");
        builder.append("</HEAD>");
        builder.append("<BODY Onload=\"javascript:document.forms[0].submit()\">");
        builder.append("<FORM METHOD=\"POST\" ACTION=\"" + redirect + "\">");
        
        // needed for binding external authentication server and keycloak browser login authenticator provider
        builder.append("<INPUT name=\"code\" TYPE=\"HIDDEN\" VALUE=\"" + code + "\" />");
        builder.append("<INPUT name=\"execution\" TYPE=\"HIDDEN\" VALUE=\"" + execution + "\" />");
        
        String clientId = queryParameters.getFirst(OAuth2Constants.CLIENT_ID);
        builder.append("<INPUT name=\"client_id\" TYPE=\"HIDDEN\" VALUE=\"" + clientId + "\" />");
        
        // forwarding http header fields
        for (String propertyName : propsHeaders.stringPropertyNames()) {
            String propertyValue = propsHeaders.getProperty(propertyName);
            log.debug(propertyName + " = " + propertyValue);
            if (headerFields.containsKey(propertyName)) {
                log.debug("forwarding header " + propertyValue + " = " + headerFields.getFirst(propertyName));
                builder.append("<INPUT name=\"" + propertyValue + "\" TYPE=\"HIDDEN\" VALUE=\"" + headerFields.getFirst(propertyName) + "\" />");
            }
        }
        
        // forwarding query parameters
        for (String propertyName : propsQueryParams.stringPropertyNames()) {
            String propertyValue = propsQueryParams.getProperty(propertyName);
            log.debug(propertyName + " = " + propertyValue);
            if (queryParameters.containsKey(propertyName)) {
                log.debug("forwarding parameter " + propertyValue + " = " + queryParameters.getFirst(propertyName));
                builder.append("<INPUT name=\"" + propertyValue + "\" TYPE=\"HIDDEN\" VALUE=\"" + queryParameters.getFirst(propertyName) + "\" />");
            }
        }
        
        builder.append("<NOSCRIPT>")
        .append("<P>JavaScript is disabled. We strongly recommend to enable it. Click the button below to continue.</P>")
        .append("<INPUT TYPE=\"SUBMIT\" VALUE=\"CONTINUE\" />")
        .append("</NOSCRIPT>");
        builder.append("</FORM></BODY></HTML>");
        return builder.toString();
    }
    
    private void checkSslForFrontEndCommunication(AuthenticationFlowContext context, String uri) {
        RealmModel realm = context.getRealm();
        ClientConnection clientConnection = context.getConnection();
        EventBuilder event = context.getEvent();
        KeycloakSession session = context.getSession();
        URI uriInfo = null;
        try {
            uriInfo = new URI(uri);
        } catch (URISyntaxException e) {
            handleRuntimeException("Unexpected exception occured when instantiating URI for external authentication server.", e);
        }
        log.debug("uri you try to access in frontend = " + uri);
        log.debug("uriInfo.getScheme.equals - https - = " + uriInfo.getScheme().equals("https"));
        log.debug("realm.getSslRequired.isRequired - clientConnection - = " + realm.getSslRequired().isRequired(clientConnection));
        if (!uriInfo.getScheme().equals("https") && realm.getSslRequired().isRequired(clientConnection)) {
            event.error(Errors.SSL_REQUIRED);
            throw new ErrorPageException(session, Messages.HTTPS_REQUIRED);
        }
    }
    
    private void checkSslForBackEndCommunication(boolean isSslRequired, String uri) {
        URI uriInfo = null;
        try {
            uriInfo = new URI(uri);
        } catch (URISyntaxException e) {
            handleRuntimeException("Unexpected exception occured when instantiating URI for external authentication server.", e);
        }
        log.debug("uri you try to access in backend = " + uri);
        log.debug("uriInfo.getScheme.equals - https - = " + uriInfo.getScheme().equals("https"));
        log.debug("isSslRequired = " + isSslRequired);
        if (!uriInfo.getScheme().equals("https") && isSslRequired) {
            handleAuthenticationFlowException(AuthenticationFlowError.INTERNAL_ERROR, "TLS must be enabled on backchannel communications.");
        }
    }
    
    private boolean verifyArtifact(String artifact) {
        if (artifact == null) return false;
        // TODO : insert codes for artifact verification, e.g. length, acceptable characters, etc...
        return true;
    }
    
    private void handleRuntimeException(String errMsg) {
        log.error(errMsg);
        throw new RuntimeException(errMsg);
    }
    
    private void handleRuntimeException(String errMsg, Throwable e) {
        log.error(errMsg + ":" + e.getMessage());
        throw new RuntimeException(errMsg, e);
    }
    
    private void handleAuthenticationFlowException(AuthenticationFlowError error, String errMsg) {
        log.error(errMsg);
        throw new AuthenticationFlowException(errMsg, error);
    }
    
    // Authentication Delegation Secure Header Settings
    private static Response.ResponseBuilder anthnSecureHeaders(Response.ResponseBuilder builder) {
        builder.header("Pragma", "no-cache");
        builder.header("X-XSS-Protection", "1; mode=block");       
        return builder;
    }
    
}
