package org.keycloak.testsuite.util.saml;


import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.apache.commons.io.IOUtils;
import org.keycloak.saml.SAMLRequestParser;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.keycloak.saml.processing.web.util.RedirectBindingUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SamlMessageReceiver implements AutoCloseable {

    private final static Pattern SAML_MESSAGE_PATTER = Pattern.compile(".*SAML(?:Response|Request)=([^&]*).*");

    private final HttpServer server;
    private String message;
    private final String url;

    public SamlMessageReceiver(int port) {
        try {
            InetSocketAddress address = new InetSocketAddress(InetAddress.getByName("localhost"), port);
            server = HttpServer.create(address, 0);
            this.url = "http://" + address.getHostString() + ":" + port ;
        } catch (IOException e) {
            throw new RuntimeException("Cannot create http server", e);
        }
        
        server.createContext("/", new MyHandler());
        server.setExecutor(null);
        server.start();
    }
    
    public String getUrl() {
        return url;
    }
    
    public boolean isMessageReceived() {
        return message != null && !message.trim().isEmpty();
    }

    public String getMessageString() {
        return message;
    }
    
    public SAMLDocumentHolder getSamlDocumentHolder() {
        Matcher m = SAML_MESSAGE_PATTER.matcher(message);
        if (m.find()) {
            try {
                return SAMLRequestParser.parseResponsePostBinding(RedirectBindingUtil.urlDecode(m.group(1)));
            } catch (IOException e) {
                throw new RuntimeException("Cannot parse response " + m.group(1), e);
            }
        }

        return null;
    }

    @Override
    public void close() throws Exception {
        server.stop(0);
    }

    private class MyHandler implements HttpHandler {
        public void handle(HttpExchange t) throws IOException {
            t.sendResponseHeaders(200, 0);

            SamlMessageReceiver.this.message = IOUtils.toString(t.getRequestBody(), StandardCharsets.UTF_8.name());
            
            OutputStream os = t.getResponseBody();
            os.close();
        }
    }
}


