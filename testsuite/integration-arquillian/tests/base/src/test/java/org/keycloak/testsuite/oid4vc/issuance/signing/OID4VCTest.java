/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.oid4vc.issuance.signing;

import java.io.IOException;
import java.net.URI;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import org.keycloak.admin.client.resource.ClientScopeResource;
import org.keycloak.admin.client.resource.ProtocolMappersResource;
import org.keycloak.common.Profile;
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.CertificateUtils;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.common.util.PemUtils;
import org.keycloak.constants.Oid4VciConstants;
import org.keycloak.crypto.ECDSASignatureSignerContext;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKBuilder;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oid4vc.OID4VCLoginProtocolFactory;
import org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerEndpoint;
import org.keycloak.protocol.oid4vc.issuance.TimeProvider;
import org.keycloak.protocol.oid4vc.issuance.VCIssuanceContext;
import org.keycloak.protocol.oid4vc.issuance.keybinding.AttestationValidatorUtil;
import org.keycloak.protocol.oid4vc.issuance.keybinding.JwtProofValidator;
import org.keycloak.protocol.oid4vc.issuance.mappers.OID4VCIssuedAtTimeClaimMapper;
import org.keycloak.protocol.oid4vc.model.CredentialRequest;
import org.keycloak.protocol.oid4vc.model.CredentialSubject;
import org.keycloak.protocol.oid4vc.model.Format;
import org.keycloak.protocol.oid4vc.model.ISO18045ResistanceLevel;
import org.keycloak.protocol.oid4vc.model.KeyAttestationJwtBody;
import org.keycloak.protocol.oid4vc.model.NonceResponse;
import org.keycloak.protocol.oid4vc.model.ProofTypesSupported;
import org.keycloak.protocol.oid4vc.model.SupportedCredentialConfiguration;
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.ComponentExportRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.util.AdminClientUtil;
import org.keycloak.testsuite.util.UserBuilder;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.testsuite.util.oauth.OAuthClient;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.http.HttpStatus;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jboss.logging.Logger;
import org.junit.Assert;

import static org.keycloak.testsuite.oid4vc.issuance.signing.OID4VCIssuerEndpointTest.TIME_PROVIDER;
import static org.keycloak.testsuite.oid4vc.issuance.signing.OID4VCSdJwtIssuingEndpointTest.getCredentialIssuer;
import static org.keycloak.testsuite.oid4vc.issuance.signing.OID4VCSdJwtIssuingEndpointTest.getJtiGeneratedIdMapper;

/**
 * Super class for all OID4VC tests. Provides convenience methods to ease the testing.
 */
@EnableFeature(value = Profile.Feature.OID4VC_VCI, skipRestart = true)
public abstract class OID4VCTest extends AbstractTestRealmKeycloakTest {

	private static final Logger LOGGER = Logger.getLogger(OID4VCTest.class);
	protected static final String CONTEXT_URL = "https://www.w3.org/2018/credentials/v1";
	protected static final URI TEST_DID = URI.create("did:web:test.org");
	protected static final List<String> TEST_TYPES = List.of("VerifiableCredential");
	protected static final Instant TEST_EXPIRATION_DATE = Instant.ofEpochSecond(2000);
	protected static final Instant TEST_ISSUANCE_DATE = Instant.ofEpochSecond(1000);

	protected static final KeyWrapper RSA_KEY = getRsaKey();

    protected static final String sdJwtTypeNaturalPersonScopeName = "oid4vc_natural_person";

    protected static final String sdJwtTypeCredentialScopeName = "sd-jwt-credential";
	protected static final String sdJwtTypeCredentialConfigurationIdName = "sd-jwt-credential-config-id";

	protected static final String jwtTypeCredentialScopeName = "jwt-credential";
	protected static final String jwtTypeCredentialConfigurationIdName = "jwt-credential-config-id";

	protected static final String TEST_CREDENTIAL_MAPPERS_FILE = "/oid4vc/test-credential-mappers.json";

