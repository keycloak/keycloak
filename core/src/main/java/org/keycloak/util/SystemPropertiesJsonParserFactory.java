package org.keycloak.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Properties;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.io.IOContext;
import org.codehaus.jackson.map.MappingJsonFactory;
import org.codehaus.jackson.util.JsonParserDelegate;
import org.keycloak.common.util.StringPropertyReplacer;
import org.keycloak.common.util.SystemEnvProperties;

/**
 * Provides replacing of system properties for parsed values
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class SystemPropertiesJsonParserFactory extends MappingJsonFactory {

    private static final Properties properties = new SystemEnvProperties();

    @Override
    protected JsonParser _createJsonParser(byte[] data, int offset, int len, IOContext ctxt) throws IOException {
        JsonParser delegate = super._createJsonParser(data, offset, len, ctxt);
        return new SystemPropertiesAwareJsonParser(delegate);
    }

    @Override
    protected JsonParser _createJsonParser(Reader r, IOContext ctxt) throws IOException {
        JsonParser delegate = super._createJsonParser(r, ctxt);
        return new SystemPropertiesAwareJsonParser(delegate);
    }

    @Override
    protected JsonParser _createJsonParser(InputStream in, IOContext ctxt) throws IOException {
        JsonParser delegate = super._createJsonParser(in, ctxt);
        return new SystemPropertiesAwareJsonParser(delegate);
    }

    public static class SystemPropertiesAwareJsonParser extends JsonParserDelegate {

        public SystemPropertiesAwareJsonParser(JsonParser d) {
            super(d);
        }

        @Override
        public String getText() throws IOException {
            String orig = super.getText();
            return StringPropertyReplacer.replaceProperties(orig, properties);
        }
    }
}
