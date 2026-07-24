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


import java.io.IOException;
import java.security.GeneralSecurityException;
import javax.security.auth.login.LoginException;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.stream.Location;

import jakarta.xml.ws.WebServiceException;

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

/**
 * <p>This interface acts as a Log Facade for PicketLink, from which exceptions and messages should be created or
 * logged.</p> <p>As PicketLink supports multiple containers and its versions, the main objective of this interface is
 * to abstract the logging aspects from the code and provide different logging implementations for each supported
 * binding/container.</p>
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 * @see {@link PicketLinkLoggerFactory}
 */
public interface PicketLinkLogger {

    /**
     * <p>Creates an {@link IllegalArgumentException} for null arguments.</p>
     *
     * @param argument
     *
     * @return
     */
    IllegalArgumentException nullArgumentError(String argument);

    /**
     * <p>Creates an {@link IllegalArgumentException} for arguments that should not be the same.</p>
     *
     * @param string
     *
     * @return
     */
    IllegalArgumentException shouldNotBeTheSameError(String string);

    /**
     * <p>Creates an {@link org.keycloak.saml.common.exceptions.ProcessingException} for resources that are not found.</p>
     *
     * @param resource
     *
     * @return
     */
    ProcessingException resourceNotFound(String resource);

    /**
     * <p>Creates an {@link ProcessingException} for generics processing errors.</p>
     *
     * @param message
     * @param t
     *
     * @return
     */
    ProcessingException processingError(Throwable t);

    /**
     * <p>Creates an {@link RuntimeException} for not supported types.</p>
     *
     * @param name
     *
     * @return
     */
    RuntimeException unsupportedType(String name);

    /**
     * <p>Creates a {@link ProcessingException} for exceptions raised during signature processing.</p>
     *
     * @param e
     *
     * @return
     */
    XMLSignatureException signatureError(Throwable e);

    /**
     * <p>Creates a {@link RuntimeException} for null values.</p>
     *
     * @param nullValue
     *
     * @return
     */
    RuntimeException nullValueError(String nullValue);

    /**
     * <p>Creates a {@link RuntimeException} for not implemented methods or features.</p>
     *
     * @param string
     *
     * @return
     */
    RuntimeException notImplementedYet(String string);

    /**
     * <p>Creates a {@link IllegalStateException} for the case the Audit Manager is null.</p>
     *
     * @return
     */
    IllegalStateException auditNullAuditManager();

    /**
     * <p>Indicates if the logging level is set to INFO.</p>
     *
     * @return
     */
    boolean isInfoEnabled();

    /**
     * <p>Logs a PicketLink Audit Event.</p>
     *
     * @param auditEvent
     */
    void auditEvent(String auditEvent);

    /**
     * <p>Creates a {@link RuntimeException} for missing values.</p>
     *
     * @param string
     *
     * @return
     */
    RuntimeException injectedValueMissing(String value);

    /** <p>Logs a message during the KeyStore setup.</p> */
    void keyStoreSetup();

    /**
     * <p>Creates a {@link IllegalStateException} for the case where the KeyStore is null.</p>
     *
     * @return
     */
    IllegalStateException keyStoreNullStore();

    /**
     * <p>Logs a message for the cases where no public key was found for a given alias.</p>
     *
     * @param alias
     */
    void keyStoreNullPublicKeyForAlias(String alias);

    /**
     * <p>Creates a {@link org.keycloak.saml.common.exceptions.TrustKeyConfigurationException} for exceptions raised during the KeyStore configuration.</p>
     *
     * @param t
     *
     * @return
     */
    TrustKeyConfigurationException keyStoreConfigurationError(Throwable t);

    /**
     * <p>Creates a {@link TrustKeyConfigurationException} for exceptions raised during the KeyStore processing.</p>
     *
     * @param t
     *
     * @return
     */
    TrustKeyProcessingException keyStoreProcessingError(Throwable t);

    /**
     * @param domain
     *
     * @return
     */
    IllegalStateException keyStoreMissingDomainAlias(String domain);

    /**
     * <p>Creates a {@link RuntimeException} for the case where the signing key password is null.</p>
     *
     * @return
     */
    RuntimeException keyStoreNullSigningKeyPass();

    RuntimeException keyStoreNullEncryptionKeyPass();

