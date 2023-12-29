package org.keycloak.sdjwt;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class DisclosureRedList {
    public static final List<String> redList = Collections.unmodifiableList(Arrays.asList("iss", "iat", "nbf", "exp", "cnf", "vct", "status"));

    private final Set<SdJwtClaimName> redListClaimNames;
    public static final DisclosureRedList defaultList = defaultList();

    public DisclosureRedList of(Set<SdJwtClaimName> redListClaimNames) {
        return new DisclosureRedList(redListClaimNames);
    }

    private static DisclosureRedList defaultList() {
        return new DisclosureRedList(redList.stream().map(SdJwtClaimName::of).collect(Collectors.toSet()));
    }

    private DisclosureRedList(Set<SdJwtClaimName> redListClaimNames) {
        this.redListClaimNames = Collections.unmodifiableSet(redListClaimNames);
    }

    public boolean isRedListedClaimName(SdJwtClaimName claimName) {
        return redListClaimNames.contains(claimName);
    }

    public boolean containsRedListedClaimNames(Collection<SdJwtClaimName> claimNames) {
        return !redListClaimNames.isEmpty() && !claimNames.isEmpty() && !Collections.disjoint(redListClaimNames, claimNames);
    }
}
