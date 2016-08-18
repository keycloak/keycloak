package org.keycloak.authorization.policy.provider.drools;

import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.policy.evaluation.Evaluation;
import org.kie.api.KieServices;
import org.kie.api.builder.KieScanner;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;

import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
class DroolsPolicy {

    private static final int SESSION_POOL_SIZE = 10;

    private final KieContainer kc;
    private final KieScanner kcs;
    private final String sessionName;

    DroolsPolicy(KieServices ks, Policy associatedPolicy) {
        String groupId = associatedPolicy.getConfig().get("mavenArtifactGroupId");
        String artifactId = associatedPolicy.getConfig().get("mavenArtifactId");
        String version = associatedPolicy.getConfig().get("mavenArtifactVersion");
        String scannerPeriod = associatedPolicy.getConfig().get("scannerPeriod");
        String scannerPeriodUnit = associatedPolicy.getConfig().get("scannerPeriodUnit");
        this.sessionName = associatedPolicy.getConfig().get("sessionName");

        this.kc = ks.newKieContainer(ks.newReleaseId(groupId, artifactId, version));
        this.kcs = ks.newKieScanner(this.kc);
        this.kcs.start(toMillis(scannerPeriod, scannerPeriodUnit));

        KieSession session = this.kc.newKieSession(this.sessionName);

        if (session == null) {
            throw new RuntimeException("Could not obtain session with name [" + this.sessionName + "].");
        }

        session.dispose();
    }

    void evaluate(Evaluation evaluation) {
        KieSession session = this.kc.newKieSession(this.sessionName);

        session.insert(evaluation);
        session.fireAllRules();

        session.dispose();
    }

    void dispose() {
        this.kcs.stop();
    }

    private long toMillis(final String scannerPeriod, final String scannerPeriodUnit) {
        switch (scannerPeriodUnit) {
            case "Seconds":
                return TimeUnit.SECONDS.toMillis(Integer.valueOf(scannerPeriod));
            case "Minutes":
                return TimeUnit.MINUTES.toMillis(Integer.valueOf(scannerPeriod));
            case "Hours":
                return TimeUnit.HOURS.toMillis(Integer.valueOf(scannerPeriod));
            case "Days":
                return TimeUnit.DAYS.toMillis(Integer.valueOf(scannerPeriod));
        }

        throw new RuntimeException("Invalid time period [" + scannerPeriodUnit + "].");
    }
}