    /**
     * <p>Creates a {@link RuntimeException} for the case where key store are not located.</p>
     *
     * @param keyStore
     *
     * @return
     */
    RuntimeException keyStoreNotLocated(String keyStore);

    /**
     * <p>Creates a {@link IllegalStateException} for the case where the alias is null.</p>
     *
     * @return
     */
    IllegalStateException keyStoreNullAlias();

    /**
     * <p>Creates a {@link RuntimeException} for the case where parser founds a unknown end element.</p>
     *
     * @param endElementName
     *
     * @return
     */
    RuntimeException parserUnknownEndElement(String endElementName, Location location);

    /**
     * @param tag
     * @param location
     *
     * @return
     */
    RuntimeException parserUnknownTag(String tag, Location location);

    /**
     * @param string
     *
     * @return
     */
    ParsingException parserRequiredAttribute(String string);

    /**
     * @param elementName
     * @param location
     *
     * @return
     */
    RuntimeException parserUnknownStartElement(String elementName, Location location);

    /** @return  */
    IllegalStateException parserNullStartElement();

    /**
     * @param xsiTypeValue
     *
     * @return
     */
    ParsingException parserUnknownXSI(String xsiTypeValue);

    /**
     * @param string
     *
     * @return
     */
    ParsingException parserExpectedEndTag(String tagName);

    /**
     * @param e
     *
     * @return
     */
    ParsingException parserException(Throwable t);

    /**
     * @param string
     *
     * @return
     */
    ParsingException parserExpectedTextValue(String string);

    /**
     * @param expectedXsi
     *
     * @return
     */
    RuntimeException parserExpectedXSI(String expectedXsi);

    /**
     * @param tag
     * @param foundElementTag
     * @param line
     * @param column
     *
     * @return
     */
    RuntimeException parserExpectedTag(String tag, String foundElementTag, Integer line, Integer column);

    /**
     * @param ns
     * @param foundElementNs
     *
     * @return
     */
    RuntimeException parserExpectedNamespace(String ns, String foundElementNs);

    /**
     * @param elementName
     *
     * @return
     */
    RuntimeException parserFailed(String elementName);

    /** @return  */
    ParsingException parserUnableParsingNullToken();

    /**
     * @param t
     *
     * @return
     */
    ParsingException parserError(Throwable t);

    /**
     * @param e
     *
     * @return
     */
    RuntimeException xacmlPDPMessageProcessingError(Throwable t);

    /**
     * @param policyConfigFileName
     *
     * @return
     */
    IllegalStateException fileNotLocated(String policyConfigFileName);

    /**
     * @param string
     *
     * @return
     */
    IllegalStateException optionNotSet(String option);

    /**
     *
     */
    void stsTokenRegistryNotSpecified();

    /** @param tokenRegistryOption */
    void stsTokenRegistryInvalidType(String tokenRegistryOption);

    /**
     *
     */
    void stsTokenRegistryInstantiationError();

    /**
     *
     */
    void stsRevocationRegistryNotSpecified();

    /** @param registryOption */
    void stsRevocationRegistryInvalidType(String registryOption);

    /**
     *
     */
    void stsRevocationRegistryInstantiationError();

    /** @return  */
    ProcessingException samlAssertionExpiredError();

    /** @return  */
    ProcessingException assertionInvalidError();

    /**
     * @param name
     *
     * @return
     */
    RuntimeException writerUnknownTypeError(String name);

    /**
     * @param string
     *
     * @return
     */
    ProcessingException writerNullValueError(String value);

    /**
     * @param value
     *
     * @return
     */
    RuntimeException writerUnsupportedAttributeValueError(String value);

    /** @return  */
    IllegalArgumentException issuerInfoMissingStatusCodeError();

    /**
     * @param fqn
     *
     * @return
     */
    ProcessingException classNotLoadedError(String fqn);

    /**
     * @param fqn
     * @param e
     *
     * @return
     */
    ProcessingException couldNotCreateInstance(String fqn, Throwable t);

    /**
     * @param property
     *
     * @return
     */
    RuntimeException systemPropertyMissingError(String property);

    /** @param t */
    void samlMetaDataIdentityProviderLoadingError(Throwable t);

    /** @param t */
    void samlMetaDataServiceProviderLoadingError(Throwable t);

