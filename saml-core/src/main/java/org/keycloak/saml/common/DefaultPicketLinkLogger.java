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

package org.keycloak.saml.common;

import org.jboss.logging.Logger;
import org.keycloak.saml.common.constants.GeneralConstants;
import org.keycloak.saml.common.constants.WSTrustConstants;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.common.exceptions.TrustKeyConfigurationException;
import org.keycloak.saml.common.exceptions.TrustKeyProcessingException;
import org.keycloak.saml.common.exceptions.fed.AssertionExpiredException;
import org.keycloak.saml.common.exceptions.fed.IssueInstantMissingException;
import org.keycloak.saml.common.exceptions.fed.IssuerNotTrustedException;
import org.keycloak.saml.common.exceptions.fed.SignatureValidationException;
import org.keycloak.saml.common.exceptions.fed.WSTrustException;
import org.w3c.dom.Element;

import javax.security.auth.login.LoginException;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.stream.Location;
import javax.xml.ws.WebServiceException;
import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 *@author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 *
 */

/**@author <a href="mailto:psilva@redhat.com">Pedro Silva</a> */
public class DefaultPicketLinkLogger implements PicketLinkLogger {

    private Logger logger = Logger.getLogger(PicketLinkLogger.class.getPackage().getName());

    DefaultPicketLinkLogger() {

    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#info(java.lang.String)
     */
    @Override
    public void info(String message) {
        if (logger.isInfoEnabled()) {
            logger.info(message);
        }
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#debug(java.lang.String)
     */
    @Override
    public void debug(String message) {
        if (logger.isDebugEnabled()) {
            logger.debug(message);
        }
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#trace(java.lang.String)
     */
    @Override
    public void trace(String message) {
        if (logger.isTraceEnabled()) {
            logger.trace(message);
        }
    }

    /*
     *(non-Javadoc)
     *
     * @see org.picketlink.identity.federation.PicketLinkLogger#trace(java.lang.String, java.lang.Throwable)
     */
    @Override
    public void trace(String message, Throwable t) {
        if (logger.isTraceEnabled()) {
            logger.trace(message, t);
        }
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#trace(java.lang.Throwable)
     */
    @Override
    public void trace(Throwable t) {
        if (logger.isTraceEnabled()) {
            logger.trace(t.getMessage(), t);
        }
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#error(java.lang.Throwable)
     */
    @Override
    public void error(Throwable t) {
        logger.error("Unexpected error", t);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#nullArgument(java.lang.String)
     */
    @Override
    public IllegalArgumentException nullArgumentError(String argument) {
        return new IllegalArgumentException(ErrorCodes.NULL_ARGUMENT + argument);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#shouldNotBeTheSame(java.lang.String)
     */
    @Override
    public IllegalArgumentException shouldNotBeTheSameError(String string) {
        return new IllegalArgumentException(ErrorCodes.SHOULD_NOT_BE_THE_SAME
                + "Only one of isSigningKey and isEncryptionKey should be true");
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#resourceNotFound(java.lang.String)
     */
    @Override
    public ProcessingException resourceNotFound(String resource) {
        return new ProcessingException(ErrorCodes.RESOURCE_NOT_FOUND + resource + " could not be loaded");
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#processingError(java.lang.Throwable)
     */
    @Override
    public ProcessingException processingError(Throwable t) {
        return new ProcessingException(ErrorCodes.PROCESSING_EXCEPTION, t);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#unsupportedType(java.lang.String)
     */
    @Override
    public RuntimeException unsupportedType(String name) {
        return new RuntimeException(ErrorCodes.UNSUPPORTED_TYPE + name);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#signatureError(java.lang.Throwable)
     */
    @Override
    public XMLSignatureException signatureError(Throwable e) {
        return new XMLSignatureException(ErrorCodes.SIGNING_PROCESS_FAILURE, e);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#nullValue(java.lang.String)
     */
    @Override
    public RuntimeException nullValueError(String nullValue) {
        return new RuntimeException(ErrorCodes.NULL_VALUE + nullValue);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#notImplementedYet()
     */
    @Override
    public RuntimeException notImplementedYet(String feature) {
        return new RuntimeException(ErrorCodes.NOT_IMPLEMENTED_YET + feature);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#auditNullAuditManager()
     */
    @Override
    public IllegalStateException auditNullAuditManager() {
        return new IllegalStateException(ErrorCodes.AUDIT_MANAGER_NULL);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#isInfoEnabled()
     */
    @Override
    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#auditEvent(java.lang.String)
     */
    @Override
    public void auditEvent(String auditEvent) {
        this.info(auditEvent);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#injectedValueMissing(java.lang.String)
     */
    @Override
    public RuntimeException injectedValueMissing(String value) {
        return new RuntimeException(ErrorCodes.INJECTED_VALUE_MISSING + value);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#keystoreSetup()
     */
    @Override
    public void keyStoreSetup() {
        this.trace("getPublicKey::Keystore is null. so setting it up");
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#keyStoreNullStore()
     */
    @Override
    public IllegalStateException keyStoreNullStore() {
        return new IllegalStateException(ErrorCodes.KEYSTOREKEYMGR_NULL_KEYSTORE);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#keyStoreNullPublicKeyForAlias(java.lang.String)
     */
    @Override
    public void keyStoreNullPublicKeyForAlias(String alias) {
        this.trace("No public key found for alias=" + alias);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#keyStoreConfigurationError(java.lang.Throwable)
     */
    @Override
    public TrustKeyConfigurationException keyStoreConfigurationError(Throwable t) {
        return new TrustKeyConfigurationException(t);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#keyStoreProcessingError(java.lang.Throwable)
     */
    @Override
    public TrustKeyProcessingException keyStoreProcessingError(Throwable t) {
        return new TrustKeyProcessingException(t);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#keyStoreMissingDomainAlias(java.lang.String)
     */
    @Override
    public IllegalStateException keyStoreMissingDomainAlias(String domain) {
        return new IllegalStateException(ErrorCodes.KEYSTOREKEYMGR_DOMAIN_ALIAS_MISSING + domain);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#keyStoreNullSigningKeyPass()
     */
    @Override
    public RuntimeException keyStoreNullSigningKeyPass() {
        return new RuntimeException(ErrorCodes.KEYSTOREKEYMGR_NULL_SIGNING_KEYPASS);
    }

    @Override
    public RuntimeException keyStoreNullEncryptionKeyPass() {
        return new RuntimeException(ErrorCodes.KEYSTOREKEYMGR_NULL_ENCRYPTION_KEYPASS);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#keyStoreNotLocated(java.lang.String)
     */
    @Override
    public RuntimeException keyStoreNotLocated(String keyStore) {
        return new RuntimeException(ErrorCodes.KEYSTOREKEYMGR_KEYSTORE_NOT_LOCATED + keyStore);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#keyStoreNullAlias()
     */
    @Override
    public IllegalStateException keyStoreNullAlias() {
        return new IllegalStateException(ErrorCodes.KEYSTOREKEYMGR_NULL_ALIAS);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#parserUnknownEndElement(java.lang.String)
     */
    @Override
    public RuntimeException parserUnknownEndElement(String endElementName, Location location) {
        return new RuntimeException(ErrorCodes.UNKNOWN_END_ELEMENT + endElementName + "::location=" + location);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#parseUnknownTag(java.lang.String, javax.xml.stream.Location)
     */
    @Override
    public RuntimeException parserUnknownTag(String tag, Location location) {
        return new RuntimeException(ErrorCodes.UNKNOWN_TAG + tag + "::location=" + location);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#parseRequiredAttribute(java.lang.String)
     */
    @Override
    public ParsingException parserRequiredAttribute(String string) {
        return new ParsingException(ErrorCodes.REQD_ATTRIBUTE + string);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#parserUnknownStartElement(java.lang.String,
     *javax.xml.stream.Location)
     */
    @Override
    public RuntimeException parserUnknownStartElement(String elementName, Location location) {
        return new RuntimeException(ErrorCodes.UNKNOWN_START_ELEMENT + elementName + "::location=" + location);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#parserNullStartElement()
     */
    @Override
    public IllegalStateException parserNullStartElement() {
        return new IllegalStateException(ErrorCodes.NULL_START_ELEMENT);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#parserUnknownXSI(java.lang.String)
     */
    @Override
    public ParsingException parserUnknownXSI(String xsiTypeValue) {
        return new ParsingException(ErrorCodes.UNKNOWN_XSI + xsiTypeValue);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#parserExpectedEndTag(java.lang.String)
     */
    @Override
    public ParsingException parserExpectedEndTag(String tagName) {
        return new ParsingException(ErrorCodes.EXPECTED_END_TAG + tagName);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#parserException(java.lang.Exception)
     */
    @Override
    public ParsingException parserException(Throwable t) {
        return new ParsingException(t);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#parserExpectedTextValue(java.lang.String)
     */
    @Override
    public ParsingException parserExpectedTextValue(String string) {
        return new ParsingException(ErrorCodes.EXPECTED_TEXT_VALUE + "SigningAlias");
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#parserExpectedXSI(java.lang.String)
     */
    @Override
    public RuntimeException parserExpectedXSI(String expectedXsi) {
        return new RuntimeException(expectedXsi);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#parserExpectedTag(java.lang.String, java.lang.String)
     */
    @Override
    public RuntimeException parserExpectedTag(String tag, String foundElementTag) {
        return new RuntimeException(ErrorCodes.EXPECTED_TAG + tag + ".  Found <" + foundElementTag + ">");
    }

    @Override
    public RuntimeException parserExpectedNamespace(String ns, String foundElementNs) {
        return new RuntimeException(ErrorCodes.EXPECTED_NAMESPACE + ns + ">.  Found <" + foundElementNs + ">");
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#parserFailed()
     */
    @Override
    public RuntimeException parserFailed(String elementName) {
        return new RuntimeException(ErrorCodes.FAILED_PARSING + elementName);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#parserUnableParsingNullToken()
     */
    @Override
    public ParsingException parserUnableParsingNullToken() {
        return new ParsingException(ErrorCodes.UNABLE_PARSING_NULL_TOKEN);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#parserError(java.lang.Exception)
     */
    @Override
    public ParsingException parserError(Throwable t) {
        return new ParsingException(ErrorCodes.PARSING_ERROR + t.getMessage(), t);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#pdpMessageProcessingError(java.lang.Exception)
     */
    @Override
    public RuntimeException xacmlPDPMessageProcessingError(Throwable t) {
        return new RuntimeException(t);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#fileNotLocated(java.lang.String)
     */
    @Override
    public IllegalStateException fileNotLocated(String policyConfigFileName) {
        return new IllegalStateException(ErrorCodes.FILE_NOT_LOCATED + policyConfigFileName);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#optionNotSet(java.lang.String)
     */
    @Override
    public IllegalStateException optionNotSet(String option) {
        return new IllegalStateException(ErrorCodes.OPTION_NOT_SET + option);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#securityTokenRegistryNotSpecified()
     */
    @Override
    public void stsTokenRegistryNotSpecified() {
        this.warn("Security Token registry option not specified: Issued Tokens will not be persisted!");
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#securityTokenRegistryInvalidType(java.lang.String)
     */
    @Override
    public void stsTokenRegistryInvalidType(String tokenRegistryOption) {
        logger.warn(tokenRegistryOption + " is not an instance of SecurityTokenRegistry - using default registry");
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#securityTokenRegistryInstantiationError()
     */
    @Override
    public void stsTokenRegistryInstantiationError() {
        logger.warn("Error instantiating token registry class - using default registry");
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#revocationRegistryNotSpecified()
     */
    @Override
    public void stsRevocationRegistryNotSpecified() {
        this.debug("Revocation registry option not specified: cancelled ids will not be persisted!");
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#revocationRegistryInvalidType(java.lang.String)
     */
    @Override
    public void stsRevocationRegistryInvalidType(String registryOption) {
        logger.warn(registryOption + " is not an instance of RevocationRegistry - using default registry");
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#revocationRegistryInstantiationError()
     */
    @Override
    public void stsRevocationRegistryInstantiationError() {
        logger.warn("Error instantiating revocation registry class - using default registry");
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#assertionExpiredError()
     */
    @Override
    public ProcessingException samlAssertionExpiredError() {
        return new ProcessingException(ErrorCodes.EXPIRED_ASSERTION);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#assertionInvalidError()
     */
    @Override
    public ProcessingException assertionInvalidError() {
        return new ProcessingException(ErrorCodes.INVALID_ASSERTION);
    }

    @Override
    public RuntimeException writerUnknownTypeError(String name) {
        return new RuntimeException(ErrorCodes.WRITER_UNKNOWN_TYPE + name);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#writerNullValueError(java.lang.String)
     */
    @Override
    public ProcessingException writerNullValueError(String value) {
        return new ProcessingException(ErrorCodes.WRITER_NULL_VALUE + value);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#writerUnsupportedAttributeValueError(java.lang.String)
     */
    @Override
    public RuntimeException writerUnsupportedAttributeValueError(String value) {
        return new RuntimeException(ErrorCodes.WRITER_UNSUPPORTED_ATTRIB_VALUE + value);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#issuerInfoMissingStatusCodeError()
     */
    @Override
    public IllegalArgumentException issuerInfoMissingStatusCodeError() {
        return new IllegalArgumentException(ErrorCodes.ISSUER_INFO_MISSING_STATUS_CODE);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#classNotLoadedError(java.lang.String)
     */
    @Override
    public ProcessingException classNotLoadedError(String fqn) {
        return new ProcessingException(ErrorCodes.CLASS_NOT_LOADED + fqn);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#couldNotCreateInstance(java.lang.String, java.lang.Exception)
     */
    @Override
    public ProcessingException couldNotCreateInstance(String fqn, Throwable t) {
        return new ProcessingException(ErrorCodes.CANNOT_CREATE_INSTANCE + fqn, t);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#systemPropertyMissingError(java.lang.String)
     */
    @Override
    public RuntimeException systemPropertyMissingError(String property) {
        return new RuntimeException(ErrorCodes.SYSTEM_PROPERTY_MISSING + property);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#metaDataIdentityProviderLoadingError(java.lang.Exception)
     */
    @Override
    public void samlMetaDataIdentityProviderLoadingError(Throwable t) {
        logger.error("Exception loading the identity providers:", t);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#metaDataServiceProviderLoadingError(java.lang.Throwable)
     */
    @Override
    public void samlMetaDataServiceProviderLoadingError(Throwable t) {
        logger.error("Exception loading the service providers:", t);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#signatureAssertionValidationError(java.lang.Exception)
     */
    @Override
    public void signatureAssertionValidationError(Throwable t) {
        logger.error("Cannot validate signature of assertion", t);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#assertionExpired(java.lang.String)
     */
    @Override
    public void samlAssertionExpired(String id) {
        this.info("Assertion has expired with id=" + id);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#unknownObjectType(java.lang.Object)
     */
    @Override
    public RuntimeException unknownObjectType(Object attrValue) {
        return new RuntimeException(ErrorCodes.UNKNOWN_OBJECT_TYPE + attrValue);
    }

    /*
     *(non-Javadoc)
     *
     *@see
     *org.picketlink.identity.federation.PicketLinkLogger#configurationError(javax.xml.parsers.ParserConfigurationException)
     */
    @Override
    public ConfigurationException configurationError(Throwable t) {
        return new ConfigurationException(t);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#signatureUnknownAlgo(java.lang.String)
     */
    @Override
    public RuntimeException signatureUnknownAlgo(String algo) {
        return new RuntimeException(ErrorCodes.UNKNOWN_SIG_ALGO + algo);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#invalidArgumentError(java.lang.String)
     */
    @Override
    public IllegalArgumentException invalidArgumentError(String message) {
        return new IllegalArgumentException(message);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#stsNoTokenProviderError(java.lang.String)
     */
    @Override
    public ProcessingException stsNoTokenProviderError(String configuration, String protocolContext) {
        return new ProcessingException(ErrorCodes.STS_NO_TOKEN_PROVIDER + configuration + "][ProtoCtx=" + protocolContext + "]");
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#stsConfigurationFileNotFoundTCL(java.lang.String)
     */
    @Override
    public void stsConfigurationFileNotFoundTCL(String fileName) {
        logger.warn(fileName + " configuration file not found using TCCL");
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#stsConfigurationFileNotFoundClassLoader(java.lang.String)
     */
    @Override
    public void stsConfigurationFileNotFoundClassLoader(String fileName) {
        logger.warn(fileName + " configuration file not found using class loader");
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#stsUsingDefaultConfiguration(java.lang.String)
     */
    @Override
    public void stsUsingDefaultConfiguration(String fileName) {
        logger.warn(fileName + " configuration file not found using URL. Using default configuration values");
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#stsConfigurationFileLoaded(java.lang.String)
     */
    @Override
    public void stsConfigurationFileLoaded(String fileName) {
        this.info(fileName + " configuration file loaded");
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#stsConfigurationFileParsingError(java.lang.Throwable)
     */
    @Override
    public ConfigurationException stsConfigurationFileParsingError(Throwable t) {
        return new ConfigurationException(ErrorCodes.STS_CONFIGURATION_FILE_PARSING_ERROR, t);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#notSerializableError(java.lang.String)
     */
    @Override
    public IOException notSerializableError(String message) {
        return new IOException(ErrorCodes.NOT_SERIALIZABLE + message);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#trustKeyCreationError()
     */
    @Override
    public void trustKeyManagerCreationError(Throwable t) {
        logger.error("Exception creating TrustKeyManager:", t);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#error(java.lang.String)
     */
    @Override
    public void error(String message) {
        logger.error(message);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#couldNotGetXMLSchema(java.lang.Throwable)
     */
    @Override
    public void xmlCouldNotGetSchema(Throwable t) {
        logger.error("Cannot get schema", t);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#isTraceEnabled()
     */
    @Override
    public boolean isTraceEnabled() {
        return logger.isTraceEnabled();
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#isDebugEnabled()
     */
    @Override
    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#jceProviderCouldNotBeLoaded(java.lang.Throwable)
     */
    @Override
    public void jceProviderCouldNotBeLoaded(String name, Throwable t) {
        logger.debug("The provider " + name + " could not be added: ", t);
        logger.debug("Check addJceProvider method of org.picketlink.identity.federation.core.util.ProvidersUtil for more info.");
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#writerInvalidKeyInfoNullContent()
     */
    @Override
    public ProcessingException writerInvalidKeyInfoNullContentError() {
        return new ProcessingException(ErrorCodes.WRITER_INVALID_KEYINFO_NULL_CONTENT);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#notEqualError(java.lang.String, java.lang.String)
     */
    @Override
    public RuntimeException notEqualError(String first, String second) {
        return new RuntimeException(ErrorCodes.NOT_EQUAL + first + " and " + second);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#wrongTypeError(java.lang.String)
     */
    @Override
    public IllegalArgumentException wrongTypeError(String message) {
        return new IllegalArgumentException(ErrorCodes.WRONG_TYPE + "xmlSource should be a stax source");
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#encryptUnknownAlgoError(java.lang.String)
     */
    @Override
    public RuntimeException encryptUnknownAlgoError(String certAlgo) {
        return new RuntimeException(ErrorCodes.UNKNOWN_ENC_ALGO + certAlgo);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#domMissingDocElementError(java.lang.String)
     */
    @Override
    public IllegalStateException domMissingDocElementError(String element) {
        return new IllegalStateException(ErrorCodes.DOM_MISSING_DOC_ELEMENT + element);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#domMissingElementError(java.lang.String)
     */
    @Override
    public IllegalStateException domMissingElementError(String element) {
        return new IllegalStateException(ErrorCodes.DOM_MISSING_ELEMENT + element);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#stsWSInvalidTokenRequestError()
     */
    @Override
    public WebServiceException stsWSInvalidTokenRequestError() {
        return new WebServiceException(ErrorCodes.STS_INVALID_TOKEN_REQUEST);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#stsWSError(java.lang.Throwable)
     */
    @Override
    public WebServiceException stsWSError(Throwable t) {
        return new WebServiceException("Security Token Service Exception", t);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#stsWSConfigurationError(java.lang.Throwable)
     */
    @Override
    public WebServiceException stsWSConfigurationError(Throwable t) {
        return new WebServiceException(ErrorCodes.STS_CONFIGURATION_EXCEPTION, t);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#stsWSInvalidRequestTypeError(java.lang.String)
     */
    @Override
    public WSTrustException stsWSInvalidRequestTypeError(String requestType) {
        return new WSTrustException(ErrorCodes.STS_INVALID_REQUEST_TYPE + requestType);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#stsWSHandlingTokenRequestError(java.lang.Throwable)
     */
    @Override
    public WebServiceException stsWSHandlingTokenRequestError(Throwable t) {
        return new WebServiceException(ErrorCodes.STS_EXCEPTION_HANDLING_TOKEN_REQ + t.getMessage(), t);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#stsWSResponseWritingError(java.lang.Throwable)
     */
    @Override
    public WebServiceException stsWSResponseWritingError(Throwable t) {
        return new WebServiceException(ErrorCodes.STS_RESPONSE_WRITING_ERROR + t.getMessage(), t);
    }

    @Override
    public RuntimeException stsUnableToConstructKeyManagerError(Throwable t) {
        return new RuntimeException(ErrorCodes.STS_UNABLE_TO_CONSTRUCT_KEYMGR, t);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#stsPublicKeyError(java.lang.String, java.lang.Throwable)
     */
    @Override
    public RuntimeException stsPublicKeyError(String serviceName, Throwable t) {
        return new RuntimeException(ErrorCodes.STS_PUBLIC_KEY_ERROR + serviceName, t);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#stsSigningKeyPairError(java.lang.Exception)
     */
    @Override
    public RuntimeException stsSigningKeyPairError(Throwable t) {
        return new RuntimeException(ErrorCodes.STS_SIGNING_KEYPAIR_ERROR, t);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#stsPublicKeyCertError(java.lang.Throwable)
     */
    @Override
    public RuntimeException stsPublicKeyCertError(Throwable t) {
        return new RuntimeException(ErrorCodes.STS_PUBLIC_KEY_CERT, t);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#tokenTimeoutNotSpecified()
     */
    @Override
    public void stsTokenTimeoutNotSpecified() {
        this.warn("Lifetime has not been specified. Using the default timeout value.");
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#stsCombinedSecretKeyError(java.lang.Throwable)
     */
    @Override
    public WSTrustException wsTrustCombinedSecretKeyError(Throwable t) {
        return new WSTrustException(ErrorCodes.STS_COMBINED_SECRET_KEY_ERROR, t);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#stsClientPublicKeyError()
     */
    @Override
    public WSTrustException wsTrustClientPublicKeyError() {
        return new WSTrustException(ErrorCodes.STS_CLIENT_PUBLIC_KEY_ERROR);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#stsError(java.lang.Throwable)
     */
    @Override
    public WSTrustException stsError(Throwable t) {
        return new WSTrustException(t.getMessage(), t);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#signatureInvalidError(java.lang.String, java.lang.Throwable)
     */
    @Override
    public XMLSignatureException signatureInvalidError(String message, Throwable t) {
        return new XMLSignatureException(ErrorCodes.INVALID_DIGITAL_SIGNATURE + message);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#stsSecurityTokenSignatureNotVerified()
     */
    @Override
    public void stsSecurityTokenSignatureNotVerified() {
        this.warn("Security Token digital signature has NOT been verified. Either the STS has been configured"
                + "not to sign tokens or the STS key pair has not been properly specified.");
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#encryptProcessError(java.lang.Throwable)
     */
    @Override
    public RuntimeException encryptProcessError(Throwable t) {
        return new RuntimeException(t);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#stsSecurityTokenShouldBeEncrypted()
     */
    @Override
    public void stsSecurityTokenShouldBeEncrypted() {
        logger.warn("Security token should be encrypted but no encrypting key could be found");
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#stsUnableToDecodePasswordError(java.lang.String)
     */
    @Override
    public RuntimeException unableToDecodePasswordError(String password) {
        return new RuntimeException(ErrorCodes.PROCESSING_EXCEPTION + "Unable to decode password:" + password);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#couldNotLoadProperties(java.lang.String)
     */
    @Override
    public IllegalStateException couldNotLoadProperties(String configFile) {
        return new IllegalStateException(ErrorCodes.PROCESSING_EXCEPTION + "Could not load properties from " + configFile);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#stsKeyInfoTypeCreationError(java.lang.Throwable)
     */
    @Override
    public WSTrustException stsKeyInfoTypeCreationError(Throwable t) {
        return new WSTrustException(ErrorCodes.PROCESSING_EXCEPTION + "Error creating KeyInfoType", t);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#stsSecretKeyNotEncrypted()
     */
    @Override
    public void stsSecretKeyNotEncrypted() {
        logger.warn("Secret key could not be encrypted because the endpoint's PKC has not been specified");
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#authCouldNotIssueSAMLToken()
     */
    @Override
    public LoginException authCouldNotIssueSAMLToken() {
        return new LoginException(ErrorCodes.PROCESSING_EXCEPTION + "Could not issue a SAML Security Token");
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#authLoginError(java.lang.Throwable)
     */
    @Override
    public LoginException authLoginError(Throwable t) {
        LoginException loginException = new LoginException("Error during login/authentication");

        loginException.initCause(t);

        return loginException;
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#authCouldNotCreateWSTrustClient(java.lang.Throwable)
     */
    @Override
    public IllegalStateException authCouldNotCreateWSTrustClient(Throwable t) {
        return new IllegalStateException(ErrorCodes.PROCESSING_EXCEPTION + "Could not create WSTrustClient:", t);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#authSAMLAssertionWithoutExpiration(java.lang.String)
     */
    @Override
    public void samlAssertionWithoutExpiration(String id) {
        logger.warn("SAML Assertion has been found to have no expiration: ID = " + id);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#authCouldNotValidateSAMLToken(org.w3c.dom.Element)
     */
    @Override
    public LoginException authCouldNotValidateSAMLToken(Element token) {
        return new LoginException(ErrorCodes.PROCESSING_EXCEPTION + "Could not validate the SAML Security Token :" + token);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#authCouldNotLocateSecurityToken()
     */
    @Override
    public LoginException authCouldNotLocateSecurityToken() {
        return new LoginException(ErrorCodes.NULL_VALUE + "Could not locate a Security Token from the callback.");
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#wsTrustNullCancelTargetError()
     */
    @Override
    public ProcessingException wsTrustNullCancelTargetError() {
        return new ProcessingException(ErrorCodes.NULL_VALUE + "Invalid cancel request: missing required CancelTarget");
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#saml11MarshallError(java.lang.Throwable)
     */
    @Override
    public ProcessingException samlAssertionMarshallError(Throwable t) {
        return new ProcessingException(ErrorCodes.PROCESSING_EXCEPTION + "Failed to marshall assertion", t);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#wsTrustNullRenewTargetError()
     */
    @Override
    public ProcessingException wsTrustNullRenewTargetError() {
        return new ProcessingException(ErrorCodes.NULL_VALUE + "Invalid renew request: missing required RenewTarget");
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#saml11UnmarshallError(java.lang.Throwable)
     */
    @Override
    public ProcessingException samlAssertionUnmarshallError(Throwable t) {
        return new ProcessingException(ErrorCodes.PROCESSING_EXCEPTION + "Error unmarshalling assertion", t);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#samlAssertionRevokedCouldNotRenew()
     */
    @Override
    public ProcessingException samlAssertionRevokedCouldNotRenew(String id) {
        return new ProcessingException(ErrorCodes.ASSERTION_RENEWAL_EXCEPTION + "SAMLV1.1 Assertion with id " + id
                + " has been canceled and cannot be renewed");
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#wsTrustNullValidationTargetError()
     */
    @Override
    public ProcessingException wsTrustNullValidationTargetError() {
        return new ProcessingException(ErrorCodes.NULL_VALUE + "Bad validate request: missing required ValidateTarget");
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#stsWrongAttributeProviderTypeNotInstalled(java.lang.String)
     */
    @Override
    public void stsWrongAttributeProviderTypeNotInstalled(String attributeProviderClassName) {
        logger.warn("Attribute provider not installed: " + attributeProviderClassName
                + "is not an instance of SAML20TokenAttributeProvider");
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#stsAttributeProviderInstationError(java.lang.Throwable)
     */
    @Override
    public void attributeProviderInstationError(Throwable t) {
        logger.warn("Error instantiating attribute provider: " + t);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#samlAssertion(java.lang.String)
     */
    @Override
    public void samlAssertion(String nodeAsString) {
        trace("SAML Assertion Element=" + nodeAsString);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#wsTrustUnableToGetDataTypeFactory(javax.xml.datatype.
     *DatatypeConfigurationException)
     */
    @Override
    public RuntimeException wsTrustUnableToGetDataTypeFactory(Throwable t) {
        return new RuntimeException(ErrorCodes.PROCESSING_EXCEPTION + "Unable to get DatatypeFactory instance", t);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#wsTrustValidationStatusCodeMissing()
     */
    @Override
    public ProcessingException wsTrustValidationStatusCodeMissing() {
        return new ProcessingException(ErrorCodes.NULL_VALUE + "Validation status code is missing");
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#identityServerActiveSessionCount(int)
     */
    @Override
    public void samlIdentityServerActiveSessionCount(int activeSessionCount) {
        info("Active Session Count=" + activeSessionCount);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#identityServerSessionCreated(java.lang.String, int)
     */
    @Override
    public void samlIdentityServerSessionCreated(String id, int activeSessionCount) {
        trace("Session Created with id=" + id + "::active session count=" + activeSessionCount);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#identityServerSessionDestroyed(java.lang.String, int)
     */
    @Override
    public void samlIdentityServerSessionDestroyed(String id, int activeSessionCount) {
        trace("Session Destroyed with id=" + id + "::active session count=" + activeSessionCount);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#unknowCredentialType(java.lang.String)
     */
    @Override
    public RuntimeException unknowCredentialType(String name) {
        return new RuntimeException(ErrorCodes.UNSUPPORTED_TYPE + "Unknown credential type:" + name);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#samlHandlerRoleGeneratorSetupError(java.lang.Throwable)
     */
    @Override
    public void samlHandlerRoleGeneratorSetupError(Throwable t) {
        logger.error("Exception initializing role generator:", t);
    }

    @Override
    public RuntimeException samlHandlerAssertionNotFound() {
        return new RuntimeException(ErrorCodes.NULL_VALUE + "Assertion not found in the handler request");
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#samlHandlerAuthnRequestIsNull()
     */
    @Override
    public ProcessingException samlHandlerAuthnRequestIsNull() {
        return new ProcessingException(ErrorCodes.NULL_VALUE + "AuthnRequest is null");
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#samlHandlerAuthenticationError(java.lang.Throwable)
     */
    @Override
    public void samlHandlerAuthenticationError(Throwable t) {
        logger.error("Exception in processing authentication:", t);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#samlHandlerNoAssertionFromIDP()
     */
    @Override
    public IllegalArgumentException samlHandlerNoAssertionFromIDP() {
        return new IllegalArgumentException(ErrorCodes.NULL_VALUE + "No assertions in reply from IDP");
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#samlHandlerNullEncryptedAssertion()
     */
    @Override
    public ProcessingException samlHandlerNullEncryptedAssertion() {
        return new ProcessingException(ErrorCodes.NULL_VALUE + "Null encrypted assertion element");
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#samlHandlerIDPAuthenticationFailedError()
     */
    @Override
    public SecurityException samlHandlerIDPAuthenticationFailedError() {
        return new SecurityException(ErrorCodes.IDP_AUTH_FAILED + "IDP forbid the user");
    }

    /*
     *(non-Javadoc)
     *
     *@see
     *org.picketlink.identity.federation.PicketLinkLogger#assertionExpiredError(org.picketlink.identity.federation.core.saml
     *.v2.exceptions.AssertionExpiredException)
     */
    @Override
    public ProcessingException assertionExpiredError(AssertionExpiredException aee) {
        return new ProcessingException(ErrorCodes.EXPIRED_ASSERTION + "Assertion has expired", aee);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#unsupportedRoleType(java.lang.Object)
     */
    @Override
    public RuntimeException unsupportedRoleType(Object attrValue) {
        return new RuntimeException(ErrorCodes.UNSUPPORTED_TYPE + "Unknown role object type : " + attrValue);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#samlHandlerFailedInResponseToVerification(java.lang.String,
     *java.lang.String)
     */
    @Override
    public void samlHandlerFailedInResponseToVerification(String inResponseTo, String authnRequestId) {
        trace("Verification of InResponseTo failed. InResponseTo from SAML response is " + inResponseTo
                + ". Value of request Id from HTTP session is " + authnRequestId);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#samlHandlerFailedInResponseToVerificarionError()
     */
    @Override
    public ProcessingException samlHandlerFailedInResponseToVerificarionError() {
        return new ProcessingException(ErrorCodes.AUTHN_REQUEST_ID_VERIFICATION_FAILED);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#samlHandlerIssuerNotTrustedError(java.lang.String)
     */
    @Override
    public IssuerNotTrustedException samlIssuerNotTrustedError(String issuer) {
        return new IssuerNotTrustedException("Issuer not Trusted: " + issuer);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#samlHandlerIssuerNotTrustedError(java.lang.Throwable)
     */
    @Override
    public IssuerNotTrustedException samlIssuerNotTrustedException(Throwable t) {
        return new IssuerNotTrustedException(t);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#samlHandlerTrustElementMissingError()
     */
    @Override
    public ConfigurationException samlHandlerTrustElementMissingError() {
        return new ConfigurationException(ErrorCodes.NULL_VALUE + "trust element missing");
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#samlHandlerIdentityServerNotFound()
     */
    @Override
    public ProcessingException samlHandlerIdentityServerNotFoundError() {
        return new ProcessingException(ErrorCodes.NULL_VALUE + "Identity Server not found");
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#samlHandlerPrincipalNotFoundError()
     */
    @Override
    public ProcessingException samlHandlerPrincipalNotFoundError() {
        return new ProcessingException(ErrorCodes.PRINCIPAL_NOT_FOUND);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#samlHandlerKeyPairNotFound()
     */
    @Override
    public void samlHandlerKeyPairNotFound() {
        trace("Key Pair cannot be found");
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#samlHandlerKeyPairNotFoundError()
     */
    @Override
    public ProcessingException samlHandlerKeyPairNotFoundError() {
        return new ProcessingException("Key Pair cannot be found");
    }

    /*
     *(non-Javadoc)
     *
     *@see
     *org.picketlink.identity.federation.PicketLinkLogger#samlHandlerErrorSigningRedirectBindingMessage(java.lang.Throwable)
     */
    @Override
    public void samlHandlerErrorSigningRedirectBindingMessage(Throwable t) {
        logger.error("Error when trying to sign message for redirection", t);
    }

    /*
     *(non-Javadoc)
     *
     *@see
     *org.picketlink.identity.federation.PicketLinkLogger#samlHandlerSigningRedirectBindingMessageError(org.picketlink.identity
     *.federation.core.exceptions.ConfigurationException)
     */
    @Override
    public RuntimeException samlHandlerSigningRedirectBindingMessageError(Throwable t) {
        return new RuntimeException(t);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#signatureValidationError()
     */
    @Override
    public SignatureValidationException samlHandlerSignatureValidationFailed() {
        return new SignatureValidationException(ErrorCodes.INVALID_DIGITAL_SIGNATURE + "Signature Validation Failed");
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#samlHandlerErrorValidatingSignature(java.lang.Throwable)
     */
    @Override
    public void samlHandlerErrorValidatingSignature(Throwable t) {
        logger.error("Error validating signature:", t);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#samlHandlerInvalidSignatureError()
     */
    @Override
    public ProcessingException samlHandlerInvalidSignatureError() {
        return new ProcessingException(ErrorCodes.INVALID_DIGITAL_SIGNATURE + "Error validating signature.");
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#samlHandlerSignatureNorPresentError()
     */
    @Override
    public ProcessingException samlHandlerSignatureNotPresentError() {
        return new ProcessingException(ErrorCodes.INVALID_DIGITAL_SIGNATURE
                + "Signature Validation failed. Signature is not present. Check if the IDP is supporting signatures.");
    }

    @Override
    public ProcessingException samlHandlerSignatureValidationError(Throwable t) {
        return new ProcessingException(ErrorCodes.INVALID_DIGITAL_SIGNATURE + "Signature Validation failed", t);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#samlHandlerChainProcessingError(java.lang.Throwable)
     */
    @Override
    public RuntimeException samlHandlerChainProcessingError(Throwable t) {
        return new RuntimeException("Error during processing the SAML Handler Chain.", t);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#trustKeyManagerMissing()
     */
    @Override
    public TrustKeyConfigurationException trustKeyManagerMissing() {
        return new TrustKeyConfigurationException(ErrorCodes.TRUST_MANAGER_MISSING);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#samlBase64DecodingError(java.lang.Throwable)
     */
    @Override
    public void samlBase64DecodingError(Throwable t) {
        logger.error("Error in base64 decoding saml message.", t);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#samlParsingError(java.lang.Throwable)
     */
    @Override
    public void samlParsingError(Throwable t) {
        logger.error("Exception in parsing saml message:", t);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#attributeManagerMappingContextNull()
     */
    @Override
    public void mappingContextNull() {
        logger.error("Mapping Context returned is null");
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#attributeManagerError(java.lang.Throwable)
     */
    @Override
    public void attributeManagerError(Throwable t) {
        logger.error("Exception in attribute mapping:", t);
    }

    @Override
    public void couldNotObtainSecurityContext() {
        logger.error("Could not obtain security context.");
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#authFailedToCreatePrincipal(java.lang.Throwable)
     */
    @Override
    public LoginException authFailedToCreatePrincipal(Throwable t) {
        LoginException loginException = new LoginException(ErrorCodes.PROCESSING_EXCEPTION + "Failed to create principal: "
                + t.getMessage());

        loginException.initCause(t);

        return loginException;
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#authSharedCredentialIsNotSAMLCredential()
     */
    @Override
    public LoginException authSharedCredentialIsNotSAMLCredential(String className) {
        return new LoginException(ErrorCodes.WRONG_TYPE
                + "SAML2STSLoginModule: Shared credential is not a SAML credential. Got " + className);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#authSTSConfigFileNotFound()
     */
    @Override
    public LoginException authSTSConfigFileNotFound() {
        return new LoginException(ErrorCodes.SAML2STSLM_CONF_FILE_MISSING);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#authErrorHandlingCallback(java.lang.Throwable)
     */
    @Override
    public LoginException authErrorHandlingCallback(Throwable t) {
        LoginException loginException = new LoginException("Error handling callback.");

        loginException.initCause(t);

        return loginException;
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#authInvalidSAMLAssertionBySTS()
     */
    @Override
    public LoginException authInvalidSAMLAssertionBySTS() {
        return new LoginException(ErrorCodes.INVALID_ASSERTION
                + "SAML2STSLoginModule: Supplied assertion was considered invalid by the STS");
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#authAssertionValidationValies(java.lang.Throwable)
     */
    @Override
    public LoginException authAssertionValidationError(Throwable t) {
        LoginException loginException = new LoginException("Failed to validate assertion using STS");

        loginException.initCause(t);

        return loginException;
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#authFailedToParseSAMLAssertion(java.lang.Throwable)
     */
    @Override
    public LoginException authFailedToParseSAMLAssertion(Throwable t) {
        LoginException exception = new LoginException("PL00044: SAML2STSLoginModule: Failed to parse assertion element:"
                + t.getMessage());
        exception.initCause(t);
        return exception;
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#authSAMLAssertionPasingFailed(java.lang.Throwable)
     */
    @Override
    public void samlAssertionPasingFailed(Throwable t) {
        logger.error("SAML Assertion parsing failed", t);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#authNullKeyStoreFromSecurityDomainError(java.lang.String)
     */
    @Override
    public LoginException authNullKeyStoreFromSecurityDomainError(String name) {
        return new LoginException(ErrorCodes.NULL_VALUE + "SAML2STSLoginModule: null truststore for " + name);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#authNullKeyStoreAliasFromSecurityDomain(java.lang.String)
     */
    @Override
    public LoginException authNullKeyStoreAliasFromSecurityDomainError(String name) {
        return new LoginException(ErrorCodes.NULL_VALUE + "SAML2STSLoginModule: null KeyStoreAlias for " + name
                + "; set 'KeyStoreAlias' in '" + name + "' security domain configuration");
    }

    @Override
    public LoginException authNoCertificateFoundForAliasError(String alias, String name) {
        return new LoginException(ErrorCodes.NULL_VALUE + "No certificate found for alias '" + alias + "' in the '" + name
                + "' security domain");
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#authSAMLInvalidSignature()
     */
    @Override
    public LoginException authSAMLInvalidSignatureError() {
        return new LoginException(ErrorCodes.INVALID_DIGITAL_SIGNATURE + "SAML2STSLoginModule: "
                + WSTrustConstants.STATUS_CODE_INVALID + " : invalid SAML V2.0 assertion signature");
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#authSAMLAssertionExpiredError()
     */
    @Override
    public LoginException authSAMLAssertionExpiredError() {
        return new LoginException(ErrorCodes.EXPIRED_ASSERTION + "SAML2STSLoginModule: " + WSTrustConstants.STATUS_CODE_INVALID
                + "::assertion expired or used before its lifetime period");
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#authSAMLAssertionIssuingFailed(java.lang.Throwable)
     */
    @Override
    public void authSAMLAssertionIssuingFailed(Throwable t) {
        logger.error("Unable to issue assertion", t);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#jbossWSUnableToCreateBinaryToken(java.lang.Throwable)
     */
    @Override
    public void jbossWSUnableToCreateBinaryToken(Throwable t) {
        logger.error("Unable to create binary token", t);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#jbossWSUnableToCreateSecurityToken()
     */
    @Override
    public void jbossWSUnableToCreateSecurityToken() {
        logger.warn("Was not able to create security token. Just sending message without binary token");
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#jbossWSUnableToWriteSOAPMessage(java.lang.Exception)
     */
    @Override
    public void jbossWSUnableToWriteSOAPMessage(Throwable t) {
        logger.error("Exception writing SOAP Message", t);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#jbossWSUnableToLoadJBossWSSEConfigError()
     */
    @Override
    public RuntimeException jbossWSUnableToLoadJBossWSSEConfigError() {
        return new RuntimeException(ErrorCodes.RESOURCE_NOT_FOUND + "unable to load jboss-wsse.xml");
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#jbossWSAuthorizationFailed()
     */
    @Override
    public RuntimeException jbossWSAuthorizationFailed() {
        return new RuntimeException(ErrorCodes.PROCESSING_EXCEPTION + "Authorization Failed");
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#jbossWSErrorGettingOperationName(java.lang.Throwable)
     */
    @Override
    public void jbossWSErrorGettingOperationName(Throwable t) {
        logger.error("Exception using backup method to get op name=", t);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#authSAMLCredentialNotAvailable()
     */
    @Override
    public LoginException authSAMLCredentialNotAvailable() {
        return new LoginException(ErrorCodes.NULL_VALUE + "SamlCredential is not available in subject");
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#unableToInstantiateHandler(java.lang.String,
     *java.lang.Throwable)
     */
    @Override
    public RuntimeException authUnableToInstantiateHandler(String token, Throwable t) {
        return new RuntimeException(ErrorCodes.CANNOT_CREATE_INSTANCE + "Unable to instantiate handler:" + token, t);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#jbossWSUnableToCreateSSLSocketFactory(java.lang.Throwable)
     */
    @Override
    public RuntimeException jbossWSUnableToCreateSSLSocketFactory(Throwable t) {
        return new RuntimeException(ErrorCodes.PROCESSING_EXCEPTION + "Unable to create SSL Socket Factory:", t);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#jbossWSUnableToFindSSLSocketFactory()
     */
    @Override
    public RuntimeException jbossWSUnableToFindSSLSocketFactory() {
        return new RuntimeException("We did not find SSL Socket Factory");
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#authUnableToGetIdentityFromSubject()
     */
    @Override
    public RuntimeException authUnableToGetIdentityFromSubject() {
        return new RuntimeException(ErrorCodes.PROCESSING_EXCEPTION + "Unable to get the Identity from the subject.");
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#authSAMLAssertionNullOrEmpty()
     */
    @Override
    public RuntimeException authSAMLAssertionNullOrEmpty() {
        return new RuntimeException("SAML Assertion is null or empty");
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#jbossWSUncheckedAndRolesCannotBeTogether()
     */
    @Override
    public ProcessingException jbossWSUncheckedAndRolesCannotBeTogether() {
        return new ProcessingException(ErrorCodes.PROCESSING_EXCEPTION + "unchecked and role(s) cannot be together");
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#samlIDPHandlingSAML11Error(java.lang.Throwable)
     */
    @Override
    public void samlIDPHandlingSAML11Error(Throwable t) {
        logger.error("Exception handling saml 11 use case:", t);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#samlIDPValidationCheckFailed()
     */
    @Override
    public GeneralSecurityException samlIDPValidationCheckFailed() {
        return new GeneralSecurityException(ErrorCodes.VALIDATION_CHECK_FAILED);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#samlIDPRequestProcessingError(java.lang.Throwable)
     */
    @Override
    public void samlIDPRequestProcessingError(Throwable t) {
        logger.error("Exception in processing request:", t);
    }

    /*
     *(non-Javadoc)
     *
     *@see
     *org.picketlink.identity.federation.PicketLinkLogger#samlIDPUnableToSetParticipantStackUsingDefault(java.lang.Throwable)
     */
    @Override
    public void samlIDPUnableToSetParticipantStackUsingDefault(Throwable t) {
        logger.warn("Unable to set the Identity Participant Stack Class. Will just use the default");
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#samlHandlerConfigurationError(java.lang.Throwable)
     */
    @Override
    public void samlHandlerConfigurationError(Throwable t) {
        logger.error("Exception dealing with handler configuration:", t);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#samlIDPSettingCanonicalizationMethod(java.lang.String)
     */
    @Override
    public void samlIDPSettingCanonicalizationMethod(String canonicalizationMethod) {
        logger.debug("Setting the CanonicalizationMethod on XMLSignatureUtil::" + canonicalizationMethod);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#samlIDPConfigurationError(java.lang.Throwable)
     */
    @Override
    public RuntimeException samlIDPConfigurationError(Throwable t) {
        return new RuntimeException(ErrorCodes.PROCESSING_EXCEPTION + t.getMessage(), t);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#configurationFileMissing(java.lang.String)
     */
    @Override
    public RuntimeException configurationFileMissing(String configFile) {
        return new RuntimeException(ErrorCodes.IDP_WEBBROWSER_VALVE_CONF_FILE_MISSING + configFile);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#samlIDPInstallingDefaultSTSConfig()
     */
    @Override
    public void samlIDPInstallingDefaultSTSConfig() {
        logger.info("Did not find picketlink-sts.xml. We will install default configuration");
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#warn(java.lang.String)
     */
    @Override
    public void warn(String message) {
        logger.warn(message);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#samlSPFallingBackToLocalFormAuthentication()
     */
    @Override
    public void samlSPFallingBackToLocalFormAuthentication() {
        logger.error("Falling back on local Form Authentication if available");
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#unableLocalAuthentication(java.lang.Throwable)
     */
    @Override
    public IOException unableLocalAuthentication(Throwable t) {
        return new IOException(ErrorCodes.UNABLE_LOCAL_AUTH, t);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#samlSPUnableToGetIDPDescriptorFromMetadata()
     */
    @Override
    public void samlSPUnableToGetIDPDescriptorFromMetadata() {
        logger.error("Unable to obtain the IDP SSO Descriptor from metadata");
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#samlSPConfigurationError(java.lang.Throwable)
     */
    @Override
    public RuntimeException samlSPConfigurationError(Throwable t) {
        return new RuntimeException(t.getMessage(), t);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#samlSPSettingCanonicalizationMethod(java.lang.String)
     */
    @Override
    public void samlSPSettingCanonicalizationMethod(String canonicalizationMethod) {
        logger.info("Service Provider is setting the CanonicalizationMethod on XMLSignatureUtil::" +  canonicalizationMethod);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#samlSPCouldNotDispatchToLogoutPage(java.lang.String)
     */
    @Override
    public void samlSPCouldNotDispatchToLogoutPage(String logOutPage) {
        logger.errorf("Cannot dispatch to the logout page: no request dispatcher" + logOutPage);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#usingLoggerImplementation(java.lang.String)
     */
    @Override
    public void usingLoggerImplementation(String className) {
        logger.debugf("Using logger implementation: " + className);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#samlResponseFromIDPParsingFailed()
     */
    @Override
    public void samlResponseFromIDPParsingFailed() {
        logger.error("Error parsing the response from the IDP. Check the strict post binding configuration on both IDP and SP side.");
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#auditSecurityDomainNotFound(java.lang.Throwable)
     */
    @Override
    public ConfigurationException auditSecurityDomainNotFound(Throwable t) {
        return new ConfigurationException(
                "Could not find a security domain configuration. Check if it is defined in WEB-INF/jboss-web.xml or set the "
                        + GeneralConstants.AUDIT_SECURITY_DOMAIN + " system property.", t);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#auditAuditManagerNotFound(java.lang.String, java.lang.Throwable)
     */
    @Override
    public ConfigurationException auditAuditManagerNotFound(String location, Throwable t) {
        return new ConfigurationException("Could not find a audit manager configuration. Location: " + location, t);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#samlIssueInstantMissingError()
     */
    @Override
    public IssueInstantMissingException samlIssueInstantMissingError() {
        return new IssueInstantMissingException(ErrorCodes.NULL_ISSUE_INSTANT);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#samlSPResponseNotCatalinaResponse()
     */
    @Override
    public RuntimeException samlSPResponseNotCatalinaResponseError(Object response) {
        return new RuntimeException(ErrorCodes.SERVICE_PROVIDER_NOT_CATALINA_RESPONSE + ". Received: " + response);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#samlLogoutError(java.lang.Throwable)
     */
    @Override
    public void samlLogoutError(Throwable t) {
        logger.error("Error during the logout.", t);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#samlErrorPageForwardError(java.lang.String, java.lang.Throwable)
     */
    @Override
    public void samlErrorPageForwardError(String errorPage, Throwable t) {
        logger.error("Error forwarding to the error page: " + errorPage);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#samlSPHandleRequestError(java.lang.Throwable)
     */
    @Override
    public void samlSPHandleRequestError(Throwable t) {
        logger.error("Service Provider could not handle the request.", t);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#samlSPProcessingExceptionError()
     */
    @Override
    public IOException samlSPProcessingExceptionError(Throwable t) {
        return new IOException(ErrorCodes.SERVICE_PROVIDER_SERVER_EXCEPTION, t);
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#samlInvalidProtocolBinding()
     */
    @Override
    public IllegalArgumentException samlInvalidProtocolBinding() {
        return new IllegalArgumentException("Invalid SAML Protocol Binding. Expected POST or REDIRECT.");
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#samlHandlerServiceProviderConfigNotFound()
     */
    @Override
    public IllegalStateException samlHandlerServiceProviderConfigNotFound() {
        return new IllegalStateException("Service Provider configuration not found. Check if the "
                + GeneralConstants.CONFIGURATION + " parameter is defined in the handler chain config.");
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#samlSecurityTokenAlreadyPersisted(java.lang.String)
     */
    @Override
    public void samlSecurityTokenAlreadyPersisted(String id) {
        warn("Security Token with id=" + id + " has already been persisted.");
    }

    /*
     *(non-Javadoc)
     *
     *@see org.picketlink.identity.federation.PicketLinkLogger#samlSecurityTokenNotFoundInRegistry(java.lang.String)
     */
    @Override
    public void samlSecurityTokenNotFoundInRegistry(String id) {
        warn("Security Token with id=" + id + " was not found in the registry.");
    }

    /*(non-Javadoc)
     *@see org.picketlink.identity.federation.PicketLinkLogger#samlMetaDataFailedToCreateCacheDuration(java.lang.String)
     */
    @Override
    public IllegalArgumentException samlMetaDataFailedToCreateCacheDuration(String timeValue) {
        return new IllegalArgumentException("Cache duration could not be created using '" + timeValue
                + "'. This value must be an ISO-8601 period or a numeric value representing the duration in milliseconds.");
    }

    @Override
    public ConfigurationException samlMetaDataNoIdentityProviderDefined() {
        return new ConfigurationException("No configuration provided for the Identity Provider.");
    }

    @Override
    public ConfigurationException samlMetaDataNoServiceProviderDefined() {
        return new ConfigurationException("No configuration provided for the Service Provider.");
    }

    /*(non-Javadoc)
     *@see org.picketlink.identity.federation.PicketLinkLogger#securityDomainNotFound()
     */
    @Override
    public ConfigurationException securityDomainNotFound() {
        return new ConfigurationException("The security domain name could not be found. Check your jboss-web.xml.");
    }

    /*(non-Javadoc)
     *@see org.picketlink.identity.federation.PicketLinkLogger#authenticationManagerError(org.picketlink.identity.federation.core.exceptions.ConfigurationException)
     */
    @Override
    public void authenticationManagerError(ConfigurationException e) {
        error("Error loading the AuthenticationManager.", e);
    }

    private void error(String msg, ConfigurationException e) {
        logger.error(msg, e);
    }

    /*(non-Javadoc)
     *@see org.picketlink.identity.federation.PicketLinkLogger#authorizationManagerError(org.picketlink.identity.federation.core.exceptions.ConfigurationException)
     */
    @Override
    public void authorizationManagerError(ConfigurationException e) {
        error("Error loading AuthorizationManager.", e);
    }

    public IllegalStateException jbdcInitializationError(Throwable throwable) {
        return new IllegalStateException(throwable);
    }

    public RuntimeException errorUnmarshallingToken(Throwable e) {
        return new RuntimeException(e);
    }

    public RuntimeException runtimeException(String msg, Throwable e) {
        return new RuntimeException(msg, e);
    }

    public IllegalStateException datasourceIsNull() {
        return new IllegalStateException();
    }

    @Override
    public IllegalArgumentException cannotParseParameterValue(String parameter, Throwable e) {
        return new IllegalArgumentException("Cannot parse: " + parameter , e);
    }

    @Override
    public RuntimeException cannotGetFreeClientPoolKey(String key) {
        return new RuntimeException("Cannot get free client pool key: " + key);
    }

    @Override
    public RuntimeException cannotGetSTSConfigByKey(String key) {
        return new RuntimeException("Cannot get STS config by key: " + key + ". The pool for given key has to be initialized first by calling STSClientPool.initialize method.");
    }

    @Override
    public RuntimeException cannotGetUsedClientsByKey(String key) {
        return new RuntimeException("Cannot get used clients by key: " + key);
    }

    @Override
    public RuntimeException removingNonExistingClientFromUsedClientsByKey(String key) {
        return new RuntimeException("removing non existing client from used clients by key: " + key);
    }

    @Override
    public RuntimeException freePoolAlreadyContainsGivenKey(String key) {
        return new RuntimeException("Free pool already contains given key: " + key);
    }

    @Override
    public RuntimeException maximumNumberOfClientsReachedforPool(String max) {
        return new RuntimeException("Pool reached miximum number of clients within the pool (" + max + ")");
    }

    @Override
    public RuntimeException cannotSetMaxPoolSizeToNegative(String max) {
        return new RuntimeException("Cannot set maximum STS client pool size to negative number (" + max + ")");
    }

    @Override
    public RuntimeException parserFeatureNotSupported(String feature) {
        return new RuntimeException("Parser feature " + feature + " not supported.");
    }

    @Override
    public ProcessingException samlAssertionWrongAudience(String serviceURL) {
        return new ProcessingException("Wrong audience [" + serviceURL + "].");
    }

    @Override
    public ProcessingException samlExtensionUnknownChild(Class<?> clazz) {
        return new ProcessingException("Unknown child type specified for extension: " 
          + (clazz == null ? "<null>" : clazz.getSimpleName())
          + ".");
    }
}
