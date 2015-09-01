package org.keycloak.adapters.jetty;

import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.security.authentication.FormAuthenticator;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.util.MultiMap;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.jetty.core.AbstractJettySessionTokenStore;
import org.keycloak.util.MultivaluedHashMap;

import javax.servlet.http.HttpSession;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class JettySessionTokenStore extends AbstractJettySessionTokenStore {
    public static final String CACHED_FORM_PARAMETERS = "__CACHED_FORM_PARAMETERS";
    protected Request myRequest;

    public JettySessionTokenStore(Request request, KeycloakDeployment deployment) {
        super(request, deployment);
        this.myRequest = request; // for IDE/compilation purposes
    }

    protected MultiMap<String> extractFormParameters(Request base_request) {
        MultiMap<String> formParameters = new MultiMap<String>();
        base_request.extractFormParameters(formParameters);
        return formParameters;
    }
    protected void restoreFormParameters(MultiMap<String> j_post, Request base_request) {
        base_request.setContentParameters(j_post);
    }

    public boolean restoreRequest() {
        HttpSession session = myRequest.getSession(false);
        if (session == null) return false;
        synchronized (session) {
            String j_uri = (String) session.getAttribute(FormAuthenticator.__J_URI);
            if (j_uri != null) {
                // check if the request is for the same url as the original and restore
                // params if it was a post
                StringBuffer buf = myRequest.getRequestURL();
                if (myRequest.getQueryString() != null)
                    buf.append("?").append(myRequest.getQueryString());
                if (j_uri.equals(buf.toString())) {
                    String method = (String)session.getAttribute(__J_METHOD);
                    myRequest.setMethod(HttpMethod.valueOf(method.toUpperCase()), method);
                    MultivaluedHashMap<String, String> j_post = (MultivaluedHashMap<String, String>) session.getAttribute(CACHED_FORM_PARAMETERS);
                    if (j_post != null) {
                        myRequest.setContentType("application/x-www-form-urlencoded");
                        MultiMap<String> map = new MultiMap<String>();
                        for (String key : j_post.keySet()) {
                            for (String val : j_post.getList(key)) {
                                map.add(key, val);
                            }
                        }
                        restoreFormParameters(map, myRequest);
                    }
                    session.removeAttribute(FormAuthenticator.__J_URI);
                    session.removeAttribute(__J_METHOD);
                    session.removeAttribute(FormAuthenticator.__J_POST);
                }
                return true;
            }
        }
        return false;
    }

    public void saveRequest() {
        // remember the current URI
        HttpSession session = myRequest.getSession();
        synchronized (session) {
            // But only if it is not set already, or we save every uri that leads to a login form redirect
            if (session.getAttribute(FormAuthenticator.__J_URI) == null) {
                StringBuffer buf = myRequest.getRequestURL();
                if (myRequest.getQueryString() != null)
                    buf.append("?").append(myRequest.getQueryString());
                session.setAttribute(FormAuthenticator.__J_URI, buf.toString());
                session.setAttribute(__J_METHOD, myRequest.getMethod());

                if ("application/x-www-form-urlencoded".equals(myRequest.getContentType()) && "POST".equalsIgnoreCase(myRequest.getMethod())) {
                    MultiMap<String> formParameters = extractFormParameters(myRequest);
                    MultivaluedHashMap<String, String> map = new MultivaluedHashMap<String, String>();
                    for (String key : formParameters.keySet()) {
                        for (Object value : formParameters.getValues(key)) {
                            map.add(key, (String) value);
                        }
                    }
                    session.setAttribute(CACHED_FORM_PARAMETERS, map);
                }
            }
        }
    }

}
