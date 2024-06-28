package org.keycloak.saml.processing.core.util;

/**
 * Constants copied from XMLConstants to work around issues with IntelliJ
 *
 * See https://issues.redhat.com/browse/KEYCLOAK-19403
 */
public class FixXMLConstants {

    public static final String ACCESS_EXTERNAL_DTD = "http://javax.xml.XMLConstants/property/accessExternalDTD";

    public static final String ACCESS_EXTERNAL_SCHEMA = "http://javax.xml.XMLConstants/property/accessExternalSchema";

    public static final String ACCESS_EXTERNAL_STYLESHEET = "http://javax.xml.XMLConstants/property/accessExternalStylesheet";

}
