package org.keycloak.services.resources.admin;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.UserRepresentation;

public final class UserUpdateUtils {

    private UserUpdateUtils() {
    }

    public static boolean onlyRequiredActionsChanged(UserRepresentation rep, UserModel user) {
        if (rep.getRequiredActions() == null) return false;

        // Compare required actions (as sets) to see if they differ
        Set<String> repReq = new HashSet<>(rep.getRequiredActions());
        Set<String> userReq = user.getRequiredActionsStream().collect(Collectors.toSet());
        if (Objects.equals(repReq, userReq)) return false;

        // If any of the profile fields changed, then this is not a required-actions-only change
        if (!Objects.equals(rep.getUsername(), user.getUsername())) return false;
        if (!Objects.equals(rep.getFirstName(), user.getFirstName())) return false;
        if (!Objects.equals(rep.getLastName(), user.getLastName())) return false;
        if (!Objects.equals(rep.getEmail(), user.getEmail())) return false;

        // If attributes are provided in the representation and are non-empty and different from existing,
        // then this is not a required-actions-only change. If attributes are null or empty, treat as unchanged
        // for the purposes of detecting required-actions-only updates.
        if (rep.getAttributes() != null && !rep.getAttributes().isEmpty()) {
            if (!Objects.equals(rep.getAttributes(), user.getAttributes())) return false;
        }

        return true;
    }
}
