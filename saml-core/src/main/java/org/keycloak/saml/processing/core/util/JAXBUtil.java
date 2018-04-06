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
package org.keycloak.saml.processing.core.util;

import org.keycloak.saml.common.PicketLinkLogger;
import org.keycloak.saml.common.PicketLinkLoggerFactory;
import org.keycloak.saml.common.constants.GeneralConstants;

import org.keycloak.saml.common.util.SecurityActions;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;

/**
 * Utility to obtain JAXB2 marshaller/unmarshaller etc
 *
 * @author Anil.Saldhana@redhat.com
 * @since May 26, 2009
 */
public class JAXBUtil {

    private static final PicketLinkLogger logger = PicketLinkLoggerFactory.getLogger();

    public static final String W3C_XML_SCHEMA_NS_URI = "http://www.w3.org/2001/XMLSchema";

    private static final HashMap<String, JAXBContext> jaxbContextHash = new HashMap<String, JAXBContext>();

    static {
        // Useful on Sun VMs. Harmless on other VMs.
        SecurityActions.setSystemProperty("com.sun.xml.bind.v2.runtime.JAXBContextImpl.fastBoot", "true");
    }

    /**
     * Get the JAXB Marshaller
     *
     * @param pkgName The package name for the jaxb context
     * @param schemaLocation location of the schema to validate against
     *
     * @return Marshaller
     *
     * @throws JAXBException
     * @throws SAXException
     */
    public static Marshaller getValidatingMarshaller(String pkgName, String schemaLocation) throws JAXBException, SAXException {
        Marshaller marshaller = getMarshaller(pkgName);

        // Validate against schema
        Schema schema = getJAXPSchemaInstance(schemaLocation);
        marshaller.setSchema(schema);

        return marshaller;
    }

