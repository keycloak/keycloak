package org.keycloak.sdjwt;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages the specification of undisclosed claims and array elements.
 * 
 * @author <a href="mailto:francis.pouatcha@adorsys.com">Francis Pouatcha</a>
 * 
 */
public class DisclosureSpec {

    // Map of undisclosed claims and corresponding salt.
    // salt can be null;
    private final Map<SdJwtClaimName, DisclosureData> undisclosedClaims;
    // Key is the claim name, value is the list of undisclosed elements
    private final Map<SdJwtClaimName, Map<Integer, DisclosureData>> undisclosedArrayElts;

    private DisclosureSpec(Map<SdJwtClaimName, DisclosureData> undisclosedClaims,
            Map<SdJwtClaimName, Map<Integer, DisclosureData>> undisclosedArrayElts) {
        this.undisclosedClaims = undisclosedClaims;
        this.undisclosedArrayElts = undisclosedArrayElts;
    }

    public Map<Integer, DisclosureData> getUndisclosedArrayElt(SdJwtClaimName arrayClaimName) {
        return undisclosedArrayElts.get(arrayClaimName);
    }

    public Map<SdJwtClaimName, DisclosureData> getUndisclosedClaims() {
        return undisclosedClaims;
    }

    // check if a claim is undisclosed
    public DisclosureData getUndisclosedClaim(SdJwtClaimName claimName) {
        return undisclosedClaims.get(claimName);
    }

    // test is claim has undisclosed array elements
    public boolean hasUndisclosedArrayElts(SdJwtClaimName claimName) {
        return undisclosedArrayElts.containsKey(claimName);
    }

    public static class Builder {
        private final Map<SdJwtClaimName, DisclosureData> undisclosedClaims = new HashMap<>();
        private final Map<SdJwtClaimName, Map<Integer, DisclosureData>> undisclosedArrayElts = new HashMap<>();

        public Builder withUndisclosedClaim(String claimName, String salt) {
            this.undisclosedClaims.put(SdJwtClaimName.of(claimName), DisclosureData.of(salt));
            return this;
        }

        public Builder withUndisclosedClaim(String claimName) {
            return withUndisclosedClaim(claimName, null);
        }

        public Builder withUndisclosedArrayElt(String claimName, Integer undisclosedEltIndex, String salt) {
            SdJwtClaimName sdJwtClaimName = SdJwtClaimName.of(claimName);

            Map<Integer, DisclosureData> indexes = this.undisclosedArrayElts.computeIfAbsent(sdJwtClaimName,
                    k -> new HashMap<>());
            indexes.put(undisclosedEltIndex, DisclosureData.of(salt));
            return this;
        }

        public DisclosureSpec build() {
            Map<SdJwtClaimName, Map<Integer, DisclosureData>> immutableMap = new HashMap<>();
            undisclosedArrayElts.forEach((k, v) -> {
                immutableMap.put(k, Collections.unmodifiableMap((v)));
            });
            return new DisclosureSpec(Collections.unmodifiableMap(undisclosedClaims),
                    Collections.unmodifiableMap(immutableMap));
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class DisclosureData {
        private final SdJwtSalt salt;

        private DisclosureData() {
            this.salt = null;
        }

        private DisclosureData(String salt) {
            this.salt = salt == null ? null : SdJwtSalt.of(salt);
        }

        public static DisclosureData of(String salt) {
            return salt == null ? new DisclosureData() : new DisclosureData(salt);
        }

        public SdJwtSalt getSalt() {
            return salt;
        }
    }
}
