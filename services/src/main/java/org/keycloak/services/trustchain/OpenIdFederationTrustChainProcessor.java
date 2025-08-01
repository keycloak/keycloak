package org.keycloak.services.trustchain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.Algorithm;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.TokenCategory;
import org.keycloak.crypto.KeyType;
import org.keycloak.crypto.KeyUse;
import org.keycloak.exceptions.InvalidTrustChainException;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKBuilder;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.openid_federation.EntityStatement;
import org.keycloak.representations.openid_federation.TrustChainResolution;
import org.keycloak.services.ErrorResponseException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.keycloak.events.Errors;
import java.util.Collections;
import java.util.stream.Stream;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.proc.DefaultJOSEObjectTypeVerifier;
import org.keycloak.util.TokenUtil;
import org.keycloak.utils.OpenIdFederationUtils;

public class OpenIdFederationTrustChainProcessor implements TrustChainProcessor {

    private static final Logger logger = Logger.getLogger(OpenIdFederationTrustChainProcessor.class);
    private  final KeycloakSession session;

    private static ObjectMapper om = new ObjectMapper();

    public OpenIdFederationTrustChainProcessor(KeycloakSession session) {
        this.session = session;
    }

    /**
     * This should construct all possible trust chains from a given leaf node self-signed and encoded JWT to a set of trust anchor urls
     * @param leafEs  this is the EntityStatement of a leaf node (Relay party or Openid Provider)
     * @param trustAnchorIds this should hold the trust anchor ids
     * @return any valid trust chains from the leaf node JWT to the trust anchor.
     */
    @Override
    public TrustChainResolution constructTrustChains(EntityStatement leafEs, Set<String> trustAnchorIds) {

        List<TrustChainResolution> trustChainResolutions = subTrustChains(leafEs.getSubject(), leafEs, trustAnchorIds, new HashSet<>());
        
        //TODO enforce policies for valid Trust Chains and set new RPMetadata
        // Need to return only one valid Trust Chain
        return trustChainResolutions.isEmpty() ? null : trustChainResolutions.get(0);
    }