	protected static CredentialSubject getCredentialSubject(Map<String, Object> claims) {
		CredentialSubject credentialSubject = new CredentialSubject();
		claims.forEach(credentialSubject::setClaims);
		return credentialSubject;
	}

	protected static VerifiableCredential getTestCredential(Map<String, Object> claims) {

		VerifiableCredential testCredential = new VerifiableCredential();
		testCredential.setId(URI.create(String.format("uri:uuid:%s", UUID.randomUUID())));
		testCredential.setContext(List.of(CONTEXT_URL));
		testCredential.setType(TEST_TYPES);
		testCredential.setIssuer(TEST_DID);
		testCredential.setExpirationDate(TEST_EXPIRATION_DATE);
		if (claims.containsKey("issuanceDate")) {
			testCredential.setIssuanceDate((Instant) claims.get("issuanceDate"));
		}

		testCredential.setCredentialSubject(getCredentialSubject(claims));
		return testCredential;
	}


	public static KeyWrapper getECKey(String keyId) {
		try {
			Security.addProvider(new BouncyCastleProvider());
			KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC", "BC");
			kpg.initialize(256);
			var keyPair = kpg.generateKeyPair();
			KeyWrapper kw = new KeyWrapper();
			kw.setPrivateKey(keyPair.getPrivate());
			kw.setPublicKey(keyPair.getPublic());
			kw.setUse(KeyUse.SIG);
			if (keyId != null) {
				kw.setKid(keyId);
			} else {
				kw.setKid(KeyUtils.createKeyId(keyPair.getPublic()));
			}
			kw.setType("EC");
			kw.setAlgorithm("ES256");
			return kw;

		} catch (NoSuchAlgorithmException | NoSuchProviderException e) {
			throw new RuntimeException(e);
		}
	}

	public static KeyWrapper getEd25519Key(String keyId) {
		try {
			Security.addProvider(new BouncyCastleProvider());
			KeyPairGenerator kpg = KeyPairGenerator.getInstance("Ed25519", "BC");
			var keyPair = kpg.generateKeyPair();
			KeyWrapper kw = new KeyWrapper();
			kw.setPrivateKey(keyPair.getPrivate());
			kw.setPublicKey(keyPair.getPublic());
			kw.setUse(KeyUse.SIG);
			kw.setKid(keyId);
			kw.setType("Ed25519");
			return kw;

		} catch (NoSuchAlgorithmException | NoSuchProviderException e) {
			throw new RuntimeException(e);
		}
	}


