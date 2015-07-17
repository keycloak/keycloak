package org.keycloak.adapters.undertow;
import io.undertow.UndertowLogger;
import io.undertow.UndertowOptions;
import io.undertow.server.Connectors;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.Session;
import io.undertow.servlet.handlers.ServletRequestContext;
import io.undertow.servlet.spec.HttpSessionImpl;
import io.undertow.util.HeaderMap;
import io.undertow.util.HeaderValues;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import io.undertow.util.ImmediatePooled;

import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.security.AccessController;
import java.util.Iterator;

/**
 * Saved servlet request.
 *
 * Note bill burke: I had to fork this because Undertow was automatically restoring the request before the code could be processed and redirected.
 *
 * @author Stuart Douglas
 */
public class SavedRequest implements Serializable {

    private static final String SESSION_KEY = SavedRequest.class.getName();

    private final byte[] data;
    private final int dataLength;
    private final HttpString method;
    private final String requestUri;
    private final HeaderMap headerMap;

    public SavedRequest(byte[] data, int dataLength, HttpString method, String requestUri, HeaderMap headerMap) {
        this.data = data;
        this.dataLength = dataLength;
        this.method = method;
        this.requestUri = requestUri;
        this.headerMap = headerMap;
    }

    public static void trySaveRequest(final HttpServerExchange exchange) {
        int maxSize = exchange.getConnection().getUndertowOptions().get(UndertowOptions.MAX_BUFFERED_REQUEST_SIZE, 16384);
        if (maxSize > 0) {
            //if this request has a body try and cache the response
            if (!exchange.isRequestComplete()) {
                final long requestContentLength = exchange.getRequestContentLength();
                if (requestContentLength > maxSize) {
                    UndertowLogger.REQUEST_LOGGER.debugf("Request to %s was to large to save", exchange.getRequestURI());
                    return;//failed to save the request, we just return
                }
                //TODO: we should really be used pooled buffers
                //TODO: we should probably limit the number of saved requests at any given time
                byte[] buffer = new byte[maxSize];
                int read = 0;
                int res = 0;
                InputStream in = exchange.getInputStream();
                try {
                    while ((res = in.read(buffer, read, buffer.length - read)) > 0) {
                        read += res;
                        if (read == maxSize) {
                            UndertowLogger.REQUEST_LOGGER.debugf("Request to %s was to large to save", exchange.getRequestURI());
                            return;//failed to save the request, we just return
                        }
                    }
                    HeaderMap headers = new HeaderMap();
                    for(HeaderValues entry : exchange.getRequestHeaders()) {
                        if(entry.getHeaderName().equals(Headers.CONTENT_LENGTH) ||
                                entry.getHeaderName().equals(Headers.TRANSFER_ENCODING) ||
                                entry.getHeaderName().equals(Headers.CONNECTION)) {
                            continue;
                        }
                        headers.putAll(entry.getHeaderName(), entry);
                    }
                    SavedRequest request = new SavedRequest(buffer, read, exchange.getRequestMethod(), exchange.getRequestURI(), exchange.getRequestHeaders());
                    final ServletRequestContext sc = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);
                    HttpSessionImpl session = sc.getCurrentServletContext().getSession(exchange, true);
                    Session underlyingSession;
                    if(System.getSecurityManager() == null) {
                        underlyingSession = session.getSession();
                    } else {
                        underlyingSession = AccessController.doPrivileged(new HttpSessionImpl.UnwrapSessionAction(session));
                    }
                    underlyingSession.setAttribute(SESSION_KEY, request);
                } catch (IOException e) {
                    UndertowLogger.REQUEST_IO_LOGGER.ioException(e);
                }
            }
        }
    }

    public static void tryRestoreRequest(final HttpServerExchange exchange, HttpSession session) {
        if(session instanceof HttpSessionImpl) {

            Session underlyingSession;
            if(System.getSecurityManager() == null) {
                underlyingSession = ((HttpSessionImpl) session).getSession();
            } else {
                underlyingSession = AccessController.doPrivileged(new HttpSessionImpl.UnwrapSessionAction(session));
            }
            SavedRequest request = (SavedRequest) underlyingSession.getAttribute(SESSION_KEY);
            if(request != null) {
                if(request.requestUri.equals(exchange.getRequestURI()) && exchange.isRequestComplete()) {
                    UndertowLogger.REQUEST_LOGGER.debugf("restoring request body for request to %s", request.requestUri);
                    exchange.setRequestMethod(request.method);
                    Connectors.ungetRequestBytes(exchange, new ImmediatePooled<ByteBuffer>(ByteBuffer.wrap(request.data, 0, request.dataLength)));
                    underlyingSession.removeAttribute(SESSION_KEY);
                    //clear the existing header map of everything except the connection header
                    //TODO: are there other headers we should preserve?
                    Iterator<HeaderValues> headerIterator = exchange.getRequestHeaders().iterator();
                    while (headerIterator.hasNext()) {
                        HeaderValues header = headerIterator.next();
                        if(!header.getHeaderName().equals(Headers.CONNECTION)) {
                            headerIterator.remove();
                        }
                    }
                    for(HeaderValues header : request.headerMap) {
                        exchange.getRequestHeaders().putAll(header.getHeaderName(), header);
                    }
                }
            }
        }
    }

}