    /** @param t */
    void signatureAssertionValidationError(Throwable t);

    /** @param id */
    void samlAssertionExpired(String id);

    /**
     * @param attrValue
     *
     * @return
     */
    RuntimeException unknownObjectType(Object attrValue);

    /**
     * @param e
     *
     * @return
     */
    ConfigurationException configurationError(Throwable t);

    /** @param message */
    void trace(String message);

    /**
     * @param string
     * @param t
     */
    void trace(String message, Throwable t);

    /**
     * @param algo
     *
     * @return
     */
    RuntimeException signatureUnknownAlgo(String algo);

    /**
     * @param message
     *
     * @return
     */
    IllegalArgumentException invalidArgumentError(String message);

    /**
     * @param configuration
     * @param protocolContext
     *
     * @return
     */
    ProcessingException stsNoTokenProviderError(String configuration, String protocolContext);

    /** @param message */
    void debug(String message);

    /** @param fileName */
    void stsConfigurationFileNotFoundTCL(String fileName);

    /** @param fileName */
    void stsConfigurationFileNotFoundClassLoader(String fileName);

    /** @param fileName */
    void stsUsingDefaultConfiguration(String fileName);

    /** @param fileName */
    void stsConfigurationFileLoaded(String fileName);

    /**
     * @param t
     *
     * @return
     */
    ConfigurationException stsConfigurationFileParsingError(Throwable t);

    /**
     * @param message
     *
     * @return
     */
    IOException notSerializableError(String message);

    /**
     *
     */
    void trustKeyManagerCreationError(Throwable t);

    /** @param message */
    void info(String message);

    /** @param string */
    void warn(String message);

    /** @param message */
    void error(String message);

    /** @param t */
    void xmlCouldNotGetSchema(Throwable t);

    /** @return  */
    boolean isTraceEnabled();

    /** @return  */
    boolean isDebugEnabled();

    /**
     * @param name
     * @param t
     */
    void jceProviderCouldNotBeLoaded(String name, Throwable t);

    /** @return  */
    ProcessingException writerInvalidKeyInfoNullContentError();

    /**
     * @param first
     * @param second
     *
     * @return
     */
    RuntimeException notEqualError(String first, String second);

    /**
     * @param message
     *
     * @return
     */
    IllegalArgumentException wrongTypeError(String message);

    /**
     * @param certAlgo
     *
     * @return
     */
    RuntimeException encryptUnknownAlgoError(String certAlgo);

    /**
     * @param element
     *
     * @return
     */
    IllegalStateException domMissingDocElementError(String element);

    /**
     * @param element
     *
     * @return
     */
    IllegalStateException domMissingElementError(String element);

    /** @return  */
    WebServiceException stsWSInvalidTokenRequestError();

    /**
     * @param t
     *
     * @return
     */
    WebServiceException stsWSError(Throwable t);

    /**
     * @param t
     *
     * @return
     */
    WebServiceException stsWSConfigurationError(Throwable t);

    /**
     * @param requestType
     *
     * @return
     */
    WSTrustException stsWSInvalidRequestTypeError(String requestType);

    /**
     * @param t
     *
     * @return
     */
    WebServiceException stsWSHandlingTokenRequestError(Throwable t);

    /**
     * @param t
     *
     * @return
     */
    WebServiceException stsWSResponseWritingError(Throwable t);

    /**
     * @param t
     *
     * @return
     */
    RuntimeException stsUnableToConstructKeyManagerError(Throwable t);

    /**
     * @param serviceName
     * @param t
     *
     * @return
     */
    RuntimeException stsPublicKeyError(String serviceName, Throwable t);

    /**
     * @param t
     *
     * @return
     */
    RuntimeException stsSigningKeyPairError(Throwable t);

    /**
     * @param t
     *
     * @return
     */
    RuntimeException stsPublicKeyCertError(Throwable t);

    /**
     *
     */
    void stsTokenTimeoutNotSpecified();

    /**
     * @param t
     *
     * @return
     */
    WSTrustException wsTrustCombinedSecretKeyError(Throwable t);

    /** @return  */
    WSTrustException wsTrustClientPublicKeyError();

    /**
     * @param t
     *
     * @return
     */
    WSTrustException stsError(Throwable t);

    /**
     * @param message
     * @param t
     *
     * @return
     */
    XMLSignatureException signatureInvalidError(String message, Throwable t);