    /**
     * Get the JAXB Marshaller
     *
     * @param pkgName The package name for the jaxb context
     *
     * @return Marshaller
     *
     * @throws JAXBException
     */
    public static Marshaller getMarshaller(String pkgName) throws JAXBException {
        if (pkgName == null)
            throw logger.nullArgumentError("pkgName");

        JAXBContext jc = getJAXBContext(pkgName);
        Marshaller marshaller = jc.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_ENCODING, GeneralConstants.SAML_CHARSET_NAME);
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.FALSE); // Breaks signatures
        return marshaller;
    }

    /**
     * Get the JAXB Unmarshaller
     *
     * @param pkgName The package name for the jaxb context
     *
     * @return unmarshaller
     *
     * @throws JAXBException
     */
    public static Unmarshaller getUnmarshaller(String pkgName) throws JAXBException {
        if (pkgName == null)
            throw logger.nullArgumentError("pkgName");
        JAXBContext jc = getJAXBContext(pkgName);
        return jc.createUnmarshaller();
    }

    /**
     * Get the JAXB Unmarshaller for a selected set of package names
     *
     * @param pkgNames
     *
     * @return
     *
     * @throws JAXBException
     */
    public static Unmarshaller getUnmarshaller(String... pkgNames) throws JAXBException {
        if (pkgNames == null)
            throw logger.nullArgumentError("pkgName");
        int len = pkgNames.length;
        if (len == 0)
            return getUnmarshaller(pkgNames[0]);

        JAXBContext jc = getJAXBContext(pkgNames);
        return jc.createUnmarshaller();
    }

    /**
     * Get the JAXB Unmarshaller
     *
     * @param pkgName The package name for the jaxb context
     * @param schemaLocation location of the schema to validate against
     *
     * @return unmarshaller
     *
     * @throws JAXBException
     * @throws SAXException
     */
    public static Unmarshaller getValidatingUnmarshaller(String pkgName, String schemaLocation) throws JAXBException,
            SAXException {
        Unmarshaller unmarshaller = getUnmarshaller(pkgName);
        Schema schema = getJAXPSchemaInstance(schemaLocation);
        unmarshaller.setSchema(schema);

        return unmarshaller;
    }

    public static Unmarshaller getValidatingUnmarshaller(String[] pkgNames, String[] schemaLocations) throws JAXBException,
            SAXException, IOException {
        StringBuilder builder = new StringBuilder();
        int len = pkgNames.length;
        if (len == 0)
            throw logger.nullValueError("Packages are empty");

        for (String pkg : pkgNames) {
            builder.append(pkg);
            builder.append(":");
        }

        Unmarshaller unmarshaller = getUnmarshaller(builder.toString());

        SchemaFactory schemaFactory = getSchemaFactory();

        // Get the sources
        Source[] schemaSources = new Source[schemaLocations.length];

        int i = 0;
        for (String schemaLocation : schemaLocations) {
            URL schemaURL = SecurityActions.loadResource(JAXBUtil.class, schemaLocation);
            if (schemaURL == null)
                throw logger.nullValueError("Schema URL :" + schemaLocation);

            schemaSources[i++] = new StreamSource(schemaURL.openStream());
        }

        Schema schema = schemaFactory.newSchema(schemaSources);
        unmarshaller.setSchema(schema);

        return unmarshaller;
    }

    private static Schema getJAXPSchemaInstance(String schemaLocation) throws SAXException {
        URL schemaURL = SecurityActions.loadResource(JAXBUtil.class, schemaLocation);
        if (schemaURL == null)
            throw logger.nullValueError("Schema URL :" + schemaLocation);
        SchemaFactory scFact = getSchemaFactory();
        Schema schema = scFact.newSchema(schemaURL);
        return schema;
    }

    private static SchemaFactory getSchemaFactory() {
        SchemaFactory scFact = SchemaFactory.newInstance(W3C_XML_SCHEMA_NS_URI);

        // Always install the resolver unless the system property is set
        if (SecurityActions.getSystemProperty("org.picketlink.identity.federation.jaxb.ls", null) == null)
            scFact.setResourceResolver(new IDFedLSInputResolver());

        scFact.setErrorHandler(new ErrorHandler() {
            public void error(SAXParseException exception) throws SAXException {
                StringBuilder builder = new StringBuilder();
                builder.append("Line Number=").append(exception.getLineNumber());
                builder.append(" Col Number=").append(exception.getColumnNumber());
                builder.append(" Public ID=").append(exception.getPublicId());
                builder.append(" System ID=").append(exception.getSystemId());
                builder.append(" exc=").append(exception.getLocalizedMessage());

                logger.trace("SAX Error:" + builder.toString());
            }

            public void fatalError(SAXParseException exception) throws SAXException {
                StringBuilder builder = new StringBuilder();
                builder.append("Line Number=").append(exception.getLineNumber());
                builder.append(" Col Number=").append(exception.getColumnNumber());
                builder.append(" Public ID=").append(exception.getPublicId());
                builder.append(" System ID=").append(exception.getSystemId());
                builder.append(" exc=").append(exception.getLocalizedMessage());

                logger.error("SAX Fatal Error:" + builder.toString());
            }

            public void warning(SAXParseException exception) throws SAXException {
                StringBuilder builder = new StringBuilder();
                builder.append("Line Number=").append(exception.getLineNumber());
                builder.append(" Col Number=").append(exception.getColumnNumber());
                builder.append(" Public ID=").append(exception.getPublicId());
                builder.append(" System ID=").append(exception.getSystemId());
                builder.append(" exc=").append(exception.getLocalizedMessage());

                logger.trace("SAX Warn:" + builder.toString());
            }
        });
        return scFact;
    }

    public static JAXBContext getJAXBContext(String path) throws JAXBException {
        JAXBContext jx = jaxbContextHash.get(path);
        if (jx == null) {
            jx = JAXBContext.newInstance(path);
            jaxbContextHash.put(path, jx);
        }
        return jx;
    }

    public static JAXBContext getJAXBContext(String... paths) throws JAXBException {
        int len = paths.length;
        if (len == 0)
            return getJAXBContext(paths[0]);

        StringBuilder builder = new StringBuilder();
        for (String path : paths) {
            builder.append(path).append(":");
        }

        String finalPath = builder.toString();

        JAXBContext jx = jaxbContextHash.get(finalPath);
        if (jx == null) {
            jx = JAXBContext.newInstance(finalPath);
            jaxbContextHash.put(finalPath, jx);
        }
        return jx;
    }

    public static JAXBContext getJAXBContext(Class<?> clazz) throws JAXBException {
        String clazzName = clazz.getName();

        JAXBContext jx = jaxbContextHash.get(clazzName);
        if (jx == null) {
            jx = JAXBContext.newInstance(clazz);
            jaxbContextHash.put(clazzName, jx);
        }
        return jx;
    }
}