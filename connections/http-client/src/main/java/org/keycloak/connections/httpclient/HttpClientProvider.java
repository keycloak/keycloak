package org.keycloak.connections.httpclient;

import org.apache.http.client.HttpClient;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface HttpClientProvider extends Provider {
    HttpClient getHttpClient();

    /**
     * Helper method
     *
     * @param uri
     * @param text
     * @return http response status
     * @throws IOException
     */
    public int postText(String uri, String text) throws IOException;

    /**
     * Helper method
     *
     * @param uri
     * @return response stream, you must close this stream or leaks will happen
     * @throws IOException
     */
    public InputStream get(String uri) throws IOException;
}
