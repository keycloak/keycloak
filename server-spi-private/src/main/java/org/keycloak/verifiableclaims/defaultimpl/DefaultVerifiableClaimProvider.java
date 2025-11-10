package org.keycloak.verifiableclaims.defaultimpl;

import org.keycloak.models.*;
import org.keycloak.userprofile.AttributeMetadata;
import org.keycloak.userprofile.Attributes;
import org.keycloak.userprofile.UserProfile;
import org.keycloak.userprofile.UserProfileContext;
import org.keycloak.userprofile.UserProfileProvider;
import org.keycloak.verifiableclaims.VerifiableClaimProvider;
import org.keycloak.verifiableclaims.model.AttestationResult;
import org.keycloak.verifiableclaims.model.ClaimProjection;
import org.keycloak.verifiableclaims.model.ClaimStatus;
import org.keycloak.verifiableclaims.model.UpdateDecision;
import org.keycloak.verifiableclaims.model.VerifiableAttributeConfig;

import java.util.*;

/**
 * Default core provider backed by User Profile annotations.
 *
 * Admin marks attributes as verifiable in: Realm Settings → User Profile → Attributes → Annotations
 *   verifiable=true
 *   verifiable.schema=scheme:whatever         (optional)
 *   verifiable.adminBypass=true|false         (optional, default true)
 *   verifiable.requireForUsers=true|false     (optional, default true)
 *   verifiable.emitInTokens=true|false        (optional, default true)
 */
public class DefaultVerifiableClaimProvider implements VerifiableClaimProvider {

    private final KeycloakSession session;
    public DefaultVerifiableClaimProvider(KeycloakSession session) { this.session = session; }

    private static final String SUFFIX_PENDING = "._pending";
    private static final String SUFFIX_STATUS  = "._status";
    private static final String SUFFIX_EVID    = "._evidenceRef";

    // annotation keys
    private static final String ANN_VERIFY            = "verifiable";
    private static final String ANN_SCHEMA            = "verifiable.schema";
    private static final String ANN_ADMIN_BYPASS      = "verifiable.adminBypass";
    private static final String ANN_REQUIRE_FOR_USERS = "verifiable.requireForUsers";
    private static final String ANN_EMIT_TOKENS       = "verifiable.emitInTokens";

    @Override
    public Map<String, VerifiableAttributeConfig> getVerifiableAttributeConfig(KeycloakSession session, RealmModel realm) {
        Map<String, VerifiableAttributeConfig> out = new HashMap<>();

        UserProfileProvider upp = session.getProvider(UserProfileProvider.class);
        if (upp == null) return out;

        // Create a "blank" profile for this realm/context so we can read AttributeMetadata
        UserProfile tmp = upp.create(UserProfileContext.USER_API, Collections.emptyMap());
        Attributes attrs = tmp.getAttributes();

        for (String name : attrs.nameSet()) {
            AttributeMetadata md = attrs.getMetadata(name);
            if (md == null) continue;

            Map<String, Object> ann = md.getAnnotations();
            if (ann == null) continue;

            if (!"true".equalsIgnoreCase(annString(ann, ANN_VERIFY))) continue;

            String schema = orDefault(annString(ann, ANN_SCHEMA), "schema:generic");
            boolean adminBypass     = annBool(ann, ANN_ADMIN_BYPASS, true);
            boolean requireForUsers = annBool(ann, ANN_REQUIRE_FOR_USERS, true);
            boolean emitInTokens    = annBool(ann, ANN_EMIT_TOKENS, true);

            out.put(name, new VerifiableAttributeConfig(
                    name,
                    requireForUsers,
                    adminBypass,
                    emitInTokens,
                    schema
            ));
        }

        return out;
    }

