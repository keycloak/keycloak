package org.keycloak.protocol.oidc.ida.mappers.connector;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import jakarta.ws.rs.core.Response;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jboss.logging.Logger;
import org.keycloak.OAuthErrorException;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.protocol.ProtocolMapperConfigException;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.services.ErrorResponseException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;

import static org.keycloak.protocol.oidc.ida.mappers.IdaConstants.ERROR_MESSAGE_CONNECT_IDA_EXTERNAL_STORE_ERROR;
import static org.keycloak.protocol.oidc.ida.mappers.IdaConstants.ERROR_MESSAGE_IDA_EXTERNAL_STORE_JSON_SYNTAX_ERROR_ERROR;
import static org.keycloak.protocol.oidc.ida.mappers.IdaConstants.ERROR_MESSAGE_IDA_EXTERNAL_STORE_NOT_SPECIFIED;
import static org.keycloak.validate.validators.NotBlankValidator.MESSAGE_BLANK;
import static org.keycloak.validate.validators.UriValidator.MESSAGE_INVALID_URI;

/**
 * Connector that uses HTTP to retrieve validated claims from an external store
 */
public class IdaHttpConnector implements IdaConnector {
    private static final Logger logger = Logger.getLogger(IdaHttpConnector.class);
    public static final String IDA_EXTERNAL_STORE_HELP_TEXT = "The URI of external store used by IDA";
    public static final String ERROR_MESSAGE_INVALID_IDA_EXTERNAL_STORE = "Invalid URI of IDA external store.";

    @Override
    public void addIdaExternalStore(List<ProviderConfigProperty> configProperties) {
        ProviderConfigProperty nameProperty = new ProviderConfigProperty();
        nameProperty.setName(IDA_EXTERNAL_STORE_NAME);
        nameProperty.setLabel(IDA_EXTERNAL_STORE_LABEL);
        nameProperty.setType(ProviderConfigProperty.STRING_TYPE);
        nameProperty.setHelpText(IDA_EXTERNAL_STORE_HELP_TEXT);
        configProperties.add(nameProperty);
    }

    @Override
    public void validateIdaExternalStore(Map<String, String> protocolMapperConfig) throws ProtocolMapperConfigException {
        String externalStoreUrl = protocolMapperConfig.get(IDA_EXTERNAL_STORE_NAME);
        if (externalStoreUrl == null || externalStoreUrl.isEmpty()) {
            throw new ProtocolMapperConfigException(ERROR_MESSAGE_IDA_EXTERNAL_STORE_NOT_SPECIFIED, MESSAGE_BLANK);
        }
        try {
            new URI(externalStoreUrl).toURL();
        } catch (MalformedURLException | URISyntaxException e) {
            throw new ProtocolMapperConfigException(ERROR_MESSAGE_INVALID_IDA_EXTERNAL_STORE, MESSAGE_INVALID_URI, e);
        }
    }

    @Override
    public Map<String, Object> getVerifiedClaims(Map<String, String> protocolMapperConfig, String userId) {
        String externalStoreUrl = protocolMapperConfig.get(IDA_EXTERNAL_STORE_NAME);
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            SimpleHttp request = SimpleHttp.doGet(externalStoreUrl + "?userId=" + userId,
                    client);
            return request.asJson(Map.class);
        } catch (IOException e) {
            if (e instanceof UnknownHostException  || e instanceof HttpHostConnectException) {
                logger.errorf(e, ERROR_MESSAGE_CONNECT_IDA_EXTERNAL_STORE_ERROR + " IDA External Store='%s'", externalStoreUrl);
                throw new ErrorResponseException(OAuthErrorException.SERVER_ERROR, ERROR_MESSAGE_CONNECT_IDA_EXTERNAL_STORE_ERROR,
                        Response.Status.INTERNAL_SERVER_ERROR);
            } else if (e instanceof MismatchedInputException || e instanceof JsonParseException) {
                logger.errorf(e, ERROR_MESSAGE_IDA_EXTERNAL_STORE_JSON_SYNTAX_ERROR_ERROR);
                throw new ErrorResponseException(OAuthErrorException.SERVER_ERROR, ERROR_MESSAGE_IDA_EXTERNAL_STORE_JSON_SYNTAX_ERROR_ERROR,
                        Response.Status.INTERNAL_SERVER_ERROR);
            } else {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void close() {}
}