    @Override
    public List<TrustChainResolution> subTrustChains(String initialEntity, EntityStatement leafEs, Set<String> trustAnchorIds, Set<String> visitedNodes) {

        List<TrustChainResolution> chainsList = new ArrayList<>();
        visitedNodes.add(leafEs.getSubject());

        if (leafEs.getAuthorityHints() != null && !leafEs.getAuthorityHints().isEmpty()) {
            leafEs.getAuthorityHints().forEach(authHint -> {
                try {
                    if (visitedNodes.contains(authHint) && !trustAnchorIds.contains(authHint))
                        return;
                    String encodedSubNodeSelf = OpenIdFederationUtils.getSelfSignedToken(authHint, session);
                    EntityStatement subNodeSelfES = parseAndValidateSelfSigned(encodedSubNodeSelf);
                    if (!validateEntityStatementFields(subNodeSelfES, authHint, authHint)) {
                        throw new ErrorResponseException(Errors.INVALID_TRUST_CHAIN, "Trust chain is not valid", Response.Status.BAD_REQUEST);
                    }
                    logger.debug(String.format("EntityStatement of %s about %s. AuthHints: %s", subNodeSelfES.getIssuer(), subNodeSelfES.getSubject(), subNodeSelfES.getAuthorityHints()));

                    String fedApiUrl = subNodeSelfES.getMetadata().getFederationEntity().getFederationFetchEndpoint();
                    String encodedSubNodeSubordinate = OpenIdFederationUtils.getSubordinateToken(fedApiUrl, leafEs.getSubject(), session);
                    EntityStatement subNodeSubordinateES = parseAndValidateSelfSigned(encodedSubNodeSubordinate, EntityStatement.class, subNodeSelfES.getJwks());
                    if (!validateEntityStatementFields(subNodeSubordinateES, authHint, leafEs.getSubject())) {
                        throw new ErrorResponseException(Errors.INVALID_TRUST_CHAIN, "Trust chain is not valid", Response.Status.BAD_REQUEST);
                    }
                    logger.debug(String.format("EntityStatement of %s about %s. AuthHints: %s", subNodeSubordinateES.getIssuer(), subNodeSubordinateES.getSubject(), subNodeSubordinateES.getAuthorityHints()));
                    visitedNodes.add(subNodeSelfES.getSubject());
                    if (trustAnchorIds.contains(authHint)) {
                        TrustChainResolution trustAnchor = new TrustChainResolution();
                        //fetch statement may include RPMetadata when trust anchor == Authority hint
                        //otherwise check that RP is registered as RP in trust anchor
                        if (subNodeSubordinateES.getSubject().equals(initialEntity) && subNodeSubordinateES.getMetadata() != null && subNodeSubordinateES.getMetadata().getRelyingPartyMetadata() != null) {
                            trustAnchor.setEntityFromTA(subNodeSubordinateES);
                        } else if (!OpenIdFederationUtils.containedInListEndpoint(subNodeSelfES.getMetadata().getFederationEntity().getFederationListEndpoint(), OpenIdFederationUtils.OPENID_RELAYING_PARTY, initialEntity, session)) {
                            throw new ErrorResponseException(Errors.INVALID_TRUST_CHAIN, "Trust chain is not valid", Response.Status.BAD_REQUEST);
                        }
                        trustAnchor.getParsedChain().add(0, subNodeSelfES);
                        trustAnchor.setTrustAnchorId(authHint);
                        chainsList.add(trustAnchor);
                    } else {
                        List<TrustChainResolution> subList = subTrustChains(initialEntity, subNodeSelfES, trustAnchorIds, visitedNodes);
                        for (TrustChainResolution tcr : subList) {
                            tcr.getParsedChain().add(0, subNodeSelfES);
                            chainsList.add(tcr);
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

            });
        } else if (trustAnchorIds.contains(leafEs.getSubject())) {
            TrustChainResolution trustAnchor = new TrustChainResolution();
            trustAnchor.setTrustAnchorId(leafEs.getSubject());
            trustAnchor.getParsedChain().add(0, leafEs);
            chainsList.add(trustAnchor);
        }

        return chainsList;

    }

    @Override
    public EntityStatement parseAndValidateSelfSigned(String token) throws InvalidTrustChainException {
        EntityStatement statement = parse(token, EntityStatement.class);
        validateToken(token, statement.getJwks());
        return statement;
    }

    private <T extends EntityStatement> T parseAndValidateSelfSigned(String token, Class<T> clazz, JSONWebKeySet jwks) throws InvalidTrustChainException {
        T statement = parse(token, clazz);
        validateToken(token, jwks);
        return statement;
    }

    @Override
    public void validateToken(String token, JSONWebKeySet jwks){
        try{
            ConfigurableJWTProcessor<SecurityContext> jwtProcessor = produceJwtProcessor(jwks);
            jwtProcessor.process(token, null);

        } catch(IOException | ParseException | BadJOSEException | JOSEException ex) {
            ex.printStackTrace();
            throw new ErrorResponseException(Errors.INVALID_TRUST_CHAIN, "Trust chain is not valid", Response.Status.BAD_REQUEST);
        }
    }

    private ConfigurableJWTProcessor<SecurityContext> produceJwtProcessor(JSONWebKeySet jwks) throws IOException, ParseException {
        String jsonKey = om.writeValueAsString(jwks);
        JWKSet jwkSet = JWKSet.load(new ByteArrayInputStream(jsonKey.getBytes()));
        JWKSource<SecurityContext> keySource = new ImmutableJWKSet<>(jwkSet);
        ConfigurableJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();

        Set<JWSAlgorithm> algs = jwkSet.getKeys().stream()
                .map(key -> {
                    Object alg = key.getAlgorithm();
                    if (alg instanceof JWSAlgorithm jwsAlgorithm) {
                        return jwsAlgorithm;
                    } else if (alg instanceof Algorithm algorithm) {
                        try {
                            return JWSAlgorithm.parse((algorithm).getName());
                        } catch (IllegalArgumentException e) {
                            return null;
                        }
                    } else if (alg instanceof String string) {
                        try {
                            return JWSAlgorithm.parse(string);
                        } catch (Exception e) {
                            return null;
                        }
                    } else {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (algs.isEmpty()) {
            algs = Collections.singleton(JWSAlgorithm.RS256); // Default to RS256
        }

        JWSKeySelector<SecurityContext> keySelector = new JWSVerificationKeySelector<>(algs, keySource);
        jwtProcessor.setJWSKeySelector(keySelector);
        jwtProcessor.setJWSTypeVerifier(new DefaultJOSEObjectTypeVerifier<>(Stream.of(new JOSEObjectType("entity-statement+jwt")).collect(Collectors.toSet())));        jwtProcessor.setJWSTypeVerifier(new DefaultJOSEObjectTypeVerifier<>(Stream.of(new JOSEObjectType(TokenUtil.ENTITY_STATEMENT_JWT)).collect(Collectors.toSet())));
        return jwtProcessor;
    }

    private boolean validateEntityStatementFields(EntityStatement statement, String issuer, String subject) {
        return statement.getIssuer() == null || statement.getIssuer().equals(issuer) || statement.getSubject() == null || statement.getSubject().equals(subject) || statement.getIat() == null || LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) > statement.getIat() || statement.getExp() == null || LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) < statement.getExp();
    }

    private <T extends EntityStatement> T parse(String token, Class<T> clazz) throws InvalidTrustChainException {
        String [] splits = token.split("\\.");
        if(splits.length != 3)
            throw new InvalidTrustChainException("Trust chain contains a chain-link which does not abide to the dot-delimited format of xxx.yyy.zzz");
        try {
            return om.readValue(Base64.getDecoder().decode(splits[1]), clazz);
        } catch (IOException e) {
            throw new InvalidTrustChainException("Trust chain does not contain a valid Entity Statement");
        }
    }

    @Override
    public JSONWebKeySet getKeySet() {
        List<JWK> keys = new LinkedList<>();
        session.keys().getKeysStream(session.getContext().getRealm())
                .filter(k -> k.getStatus().isEnabled() && k.getUse().equals(KeyUse.SIG) && k.getPublicKey() != null && k.getAlgorithm().equals(session.tokens().signatureAlgorithm(TokenCategory.ENTITY_STATEMENT)))
                .forEach(k -> {
                    JWKBuilder b = JWKBuilder.create().kid(k.getKid()).algorithm(k.getAlgorithm());
                    if (k.getType().equals(KeyType.RSA)) {
                        keys.add(b.rsa(k.getPublicKey(), k.getCertificate()));
                    } else if (k.getType().equals(KeyType.EC)) {
                        keys.add(b.ec(k.getPublicKey()));
                    }
                });

        JSONWebKeySet keySet = new JSONWebKeySet();

        JWK[] k = new JWK[keys.size()];
        k = keys.toArray(k);
        keySet.setKeys(k);
        return keySet;
    }

    @Override
    public void close() {
    }

}