    @Override
    public UpdateDecision onAttributesAboutToUpdate(KeycloakSession session, RealmModel realm, UserModel user,
                                                    UserProfile profile, UserProfileContext context, Attributes incoming) {

        Map<String, VerifiableAttributeConfig> cfg = getVerifiableAttributeConfig(session, realm);

        // Seed with the caller's proposed attributes (managed + unmanaged)
        UpdateDecision.Builder b = UpdateDecision.builder(incoming.toMap());

        for (String attr : cfg.keySet()) {
            List<String> newVals = incoming.toMap().get(attr);
            if (newVals == null) continue; // not being changed

            boolean admin = context == UserProfileContext.USER_API;
            VerifiableAttributeConfig c = cfg.get(attr);

            if (admin && c.isAllowAdminBypass()) {
                // Admin bypass → persist now, mark VERIFIED
                b.setNow(attr, newVals)
                        .status(attr, new ClaimProjection(ClaimStatus.VERIFIED, "admin-bypass", "admin", new Date()));
                clearShadow(user, attr);
                setShadow(user, attr + SUFFIX_STATUS, List.of(ClaimStatus.VERIFIED.name()));
                continue;
            }

            // User path → stage change as PENDING
            String correlation = UUID.randomUUID().toString();
            setShadow(user, attr + SUFFIX_PENDING, newVals);
            setShadow(user, attr + SUFFIX_STATUS, List.of(ClaimStatus.PENDING.name()));
            setShadow(user, attr + SUFFIX_EVID, List.of(correlation));

            // Do NOT persist the new value now
            b.delay(attr)
                    .status(attr, new ClaimProjection(ClaimStatus.PENDING, correlation, "pending", null));
        }

        return b.build();
    }

    @Override
    public Attributes enrichAttributesForRepresentation(KeycloakSession session, RealmModel realm, UserModel user,
                                                        Attributes base, UserProfileContext context) {
        // Nothing to mutate in Attributes here; shadow fields already live on UserModel.
        return base;
    }

    @Override
    public boolean completeAttestation(KeycloakSession session, RealmModel realm, String userId, String ref, AttestationResult result) {
        UserModel user = session.users().getUserById(realm, userId);
        if (user == null) return false;

        Map<String, VerifiableAttributeConfig> cfg = getVerifiableAttributeConfig(session, realm);

        for (String attr : cfg.keySet()) {
            String evid = user.getFirstAttribute(attr + SUFFIX_EVID);
            if (!Objects.equals(evid, ref)) continue;

            switch (result.getStatus()) {
                case VERIFIED -> {
                    List<String> pending = user.getAttributes().get(attr + SUFFIX_PENDING);
                    if (pending != null && !pending.isEmpty()) user.setAttribute(attr, pending);
                    setShadow(user, attr + SUFFIX_STATUS, List.of(ClaimStatus.VERIFIED.name()));
                    setShadow(user, attr + SUFFIX_EVID, List.of(orDefault(result.getEvidenceRef(), ref)));
                    user.removeAttribute(attr + SUFFIX_PENDING);
                    return true;
                }
                case REJECTED, EXPIRED -> {
                    user.removeAttribute(attr + SUFFIX_PENDING);
                    setShadow(user, attr + SUFFIX_STATUS, List.of(result.getStatus().name()));
                    if (result.getEvidenceRef() != null) setShadow(user, attr + SUFFIX_EVID, List.of(result.getEvidenceRef()));
                    return true;
                }
                case PENDING -> { return false; }
            }
        }

        return false;
    }

    // ---------------- helpers ----------------

    private static String annString(Map<String, Object> ann, String key) {
        Object v = ann.get(key);
        if (v instanceof String s) return s;
        if (v instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof String s) return s;
        return null;
    }

    private static boolean annBool(Map<String, Object> ann, String key, boolean dflt) {
        String s = annString(ann, key);
        if (s == null) return dflt;
        return "true".equalsIgnoreCase(s) || "1".equals(s);
    }

    private static String orDefault(String v, String d) { return v == null ? d : v; }

    private static void setShadow(UserModel user, String key, List<String> values) { user.setAttribute(key, values); }

    private static void clearShadow(UserModel user, String attr) {
        user.removeAttribute(attr + SUFFIX_PENDING);
        user.removeAttribute(attr + SUFFIX_STATUS);
        user.removeAttribute(attr + SUFFIX_EVID);
    }

    @Override public void close() { }
}
