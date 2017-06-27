package org.keycloak.examples.authn.delegate.util.httpclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.jboss.logging.Logger;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.models.KeycloakSession;

public class HttpClientProviderWrapper {
    KeycloakSession session;
    
    private static final Logger logger = Logger.getLogger(HttpClientProviderWrapper.class);
    
    public HttpClientProviderWrapper(KeycloakSession session) {
        this.session = session;
    }
    
    public BackChannelHttpResponse doGet(String uri) throws IOException {
        if (session == null) return null;
        if (uri == null) return null;
        
        logger.debug("doGet in BackChannel: uri =  " + uri);
        HttpClient httpClient = session.getProvider(HttpClientProvider.class).getHttpClient();
        HttpGet request = new HttpGet(uri);
        
        HttpResponse response = httpClient.execute(request);
        
        return createBackChannelHttpResponse(response);
    }
    
    public BackChannelHttpResponse doPost(String uri, String body) throws IOException {
        if (session == null) return null;
        if (uri == null) return null;
        if (body == null) return null;
        
        logger.debug("doPost in BackChannel: uri =  " + uri);
        logger.debug("doPost in BackChannel: body =  " + body);
        HttpClient httpClient = session.getProvider(HttpClientProvider.class).getHttpClient();
        HttpPost request = new HttpPost(uri);
        
        request.setEntity(EntityBuilder.create().setText(body).setContentType(ContentType.APPLICATION_FORM_URLENCODED).build());
        HttpResponse response = httpClient.execute(request);
        
        return createBackChannelHttpResponse(response);
    }
    
    private BackChannelHttpResponse createBackChannelHttpResponse(HttpResponse response) throws IOException {
        BackChannelHttpResponse backChannelHttpResponse = new BackChannelHttpResponse(response.getStatusLine().getStatusCode());
        HttpEntity entity = response.getEntity();

        InputStream is = null;
        if (entity == null) {
            backChannelHttpResponse.setBody(null);
            return backChannelHttpResponse;
        }
        else is = entity.getContent();
        
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;
        try {
            while ((line = br.readLine()) != null) sb.append(line);
            logger.debug("read from inputstream : " + sb.toString());
        } catch (IOException ioe) {
            logger.warn("Eror. ioe  msg=" + ioe.getMessage() + " description=" + ioe.toString());
        } finally {
            br.close();
        }
        backChannelHttpResponse.setBody(sb.toString());
        
        return backChannelHttpResponse;
    }

}
