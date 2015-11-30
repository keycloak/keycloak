package org.keycloak.client.registration.cli;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.keycloak.client.registration.ClientRegistration;
import org.keycloak.util.SystemPropertiesJsonParserFactory;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class Context {

    private static final ObjectMapper mapper = new ObjectMapper(new SystemPropertiesJsonParserFactory());
    static {
        mapper.setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);
        mapper.enable(SerializationConfig.Feature.INDENT_OUTPUT);
    }

    private ClientRegistration reg;

    public ClientRegistration getReg() {
        return reg;
    }

    public void setReg(ClientRegistration reg) {
        this.reg = reg;
    }

    public static <T> T readJson(InputStream bytes, Class<T> type) throws IOException {
        return mapper.readValue(bytes, type);
    }

}