    /**
     *
     */
    void stsSecurityTokenSignatureNotVerified();

    /**
     * @param e
     *
     * @return
     */
    RuntimeException encryptProcessError(Throwable t);

    /**
     *
     */
    void stsSecurityTokenShouldBeEncrypted();

    /**
     * @param password
     *
     * @return
     */
    RuntimeException unableToDecodePasswordError(String password);

    /**
     * @param configFile
     *
     * @return
     */
    IllegalStateException couldNotLoadProperties(String configFile);

    /**
     * @param t
     *
     * @return
     */
    WSTrustException stsKeyInfoTypeCreationError(Throwable t);

    /**
     *
     */
    void stsSecretKeyNotEncrypted();

    /** @return  */
    LoginException authCouldNotIssueSAMLToken();

    /**
     * @param t
     *
     * @return
     */
    LoginException authLoginError(Throwable t);

    /**
     * @param e
     *
     * @return
     */
    IllegalStateException authCouldNotCreateWSTrustClient(Throwable t);

    /** @param id */
    void samlAssertionWithoutExpiration(String id);

    /**
     * @param token
     *
     * @return
     */
    LoginException authCouldNotValidateSAMLToken(Element token);

    /** @return  */
    LoginException authCouldNotLocateSecurityToken();

    /** @return  */
    ProcessingException wsTrustNullCancelTargetError();

    /**
     * @param t
     *
     * @return
     */
    ProcessingException samlAssertionMarshallError(Throwable t);

    /** @return  */
    ProcessingException wsTrustNullRenewTargetError();

    /**
     * @param t
     *
     * @return
     */
    ProcessingException samlAssertionUnmarshallError(Throwable t);

    /** @return  */
    ProcessingException samlAssertionRevokedCouldNotRenew(String id);

    /** @return  */
    ProcessingException wsTrustNullValidationTargetError();

    /** @param attributeProviderClassName */
    void stsWrongAttributeProviderTypeNotInstalled(String attributeProviderClassName);

    /** @param t */
    void attributeProviderInstationError(Throwable t);

    /** @param nodeAsString */
    void samlAssertion(String nodeAsString);

    /**
     * @param dce
     *
     * @return
     */
    RuntimeException wsTrustUnableToGetDataTypeFactory(Throwable t);

    /** @return  */
    ProcessingException wsTrustValidationStatusCodeMissing();

    /** @param activeSessionCount */
    void samlIdentityServerActiveSessionCount(int activeSessionCount);

    /**
     * @param id
     * @param activeSessionCount
     */
    void samlIdentityServerSessionCreated(String id, int activeSessionCount);

    /**
     * @param id
     * @param activeSessionCount
     */
    void samlIdentityServerSessionDestroyed(String id, int activeSessionCount);

    /**
     * @param name
     *
     * @return
     */
    RuntimeException unknowCredentialType(String name);

    /** @param t */
    void samlHandlerRoleGeneratorSetupError(Throwable t);

    /** @return  */
    RuntimeException samlHandlerAssertionNotFound();

    /** @return  */
    ProcessingException samlHandlerAuthnRequestIsNull();

    /** @param t */
    void samlHandlerAuthenticationError(Throwable t);

    /** @return  */
    IllegalArgumentException samlHandlerNoAssertionFromIDP();

    /** @return  */
    ProcessingException samlHandlerNullEncryptedAssertion();

    /** @return  */
    SecurityException samlHandlerIDPAuthenticationFailedError();

    /**
     * @param aee
     *
     * @return
     */
    ProcessingException assertionExpiredError(AssertionExpiredException aee);

    /**
     * @param attrValue
     *
     * @return
     */
    RuntimeException unsupportedRoleType(Object attrValue);

    /**
     * @param inResponseTo
     * @param authnRequestId
     */
    void samlHandlerFailedInResponseToVerification(String inResponseTo, String authnRequestId);

    /** @return  */
    ProcessingException samlHandlerFailedInResponseToVerificarionError();

    /**
     * @param issuer
     *
     * @return
     */
    IssuerNotTrustedException samlIssuerNotTrustedError(String issuer);

    /**
     * @param e
     *
     * @return
     */
    IssuerNotTrustedException samlIssuerNotTrustedException(Throwable t);

