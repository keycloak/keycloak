package org.keycloak.connections.httpclient;

import org.apache.http.client.HttpClient;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface HttpClientFactory extends ProviderFactory<HttpClientProvider> {
}
