package org.keycloak.adapters.servlet;

import org.keycloak.adapters.AdapterSessionStore;
import org.keycloak.adapters.HttpFacade;
import org.keycloak.util.MultivaluedHashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;

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
                headers.add(name, values.nextElement());
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
                os.write(buffer);
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