    /** @return  */
    ConfigurationException samlHandlerTrustElementMissingError();

    /** @return  */
    ProcessingException samlHandlerIdentityServerNotFoundError();

    /** @return  */
    ProcessingException samlHandlerPrincipalNotFoundError();

    /**
     *
     */
    void samlHandlerKeyPairNotFound();

    /** @return  */
    ProcessingException samlHandlerKeyPairNotFoundError();

    /** @param t */
    void samlHandlerErrorSigningRedirectBindingMessage(Throwable t);

    /**
     * @param t
     *
     * @return
     */
    RuntimeException samlHandlerSigningRedirectBindingMessageError(Throwable t);

    /** @return  */
    SignatureValidationException samlHandlerSignatureValidationFailed();

    /** @param t */
    void samlHandlerErrorValidatingSignature(Throwable t);

    /** @return  */
    ProcessingException samlHandlerInvalidSignatureError();

    /** @return  */
    ProcessingException samlHandlerSignatureNotPresentError();

    /**
     * @param t
     *
     * @return
     */
    ProcessingException samlHandlerSignatureValidationError(Throwable t);

    /** @param t */
    void error(Throwable t);

    /**
     * @param t
     *
     * @return
     */
    RuntimeException samlHandlerChainProcessingError(Throwable t);

    /** @return  */
    TrustKeyConfigurationException trustKeyManagerMissing();

    /** @param rte */
    void samlBase64DecodingError(Throwable t);

    /** @param t */
    void samlParsingError(Throwable t);

    /** @param t */
    void trace(Throwable t);

    /**
     *
     */
    void mappingContextNull();

    /** @param t */
    void attributeManagerError(Throwable t);

    /**
     *
     */
    void couldNotObtainSecurityContext();

    /**
     * @param t
     *
     * @return
     */
    LoginException authFailedToCreatePrincipal(Throwable t);

    /**
     * @param class1
     *
     * @return
     */
    LoginException authSharedCredentialIsNotSAMLCredential(String className);

    /** @return  */
    LoginException authSTSConfigFileNotFound();

    /**
     * @param t
     *
     * @return
     */
    LoginException authErrorHandlingCallback(Throwable t);

    /** @return  */
    LoginException authInvalidSAMLAssertionBySTS();

    /**
     * @param t
     *
     * @return
     */
    LoginException authAssertionValidationError(Throwable t);

    /**
     * @param t
     *
     * @return
     */
    LoginException authFailedToParseSAMLAssertion(Throwable t);

    /** @param t */
    void samlAssertionPasingFailed(Throwable t);

    LoginException authNullKeyStoreFromSecurityDomainError(String name);

    LoginException authNullKeyStoreAliasFromSecurityDomainError(String name);

    LoginException authNoCertificateFoundForAliasError(String alias, String name);

    LoginException authSAMLInvalidSignatureError();

    LoginException authSAMLAssertionExpiredError();

    /** @param t */
    void authSAMLAssertionIssuingFailed(Throwable t);

    /** @param t */
    void jbossWSUnableToCreateBinaryToken(Throwable t);

    /**
     *
     */
    void jbossWSUnableToCreateSecurityToken();

    /** @param ignore */
    void jbossWSUnableToWriteSOAPMessage(Throwable t);

    /** @return  */
    RuntimeException jbossWSUnableToLoadJBossWSSEConfigError();

    /** @return  */
    RuntimeException jbossWSAuthorizationFailed();

    /** @param t */
    void jbossWSErrorGettingOperationName(Throwable t);

    /** @return  */
    LoginException authSAMLCredentialNotAvailable();

    /**
     * @param token
     * @param t
     *
     * @return
     */
    RuntimeException authUnableToInstantiateHandler(String token, Throwable t);

    /**
     * @param e1
     *
     * @return
     */
    RuntimeException jbossWSUnableToCreateSSLSocketFactory(Throwable t);

    /** @return  */
    RuntimeException jbossWSUnableToFindSSLSocketFactory();

    /** @return  */
    RuntimeException authUnableToGetIdentityFromSubject();

    /** @return  */
    RuntimeException authSAMLAssertionNullOrEmpty();

    /** @return  */
    ProcessingException jbossWSUncheckedAndRolesCannotBeTogether();

