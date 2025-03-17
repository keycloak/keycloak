package org.keycloak.operator.update.impl;

import java.util.Objects;
import java.util.Optional;

import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import org.keycloak.operator.ContextUtils;
import org.keycloak.operator.crds.v2alpha1.CRDUtils;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.UpdateSpec;

/**
 * Implements an explicit update logic.
 * <p>
 * The decision is controlled by an outside actor using the revision field.
 */
public class ExplicitUpdateLogic extends BaseUpdateLogic {

    public ExplicitUpdateLogic(Context<Keycloak> context, Keycloak keycloak) {
        super(context, keycloak);
    }

    @Override
    Optional<UpdateControl<Keycloak>> onUpdate() {
        var maybeCurrentRevision = CRDUtils.getRevision(ContextUtils.getCurrentStatefulSet(context).orElseThrow());

        if (maybeCurrentRevision.isEmpty()) {
            decideRecreateUpdate("Explicit strategy configured. Revision annotation not present in stateful set.");
            return Optional.empty();
        }
        // CRD validation ensures the revision is present
        var desiredRevision = UpdateSpec.getRevision(keycloak).orElseThrow();
        if (Objects.equals(maybeCurrentRevision.get(), desiredRevision)) {
            decideRollingUpdate("Explicit strategy configured. Revision matches.");
            return Optional.empty();
        }

        decideRecreateUpdate("Explicit strategy configured. Revision (%s) does not match (%s).".formatted(maybeCurrentRevision.get(), desiredRevision));
        return Optional.empty();
    }
}
