/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.saml.processing.core.util;

import org.keycloak.saml.common.util.SecurityActions;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.xml.XMLConstants;
import javax.xml.validation.SchemaFactory;

import org.junit.Test;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;

public class IDFedLSInputResolverTest {

    @Test
    public void testSchemaConstruction() throws Exception {
         
        // make sure there is no outgoing call to get schema online; 
        // all resources must have a result for our resolver
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

        final IDFedLSInputResolver idFedLSInputResolver = new IDFedLSInputResolver();
        
        schemaFactory.setResourceResolver(new LSResourceResolver() {
            
            @Override
            public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId, String baseURI) {
                LSInput input = idFedLSInputResolver.resolveResource(type, namespaceURI, publicId, systemId, baseURI);
                
                if(input == null) {
                    throw new IllegalArgumentException("Unable to resolve " + systemId);
                }
                
                InputStream is = input.getByteStream();
                if(is == null) {
                    throw new IllegalArgumentException("Unable to resolve stream for " + systemId);
                }
                try {
                    is.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                
                return input;
            }
        });

        // check that all schema can be resolved
        for(String schema : SchemaManagerUtil.getSchemas()) {
            if(schema.contains("saml")) {
                URL schemaFile = SecurityActions.loadResource(getClass(), schema);
                schemaFactory.newSchema(schemaFile);
            }
        }
        
        JAXPValidationUtil.validator();
        
    }

}