	public static KeyWrapper getRsaKey(KeyUse keyUse, String algorithm, String keyName) {
		try {
			KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
			kpg.initialize(2048);
			var keyPair = kpg.generateKeyPair();
			KeyWrapper kw = new KeyWrapper();
			kw.setPrivateKey(keyPair.getPrivate());
			kw.setPublicKey(keyPair.getPublic());
			kw.setUse(keyUse);
			kw.setKid(keyName != null ? keyName : KeyUtils.createKeyId(keyPair.getPublic()));
			kw.setType("RSA");
			kw.setAlgorithm(algorithm);
			return kw;
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	public static KeyWrapper getRsaKey() {
		return getRsaKey(KeyUse.SIG, "RS256", null);
	}

	public static ComponentExportRepresentation getRsaKeyProvider(KeyWrapper keyWrapper) {
		ComponentExportRepresentation componentExportRepresentation = new ComponentExportRepresentation();
		componentExportRepresentation.setName("rsa-key-provider");
		componentExportRepresentation.setId(UUID.randomUUID().toString());
		componentExportRepresentation.setProviderId("rsa");

		Certificate certificate = CertificateUtils.generateV1SelfSignedCertificate(
				new KeyPair((PublicKey) keyWrapper.getPublicKey(), (PrivateKey) keyWrapper.getPrivateKey()), "TestKey");

		componentExportRepresentation.setConfig(new MultivaluedHashMap<>(
				Map.of(
						"privateKey", List.of(PemUtils.encodeKey(keyWrapper.getPrivateKey())),
						"certificate", List.of(PemUtils.encodeCertificate(certificate)),
						"active", List.of("true"),
						"priority", List.of("0"),
						"enabled", List.of("true"),
						"algorithm", List.of(keyWrapper.getAlgorithm()),
						"keyUse", List.of(keyWrapper.getUse().name())
				)
		));
		return componentExportRepresentation;
	}

	public static ComponentExportRepresentation getRsaEncKeyProvider(String algorithm, String keyName, int priority) {
		KeyWrapper keyWrapper = getRsaKey(KeyUse.ENC, algorithm, keyName);
		ComponentExportRepresentation componentExportRepresentation = new ComponentExportRepresentation();
		componentExportRepresentation.setName(keyName);
		componentExportRepresentation.setId(UUID.randomUUID().toString());
		componentExportRepresentation.setProviderId("rsa");

		Certificate certificate = CertificateUtils.generateV1SelfSignedCertificate(
				new KeyPair((PublicKey) keyWrapper.getPublicKey(), (PrivateKey) keyWrapper.getPrivateKey()), "TestKey");

		componentExportRepresentation.setConfig(new MultivaluedHashMap<>(
				Map.of(
						"privateKey", List.of(PemUtils.encodeKey(keyWrapper.getPrivateKey())),
						"certificate", List.of(PemUtils.encodeCertificate(certificate)),
						"active", List.of("true"),
						"priority", List.of(String.valueOf(priority)),
						"enabled", List.of("true"),
						"algorithm", List.of(algorithm),
						"keyUse", List.of(KeyUse.ENC.name())
				)
		));
		return componentExportRepresentation;
	}


	protected ClientRepresentation getTestClient(String clientId) {
		ClientRepresentation clientRepresentation = new ClientRepresentation();
		clientRepresentation.setClientId(clientId);
		clientRepresentation.setProtocol(OID4VCLoginProtocolFactory.PROTOCOL_ID);
		clientRepresentation.setEnabled(true);
		return clientRepresentation;
	}

	protected ComponentExportRepresentation getEdDSAKeyProvider() {
		ComponentExportRepresentation componentExportRepresentation = new ComponentExportRepresentation();
		componentExportRepresentation.setName("eddsa-generated");
		componentExportRepresentation.setId(UUID.randomUUID().toString());
		componentExportRepresentation.setProviderId("eddsa-generated");

		componentExportRepresentation.setConfig(new MultivaluedHashMap<>(
						Map.of(
								"eddsaEllipticCurveKey", List.of("Ed25519"))
				)
		);
		return componentExportRepresentation;
	}

	protected ComponentExportRepresentation getEcKeyProvider() {
		ComponentExportRepresentation componentExportRepresentation = new ComponentExportRepresentation();
		componentExportRepresentation.setName("ecdsa-issuer-key");
		componentExportRepresentation.setId(UUID.randomUUID().toString());
		componentExportRepresentation.setProviderId("ecdsa-generated");
		componentExportRepresentation.setConfig(new MultivaluedHashMap<>(
				Map.of(
						"ecdsaEllipticCurveKey", List.of("P-256"),
						"algorithm", List.of("ES256"))
		));
		return componentExportRepresentation;
	}

	public void addProtocolMappersToClientScope(ClientScopeRepresentation clientScope,
												List<ProtocolMapperRepresentation> protocolMappers) {
		String scopeId = clientScope.getId();
		String scopeName = clientScope.getName();

		if (protocolMappers.isEmpty()) {
			return;
		}

		ClientScopeResource clientScopeResource = testRealm().clientScopes().get(scopeId);
		ProtocolMappersResource protocolMappersResource = clientScopeResource.getProtocolMappers();

		for (ProtocolMapperRepresentation protocolMapper : protocolMappers) {
			Response response = protocolMappersResource.createMapper(protocolMapper);
			if (response.getStatus() != 201) {
				LOGGER.errorf("Failed to create protocol mapper: {} for scope: {}", protocolMapper, scopeName);
			}
		}
	}

	public List<ProtocolMapperRepresentation> getProtocolMappers(String scopeName) {
		return List.of(
				getUserAttributeMapper("email", "email"),
				getUserAttributeMapper("firstName", "firstName"),
				getUserAttributeMapper("lastName", "lastName"),
				getJtiGeneratedIdMapper(),
				getStaticClaimMapper(scopeName),
				getIssuedAtTimeMapper("iat", ChronoUnit.HOURS.name(), "COMPUTE"),
				getIssuedAtTimeMapper("nbf", null, "COMPUTE"));
	}

	public static ProtocolMapperRepresentation getRoleMapper(String clientId) {
		ProtocolMapperRepresentation protocolMapperRepresentation = new ProtocolMapperRepresentation();
		protocolMapperRepresentation.setName("role-mapper");
		protocolMapperRepresentation.setId(UUID.randomUUID().toString());
		protocolMapperRepresentation.setProtocol(Oid4VciConstants.OID4VC_PROTOCOL);
		protocolMapperRepresentation.setProtocolMapper("oid4vc-target-role-mapper");
		protocolMapperRepresentation.setConfig(
				Map.of("claim.name", "roles", "clientId", clientId)
		);
		return protocolMapperRepresentation;
	}

	public static ProtocolMapperRepresentation getIdMapper() {
		ProtocolMapperRepresentation protocolMapperRepresentation = new ProtocolMapperRepresentation();
		protocolMapperRepresentation.setName("id-mapper");
		protocolMapperRepresentation.setProtocol(Oid4VciConstants.OID4VC_PROTOCOL);
		protocolMapperRepresentation.setId(UUID.randomUUID().toString());
		protocolMapperRepresentation.setProtocolMapper("oid4vc-subject-id-mapper");
		protocolMapperRepresentation.setConfig(Map.of());
		return protocolMapperRepresentation;
	}

	public static ProtocolMapperRepresentation getStaticClaimMapper(String scopeName) {
		ProtocolMapperRepresentation protocolMapperRepresentation = new ProtocolMapperRepresentation();
		protocolMapperRepresentation.setName(UUID.randomUUID().toString());
		protocolMapperRepresentation.setProtocol(Oid4VciConstants.OID4VC_PROTOCOL);
		protocolMapperRepresentation.setId(UUID.randomUUID().toString());
		protocolMapperRepresentation.setProtocolMapper("oid4vc-static-claim-mapper");
		protocolMapperRepresentation.setConfig(
				Map.of("claim.name", "scope-name",
						"staticValue", scopeName)
		);
		return protocolMapperRepresentation;
	}

	public static KeyWrapper getKeyFromSession(KeycloakSession keycloakSession) {
		// we only set one key to the realm, thus can just take the first one
		// if run inside the testsuite, configure is called separated from the test itself, thus we cannot just take
		// the key from the `configureTestRealm` method.
		KeyWrapper kw = keycloakSession
				.keys()
				.getKeysStream(keycloakSession.getContext().getRealm())
				.peek(k -> LOGGER.warnf("THE KEY: %s - %s", k.getKid(), k.getAlgorithm()))
				.findFirst()
				.orElseThrow(() -> new RuntimeException("No key was configured"));
		LOGGER.warnf("Kid is %s", kw.getKid());
		return kw;
	}

	public static String getKeyIdFromSession(KeycloakSession keycloakSession) {
		return getKeyFromSession(keycloakSession).getKid();
	}

	public static ComponentExportRepresentation getCredentialBuilderProvider(String vcFormat) {
		ComponentExportRepresentation componentExportRepresentation = new ComponentExportRepresentation();
		componentExportRepresentation.setName("credential-builder-" + vcFormat);
		componentExportRepresentation.setId(UUID.randomUUID().toString());
		componentExportRepresentation.setProviderId(vcFormat);

		return componentExportRepresentation;
	}

	public static UserRepresentation getUserRepresentation(Map<String, List<String>> clientRoles) {
		UserBuilder userBuilder = UserBuilder.create()
				.id(KeycloakModelUtils.generateId())
				.username("john")
				.enabled(true)
				.email("john@email.cz")
				.emailVerified(true)
				.firstName("John")
				.lastName("Doe")
				.password("password")
				.role("account", "manage-account")
				.role("account", "view-profile");

		clientRoles.entrySet().forEach(entry -> {
			entry.getValue().forEach(role -> userBuilder.role(entry.getKey(), role));
		});

		return userBuilder.build();
	}

	public static RoleRepresentation getRoleRepresentation(String roleName, String clientId) {

		RoleRepresentation role = new RoleRepresentation();
		role.setName(roleName);
		role.setId(clientId);
		role.setClientRole(true);
		return role;
	}

	protected String getBearerToken(OAuthClient oAuthClient) {
		return getBearerToken(oAuthClient, null);
	}

	protected String getBearerToken(OAuthClient oAuthClient, ClientRepresentation client) {
		return getBearerToken(oAuthClient, client, null);
	}

	protected String getBearerToken(OAuthClient oAuthClient, ClientRepresentation client, String credentialScopeName) {
		if (client != null) {
			oAuthClient.client(client.getClientId(), client.getSecret());
		}
		if (credentialScopeName != null) {
			oAuthClient.scope(credentialScopeName);
		}
		AuthorizationEndpointResponse authorizationEndpointResponse = oAuthClient.doLogin("john",
				"password");
		return oAuthClient.doAccessTokenRequest(authorizationEndpointResponse.getCode()).getAccessToken();
	}

	public static class StaticTimeProvider implements TimeProvider {
		private final int currentTimeInS;

		public StaticTimeProvider(int currentTimeInS) {
			this.currentTimeInS = currentTimeInS;
		}

		@Override
		public int currentTimeSeconds() {
			return currentTimeInS;
		}

		@Override
		public long currentTimeMillis() {
			return currentTimeInS * 1000L;
		}
	}

	protected ProtocolMapperRepresentation getUserAttributeMapper(String subjectProperty, String attributeName) {
		ProtocolMapperRepresentation protocolMapperRepresentation = new ProtocolMapperRepresentation();
		protocolMapperRepresentation.setName(attributeName + "-mapper");
		protocolMapperRepresentation.setProtocol(Oid4VciConstants.OID4VC_PROTOCOL);
		protocolMapperRepresentation.setId(UUID.randomUUID().toString());
		protocolMapperRepresentation.setProtocolMapper("oid4vc-user-attribute-mapper");
		protocolMapperRepresentation.setConfig(
				Map.of(
						"claim.name", subjectProperty,
						"userAttribute", attributeName)
		);
		return protocolMapperRepresentation;
	}

	protected ProtocolMapperRepresentation getIssuedAtTimeMapper(String subjectProperty, String truncateToTimeUnit, String valueSource) {
		ProtocolMapperRepresentation protocolMapperRepresentation = new ProtocolMapperRepresentation();
		protocolMapperRepresentation.setName(subjectProperty + "-oid4vc-issued-at-time-claim-mapper");
		protocolMapperRepresentation.setProtocol(Oid4VciConstants.OID4VC_PROTOCOL);
		protocolMapperRepresentation.setId(UUID.randomUUID().toString());
		protocolMapperRepresentation.setProtocolMapper("oid4vc-issued-at-time-claim-mapper");

		Map<String, String> configMap = new HashMap<>();
		Optional.ofNullable(subjectProperty)
				.ifPresent(value -> configMap.put(OID4VCIssuedAtTimeClaimMapper.CLAIM_NAME, value));
		Optional.ofNullable(truncateToTimeUnit)
				.ifPresent(value -> configMap.put(OID4VCIssuedAtTimeClaimMapper.TRUNCATE_TO_TIME_UNIT_KEY, value));
		Optional.ofNullable(valueSource)
				.ifPresent(value -> configMap.put(OID4VCIssuedAtTimeClaimMapper.VALUE_SOURCE, value));

		protocolMapperRepresentation.setConfig(configMap);
		return protocolMapperRepresentation;
	}

	public static String generateJwtProof(String aud, String nonce) {
		KeyWrapper keyWrapper = getECKey(null);
		keyWrapper.setKid(null); // erase the autogenerated one

		// JWK public key
		JWK jwk = JWKBuilder.create().ec(keyWrapper.getPublicKey());

		return generateUnsignedJwtProof(jwk, aud, nonce)
				.sign(new ECDSASignatureSignerContext(keyWrapper));
	}

	public static String generateInvalidJwtProof(String aud, String nonce) {
		KeyWrapper keyWrapper = getECKey(null);
		keyWrapper.setKid(null); // erase the autogenerated one

		KeyWrapper unrelatedKeyWrapper = getECKey(null);
		unrelatedKeyWrapper.setKid(null); // erase the autogenerated one

		// JWK public key
		JWK jwk = JWKBuilder.create().ec(keyWrapper.getPublicKey());

		// Sign with an unrelated key
		return generateUnsignedJwtProof(jwk, aud, nonce)
				.sign(new ECDSASignatureSignerContext(unrelatedKeyWrapper));
	}

	public static JWSBuilder.EncodingBuilder generateUnsignedJwtProof(JWK jwk, String aud, String nonce) {
		AccessToken token = new AccessToken();
		token.addAudience(aud);
		token.setNonce(nonce);
		token.issuedNow();

		return new JWSBuilder()
				.type(JwtProofValidator.PROOF_JWT_TYP)
				.jwk(jwk)
				.jsonContent(token);
	}

	public static String getCNonce() {
		UriBuilder builder = UriBuilder.fromUri(OAuthClient.AUTH_SERVER_ROOT);
		URI oid4vcUri = RealmsResource.protocolUrl(builder).build(AbstractTestRealmKeycloakTest.TEST_REALM_NAME,
				OID4VCLoginProtocolFactory.PROTOCOL_ID);
		String nonceUrl = String.format("%s/%s", oid4vcUri.toString(), OID4VCIssuerEndpoint.NONCE_PATH);

		String nonceResponseString;
		// request cNonce
		try (Client client = AdminClientUtil.createResteasyClient()) {
			WebTarget nonceTarget = client.target(nonceUrl);
			// the nonce endpoint must be unprotected, and therefore it must be accessible without any authentication
			Invocation.Builder nonceInvocationBuilder = nonceTarget.request()
					// just making sure that no authentication is added
					// by interceptors or similar
					.header(HttpHeaders.AUTHORIZATION, null)
					.header(HttpHeaders.COOKIE, null);

			try (Response response = nonceInvocationBuilder.post(null)) {
				Assert.assertEquals(HttpStatus.SC_OK, response.getStatus());
				Assert.assertTrue(response.getMediaType().toString().startsWith(MediaType.APPLICATION_JSON_TYPE.toString()));
				nonceResponseString = parseResponse(response);
				Assert.assertNotNull(nonceResponseString);
				Assert.assertEquals("no-store", response.getHeaderString(HttpHeaders.CACHE_CONTROL));
			}
		}
		NonceResponse nonceResponse;
		try {
			nonceResponse = JsonSerialization.readValue(nonceResponseString, NonceResponse.class);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return nonceResponse.getNonce();
	}

	public static String parseResponse(Response response) {
		try {
			return response.readEntity(String.class);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static String generateJwtProofWithKey(String aud, String nonce, KeyWrapper keyWrapper) {
		String originalKid = keyWrapper.getKid();

		JWK jwk = JWKBuilder.create()
				.kid(originalKid)
				.ec(keyWrapper.getPublicKey());

		keyWrapper.setKid(null);

		return generateUnsignedJwtProof(jwk, aud, nonce)
				.sign(new ECDSASignatureSignerContext(keyWrapper));
	}

	protected static String createValidAttestationJwt(KeycloakSession session,
													  KeyWrapper attestationKey,
													  JWK proofJwk,
													  String cNonce) {
		return createValidAttestationJwt(session, attestationKey, List.of(proofJwk), cNonce);
	}

	protected static String createValidAttestationJwt(KeycloakSession session,
													  KeyWrapper attestationKey,
													  List<JWK> proofJwks,
													  String cNonce) {
		try {
			KeyAttestationJwtBody payload = new KeyAttestationJwtBody();
			payload.setIat((long) TIME_PROVIDER.currentTimeSeconds());
			payload.setNonce(cNonce);
			payload.setAttestedKeys(proofJwks);
			payload.setKeyStorage(List.of(ISO18045ResistanceLevel.HIGH.getValue()));
			payload.setUserAuthentication(List.of(ISO18045ResistanceLevel.HIGH.getValue()));

			return new JWSBuilder()
					.type(AttestationValidatorUtil.ATTESTATION_JWT_TYP)
					.kid(attestationKey.getKid())
					.jsonContent(payload)
					.sign(new ECDSASignatureSignerContext(attestationKey));
		} catch (Exception e) {
			throw new RuntimeException("Failed to create attestation JWT", e);
		}
	}

	protected static VCIssuanceContext createVCIssuanceContext(KeycloakSession session) {
		VCIssuanceContext context = new VCIssuanceContext();
		SupportedCredentialConfiguration config = new SupportedCredentialConfiguration()
				.setFormat(Format.SD_JWT_VC)
				.setVct("https://credentials.example.com/test-credential")
				.setCryptographicBindingMethodsSupported(List.of("jwk"))
				.setProofTypesSupported(ProofTypesSupported.parse(session, List.of("ES256")));

		context.setCredentialConfig(config)
				.setCredentialRequest(new CredentialRequest());
		return context;
	}

	protected static String generateJwtProofWithKeyAttestation(KeycloakSession session,
															   KeyWrapper proofKey,
															   String attestationJwt,
															   String cNonce) throws IOException, JWSInputException {
		if (proofKey.getPrivateKey() == null) {
			throw new IllegalStateException("Proof key must have private key for signing");
		}
		JWK proofJwk = JWKBuilder.create().ec(proofKey.getPublicKey());
		proofJwk.setKeyId(proofKey.getKid());
		proofJwk.setAlgorithm(proofKey.getAlgorithm());

		AccessToken token = new AccessToken();
		token.addAudience(getCredentialIssuer(session));
		token.setNonce(cNonce);
		token.issuedNow();

		Map<String, Object> header = new LinkedHashMap<>();
		header.put("alg", proofKey.getAlgorithm());
		header.put("typ", JwtProofValidator.PROOF_JWT_TYP);
		header.put("jwk", proofJwk);
		header.put("key_attestation", attestationJwt);

		return new JWSBuilder() {
			@Override
			protected String encodeHeader(String sigAlgName) {
				try {
					return Base64Url.encode(JsonSerialization.writeValueAsBytes(header));
				} catch (IOException e) {
					throw new RuntimeException("Failed to encode header", e);
				}
			}
		}.jsonContent(token).sign(new ECDSASignatureSignerContext(proofKey));
	}


	protected static Map<String, Object> createAttestationPayload(JWK proofJwk, String cNonce) {
		Objects.requireNonNull(proofJwk, "Proof JWK cannot be null");
		Map<String, Object> payload = new HashMap<>();
		payload.put("iat", TIME_PROVIDER.currentTimeSeconds());
		payload.put("attested_keys", List.of(proofJwk));
		payload.put("key_storage", List.of(ISO18045ResistanceLevel.HIGH.getValue()));
		payload.put("user_authentication", List.of(ISO18045ResistanceLevel.HIGH.getValue()));
		payload.put("nonce", cNonce);

		return payload;
	}

	public static <T> T fromJsonString(String representation, Class<T> clazz) {
		try {
			return JsonSerialization.readValue(representation, clazz);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static <T> T fromJsonString(String representation, TypeReference<T> typeReference) {
		if (representation == null) {
			return null;
		}
		try {
			return JsonSerialization.readValue(representation, typeReference);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static String toJsonString(Object object) {
		if (object == null) {
			return null;
		}
		try {
			return JsonSerialization.writeValueAsString(object);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
