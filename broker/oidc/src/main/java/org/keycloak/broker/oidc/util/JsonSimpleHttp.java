package org.keycloak.broker.oidc.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.keycloak.broker.provider.util.SimpleHttp;

import java.io.IOException;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class JsonSimpleHttp extends SimpleHttp {
    public JsonSimpleHttp(String url, String method) {
        super(url, method);
    }

    public static JsonSimpleHttp doGet(String url) {
        return new JsonSimpleHttp(url, "GET");
    }

    public static JsonSimpleHttp doPost(String url) {
        return new JsonSimpleHttp(url, "POST");
    }

    private static ObjectMapper mapper = new ObjectMapper();

    public static JsonNode asJson(SimpleHttp request) throws IOException {
        return mapper.readTree(request.asString());
    }

}