    /** @param t */
    void samlIDPHandlingSAML11Error(Throwable t);

    /** @return  */
    GeneralSecurityException samlIDPValidationCheckFailed();

    /** @param t */
    void samlIDPRequestProcessingError(Throwable t);

    /** @param t */
    void samlIDPUnableToSetParticipantStackUsingDefault(Throwable t);

    /** @param t */
    void samlHandlerConfigurationError(Throwable t);

    /** @param canonicalizationMethod */
    void samlIDPSettingCanonicalizationMethod(String canonicalizationMethod);

    /**
     * @param t
     *
     * @return
     */
    RuntimeException samlIDPConfigurationError(Throwable t);

    /**
     * @param configFile
     *
     * @return
     */
    RuntimeException configurationFileMissing(String configFile);

    /**
     *
     */
    void samlIDPInstallingDefaultSTSConfig();

    void samlSPFallingBackToLocalFormAuthentication();

    /**
     * @param ex
     *
     * @return
     */
    IOException unableLocalAuthentication(Throwable t);

    /**
     *
     */
    void samlSPUnableToGetIDPDescriptorFromMetadata();

    /**
     * @param t
     *
     * @return
     */
    RuntimeException samlSPConfigurationError(Throwable t);

    /** @param canonicalizationMethod */
    void samlSPSettingCanonicalizationMethod(String canonicalizationMethod);

    /** @param logOutPage */
    void samlSPCouldNotDispatchToLogoutPage(String logOutPage);

    /**
     * <p>Logs the implementation being used to log messages and exceptions.</p>
     *
     * @param name
     */
    void usingLoggerImplementation(String className);

    /**
     *
     */
    void samlResponseFromIDPParsingFailed();

    /**
     * @param t
     *
     * @return
     */
    ConfigurationException auditSecurityDomainNotFound(Throwable t);

    /**
     * @param location
     * @param t
     *
     * @return
     */
    ConfigurationException auditAuditManagerNotFound(String location, Throwable t);

    /** @return  */
    IssueInstantMissingException samlIssueInstantMissingError();

    /**
     * @param response
     *
     * @return
     */
    RuntimeException samlSPResponseNotCatalinaResponseError(Object response);

    /** @param t */
    void samlLogoutError(Throwable t);

    /** @param t */
    void samlErrorPageForwardError(String errorPage, Throwable t);

    /** @param t */
    void samlSPHandleRequestError(Throwable t);

    /**
     * @param t
     *
     * @return
     */
    IOException samlSPProcessingExceptionError(Throwable t);

    /** @return  */
    IllegalArgumentException samlInvalidProtocolBinding();

    /** @return  */
    IllegalStateException samlHandlerServiceProviderConfigNotFound();

    /**
     *
     */
    void samlSecurityTokenAlreadyPersisted(String id);

    /** @param id */
    void samlSecurityTokenNotFoundInRegistry(String id);

    IllegalArgumentException samlMetaDataFailedToCreateCacheDuration(String timeValue);

    ConfigurationException samlMetaDataNoIdentityProviderDefined();

    ConfigurationException samlMetaDataNoServiceProviderDefined();

    ConfigurationException securityDomainNotFound();

    void authenticationManagerError(ConfigurationException e);

    void authorizationManagerError(ConfigurationException e);

    IllegalStateException jbdcInitializationError(Throwable throwable);

    RuntimeException errorUnmarshallingToken(Throwable e);

    RuntimeException runtimeException(String msg, Throwable e);

    IllegalStateException datasourceIsNull();

    IllegalArgumentException cannotParseParameterValue(String parameter, Throwable e);

    RuntimeException cannotGetFreeClientPoolKey(String key);

    RuntimeException cannotGetSTSConfigByKey(String key);

    RuntimeException cannotGetUsedClientsByKey(String key);

    RuntimeException removingNonExistingClientFromUsedClientsByKey(String key);

    RuntimeException freePoolAlreadyContainsGivenKey(String key);

    RuntimeException maximumNumberOfClientsReachedforPool(String max);

    RuntimeException cannotSetMaxPoolSizeToNegative(String max);

    RuntimeException parserFeatureNotSupported(String feature);

    ProcessingException samlAssertionWrongAudience(String serviceURL);

    ProcessingException samlExtensionUnknownChild(Class<?> clazz);
}
