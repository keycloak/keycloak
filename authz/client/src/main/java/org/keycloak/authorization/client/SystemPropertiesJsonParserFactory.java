/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.authorization.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import org.keycloak.common.util.StringPropertyReplacer;
import org.keycloak.common.util.SystemEnvProperties;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.util.JsonParserDelegate;
import com.fasterxml.jackson.databind.MappingJsonFactory;

/**
 * Provides replacing of system properties for parsed values
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
class SystemPropertiesJsonParserFactory extends MappingJsonFactory {

    @Override
    protected JsonParser _createParser(InputStream in, IOContext ctxt) throws IOException {
        JsonParser delegate =  super._createParser(in, ctxt);
        return new SystemPropertiesAwareJsonParser(delegate);
    }

    @Override
    protected JsonParser _createParser(Reader r, IOContext ctxt) throws IOException {
        JsonParser delegate =  super._createParser(r, ctxt);
        return new SystemPropertiesAwareJsonParser(delegate);
    }

    @Override
    protected JsonParser _createParser(char[] data, int offset, int len, IOContext ctxt, boolean recyclable) throws IOException {
        JsonParser delegate =  super._createParser(data, offset, len, ctxt, recyclable);
        return new SystemPropertiesAwareJsonParser(delegate);
    }

    @Override
    protected JsonParser _createParser(byte[] data, int offset, int len, IOContext ctxt) throws IOException {
        JsonParser delegate =  super._createParser(data, offset, len, ctxt);
        return new SystemPropertiesAwareJsonParser(delegate);
    }

    public static class SystemPropertiesAwareJsonParser extends JsonParserDelegate {

        public SystemPropertiesAwareJsonParser(JsonParser d) {
            super(d);
        }

        @Override
        public String getText() throws IOException {
            String orig = super.getText();
            return StringPropertyReplacer.replaceProperties(orig, SystemEnvProperties.UNFILTERED::getProperty);
        }
    }
}
