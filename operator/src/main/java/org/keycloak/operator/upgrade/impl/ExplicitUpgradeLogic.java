package org.keycloak.operator.upgrade.impl;

import java.util.Objects;
import java.util.Optional;

import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import org.keycloak.operator.ContextUtils;
import org.keycloak.operator.crds.v2alpha1.CRDUtils;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;

/**
 * Implements an explicit upgrade logic.
 * <p>
 * The decision is controlled by an outside actor using the revision field.
 */
public class ExplicitUpgradeLogic extends BaseUpgradeLogic {

    public ExplicitUpgradeLogic(Context<Keycloak> context, Keycloak keycloak) {
        super(context, keycloak);
    }

    @Override
    Optional<UpdateControl<Keycloak>> onUpgrade() {
        var maybeCurrentRevision = CRDUtils.getRevision(ContextUtils.getCurrentStatefulSet(context).orElseThrow());

        if (maybeCurrentRevision.isEmpty()) {
            decideRecreateUpgrade("Explicit strategy configured. Revision annotation not present in stateful set.");
            return Optional.empty();
        }
        var desiredRevision = CRDUtils.getRevision(context, keycloak);
        if (Objects.equals(maybeCurrentRevision.get(), desiredRevision)) {
            decideRollingUpgrade("Explicit strategy configured. Revision matches.");
            return Optional.empty();
        }

        decideRecreateUpgrade("Explicit strategy configured. Revision (%s) does not match (%s).".formatted(maybeCurrentRevision.get(), desiredRevision));
        return Optional.empty();
    }
}
