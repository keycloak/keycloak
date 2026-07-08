/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.infinispan.health.site;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SiteHealthImplTest {

    private static final String SITE_A = "site-a";
    private static final String SITE_B = "site-b";

    private StubSiteStorage storage;

    @Before
    public void setUp() {
        storage = new StubSiteStorage();
    }

    private SiteHealthImpl createSite(StubInfinispanManagement management) {
        return createSite(management, null);
    }

    private SiteHealthImpl createSite(StubInfinispanManagement management, Runnable onBecameHealthy) {
        return new SiteHealthImpl(storage, management, Runnable::run, s -> {}, onBecameHealthy);
    }

    // --- Initial state ---

    @Test
    public void testInitialStateIsDown() {
        var management = new StubInfinispanManagement(SITE_A);
        var site = createSite(management);

        assertFalse(site.isHealthy());
    }

    // --- HEALTHY state tests ---

    @Test
    public void testHealthyAllSitesReachableStaysHealthy() {
        var management = new StubInfinispanManagement(SITE_A);
        var site = createSite(management);

        site.triggerClusterHealthCheck();

        assertTrue(site.isHealthy());
        assertEquals(Status.HEALTHY, storage.state.status());
    }

    @Test
    public void testHealthyRemoteSiteUnreachableTransitionsToSuspecting() {
        var management = new StubInfinispanManagement(SITE_A);
        management.connection = new SiteConnection(SITE_A, Set.of(SITE_A));
        var site = createSite(management);

        site.triggerClusterHealthCheck();

        assertTrue(site.isHealthy());
        assertEquals(Status.SUSPECTING, storage.state.status());
        assertEquals(SITE_A, storage.state.activeSite());
    }

    @Test
    public void testHealthyCasFailureStaysHealthy() {
        var management = new StubInfinispanManagement(SITE_A);
        management.connection = new SiteConnection(SITE_A, Set.of(SITE_A));
        storage.casFailure = true;
        var site = createSite(management);

        site.triggerClusterHealthCheck();

        assertTrue(site.isHealthy());
        assertEquals(Status.HEALTHY, storage.state.status());
    }

    // --- SUSPECTING state tests (non-active site) ---

    @Test
    public void testSuspectingNonActiveSiteFirstRoundWaitsForActiveSite() {
        storage.state = SiteState.suspecting(SITE_B);
        var management = new StubInfinispanManagement(SITE_A);
        management.connection = new SiteConnection(SITE_A, Set.of(SITE_A));
        var site = createSite(management);

        site.triggerClusterHealthCheck();

        assertTrue(site.isHealthy());
        assertEquals(Status.SUSPECTING, storage.state.status());
        assertEquals(SITE_B, storage.state.activeSite());
    }

    @Test
    public void testSuspectingNonActiveSiteActiveSiteProgressedNoSteal() {
        storage.state = SiteState.suspecting(SITE_B);
        var management = new StubInfinispanManagement(SITE_A);
        management.connection = new SiteConnection(SITE_A, Set.of(SITE_A));
        var site = createSite(management);

        // round 1: records transitionState
        site.triggerClusterHealthCheck();
        assertEquals(SITE_B, storage.state.activeSite());

        // active site (SITE_B) progressed to UNHEALTHY between rounds
        storage.state = SiteState.unhealthy(SITE_B);

        // round 2: state changed, goes to onUnhealthy, not stealing
        site.triggerClusterHealthCheck();
        assertEquals(Status.UNHEALTHY, storage.state.status());
        assertEquals(SITE_B, storage.state.activeSite());
        assertFalse(site.isHealthy());
    }

    @Test
    public void testSuspectingNonActiveSiteActiveSiteCrashedSteals() {
        storage.state = SiteState.suspecting(SITE_B);
        var management = new StubInfinispanManagement(SITE_A);
        management.connection = new SiteConnection(SITE_A, Set.of(SITE_A));
        var site = createSite(management);

        // round 1: records transitionState
        site.triggerClusterHealthCheck();
        assertEquals(SITE_B, storage.state.activeSite());

        // round 2: state unchanged (active site crashed), steal
        site.triggerClusterHealthCheck();
        assertEquals(Status.SUSPECTING, storage.state.status());
        assertEquals(SITE_A, storage.state.activeSite());
    }

    @Test
    public void testSuspectingNonActiveSiteStealsAndProceeds() {
        storage.state = SiteState.suspecting(SITE_B);
        var management = new StubInfinispanManagement(SITE_A);
        management.connection = new SiteConnection(SITE_A, Set.of(SITE_A));
        var site = createSite(management);

        // round 1: records transitionState
        site.triggerClusterHealthCheck();

        // round 2: steal
        site.triggerClusterHealthCheck();
        assertEquals(Status.SUSPECTING, storage.state.status());
        assertEquals(SITE_A, storage.state.activeSite());

        // round 3: now active site, transitionState matches, proceeds to UNHEALTHY
        site.triggerClusterHealthCheck();
        assertEquals(Status.UNHEALTHY, storage.state.status());
        assertEquals(SITE_A, storage.state.activeSite());
        assertTrue(management.disconnectedSites.contains(SITE_B));
    }

    @Test
    public void testSuspectingNonActiveSiteStealCasFailsNoChange() {
        storage.state = SiteState.suspecting(SITE_B);
        var management = new StubInfinispanManagement(SITE_A);
        management.connection = new SiteConnection(SITE_A, Set.of(SITE_A));
        var site = createSite(management);

        // round 1: records transitionState
        site.triggerClusterHealthCheck();

        // round 2: steal attempt but CAS fails
        storage.casFailure = true;
        site.triggerClusterHealthCheck();

        assertEquals(Status.SUSPECTING, storage.state.status());
        assertEquals(SITE_B, storage.state.activeSite());
    }

    // --- SUSPECTING state tests (active site) ---

    @Test
    public void testSuspectingActiveSiteFirstRoundWaitsForNextRound() {
        storage.state = SiteState.suspecting(SITE_A);
        var management = new StubInfinispanManagement(SITE_A);
        management.connection = new SiteConnection(SITE_A, Set.of(SITE_A));
        var site = createSite(management);

        site.triggerClusterHealthCheck();

        assertEquals(Status.SUSPECTING, storage.state.status());
        assertTrue(site.isHealthy());
    }

    @Test
    public void testSuspectingActiveSiteSecondRoundSitesRecoveredBackToHealthy() {
        storage.state = SiteState.suspecting(SITE_A);
        var management = new StubInfinispanManagement(SITE_A);
        management.connection = new SiteConnection(SITE_A, Set.of(SITE_A));
        var site = createSite(management);

        // round 1: records transitionState
        site.triggerClusterHealthCheck();

        // round 2: sites are now reachable
        management.connection = new SiteConnection(SITE_A, Set.of(SITE_A, SITE_B));
        site.triggerClusterHealthCheck();

        assertEquals(Status.HEALTHY, storage.state.status());
        assertTrue(site.isHealthy());
    }

    @Test
    public void testSuspectingActiveSiteSecondRoundStillUnreachableTransitionsToUnhealthy() {
        storage.state = SiteState.suspecting(SITE_A);
        var management = new StubInfinispanManagement(SITE_A);
        management.connection = new SiteConnection(SITE_A, Set.of(SITE_A));
        var site = createSite(management);

        // round 1: records transitionState
        site.triggerClusterHealthCheck();

        // round 2: still unreachable
        site.triggerClusterHealthCheck();

        assertEquals(Status.UNHEALTHY, storage.state.status());
        assertEquals(SITE_A, storage.state.activeSite());
        assertTrue(management.disconnectedSites.contains(SITE_B));
    }

    @Test
    public void testSuspectingActiveSiteCasToUnhealthyFailsNoDisconnect() {
        storage.state = SiteState.suspecting(SITE_A);
        var management = new StubInfinispanManagement(SITE_A);
        management.connection = new SiteConnection(SITE_A, Set.of(SITE_A));
        var site = createSite(management);

        // round 1
        site.triggerClusterHealthCheck();

        // round 2 with CAS failure
        storage.casFailure = true;
        site.triggerClusterHealthCheck();

        assertEquals(Status.SUSPECTING, storage.state.status());
        assertTrue(management.disconnectedSites.isEmpty());
    }

    // --- UNHEALTHY state tests ---

    @Test
    public void testUnhealthyNonActiveSiteMarkedDown() {
        storage.state = SiteState.unhealthy(SITE_B);
        var management = new StubInfinispanManagement(SITE_A);
        var site = createSite(management);

        site.triggerClusterHealthCheck();

        assertFalse(site.isHealthy());
    }

    @Test
    public void testUnhealthyActiveSiteSitesStillUnreachableStaysUnhealthy() {
        storage.state = SiteState.unhealthy(SITE_A);
        var management = new StubInfinispanManagement(SITE_A);
        management.connection = new SiteConnection(SITE_A, Set.of(SITE_A));
        management.siteStatuses = Map.of(SITE_B, "offline");
        var site = createSite(management);

        site.triggerClusterHealthCheck();

        assertEquals(Status.UNHEALTHY, storage.state.status());
        assertTrue(site.isHealthy());
    }

    @Test
    public void testUnhealthyActiveSiteSitesReachableButMixedStaysUnhealthy() {
        storage.state = SiteState.unhealthy(SITE_A);
        var management = new StubInfinispanManagement(SITE_A);
        management.siteStatuses = Map.of(SITE_B, "mixed");
        var site = createSite(management);

        site.triggerClusterHealthCheck();

        assertEquals(Status.UNHEALTHY, storage.state.status());
    }

    @Test
    public void testUnhealthyActiveSiteSitesReachableAndOnlineTransitionsToRecovery() {
        storage.state = SiteState.unhealthy(SITE_A);
        var management = new StubInfinispanManagement(SITE_A);
        var site = createSite(management);

        site.triggerClusterHealthCheck();

        assertEquals(Status.RECOVERING, storage.state.status());
        assertEquals(SITE_A, storage.state.activeSite());
    }

    @Test
    public void testUnhealthyActiveSiteCasToRecoveryFailsStaysUnhealthy() {
        storage.state = SiteState.unhealthy(SITE_A);
        storage.casFailure = true;
        var management = new StubInfinispanManagement(SITE_A);
        var site = createSite(management);

        site.triggerClusterHealthCheck();

        assertEquals(Status.UNHEALTHY, storage.state.status());
    }

    // --- RECOVERY state tests ---

    @Test
    public void testRecoveryNonActiveSiteMarkedDown() {
        storage.state = SiteState.recovering(SITE_B);
        var management = new StubInfinispanManagement(SITE_A);
        var site = createSite(management);

        site.triggerClusterHealthCheck();

        assertFalse(site.isHealthy());
    }

    @Test
    public void testRecoveryActiveSiteFirstRoundWaitsForNextRound() {
        storage.state = SiteState.recovering(SITE_A);
        var management = new StubInfinispanManagement(SITE_A);
        var site = createSite(management);

        site.triggerClusterHealthCheck();

        assertEquals(Status.RECOVERING, storage.state.status());
    }

    @Test
    public void testRecoveryActiveSiteSecondRoundAllOnlineTransitionsToHealthy() {
        storage.state = SiteState.recovering(SITE_A);
        var management = new StubInfinispanManagement(SITE_A);
        var site = createSite(management);

        // round 1
        site.triggerClusterHealthCheck();
        // round 2
        site.triggerClusterHealthCheck();

        assertEquals(Status.HEALTHY, storage.state.status());
    }

    @Test
    public void testRecoveryActiveSiteSecondRoundConnectionBrokenFallsBackToUnhealthy() {
        storage.state = SiteState.recovering(SITE_A);
        var management = new StubInfinispanManagement(SITE_A);
        var site = createSite(management);

        // round 1
        site.triggerClusterHealthCheck();

        // round 2: site became unreachable again
        management.connection = new SiteConnection(SITE_A, Set.of(SITE_A));
        site.triggerClusterHealthCheck();

        assertEquals(Status.UNHEALTHY, storage.state.status());
        assertEquals(SITE_A, storage.state.activeSite());
    }

    @Test
    public void testRecoveryActiveSiteSecondRoundCachesNotOnlineFallsBackToUnhealthy() {
        storage.state = SiteState.recovering(SITE_A);
        var management = new StubInfinispanManagement(SITE_A);
        var site = createSite(management);

        // round 1
        site.triggerClusterHealthCheck();

        // round 2: caches went to mixed
        management.siteStatuses = Map.of(SITE_B, "mixed");
        site.triggerClusterHealthCheck();

        assertEquals(Status.UNHEALTHY, storage.state.status());
    }

    @Test
    public void testRecoveryCasToHealthyFailsStaysInRecovery() {
        storage.state = SiteState.recovering(SITE_A);
        var management = new StubInfinispanManagement(SITE_A);
        var site = createSite(management);

        // round 1
        site.triggerClusterHealthCheck();

        // round 2 with CAS failure
        storage.casFailure = true;
        site.triggerClusterHealthCheck();

        assertEquals(Status.RECOVERING, storage.state.status());
    }

    @Test
    public void testRecoveryCasToUnhealthyFailsStaysInRecovery() {
        storage.state = SiteState.recovering(SITE_A);
        var management = new StubInfinispanManagement(SITE_A);
        var site = createSite(management);

        // round 1
        site.triggerClusterHealthCheck();

        // round 2: connection broken but CAS fails
        management.connection = new SiteConnection(SITE_A, Set.of(SITE_A));
        storage.casFailure = true;
        site.triggerClusterHealthCheck();

        assertEquals(Status.RECOVERING, storage.state.status());
    }

    // --- Cross-site disabled / no replication ---

    @Test
    public void testCrossSiteDisabledAlwaysHealthy() {
        var management = new StubInfinispanManagement(SITE_A);
        management.connection = new SiteConnection(null, Set.of());
        management.siteStatuses = Map.of();
        var site = createSite(management);

        site.triggerClusterHealthCheck();

        assertTrue(site.isHealthy());
    }

    @Test
    public void testNoCachesReplicatingAlwaysHealthy() {
        var management = new StubInfinispanManagement(SITE_A);
        management.connection = new SiteConnection(SITE_A, Set.of(SITE_A));
        management.siteStatuses = Map.of();
        var site = createSite(management);

        site.triggerClusterHealthCheck();

        assertTrue(site.isHealthy());
    }

    // --- Exception handling ---

    @Test
    public void testInfinispanConnectionFailureMarkedDown() {
        var management = new StubInfinispanManagement(SITE_A);
        management.throwOnSiteStatus = true;
        var site = createSite(management);

        site.triggerClusterHealthCheck();

        assertFalse(site.isHealthy());
    }

    // --- Full lifecycle tests ---

    @Test
    public void testFullLifecycleHealthySuspectUnhealthyRecoveryHealthy() {
        var management = new StubInfinispanManagement(SITE_A);
        var site = createSite(management);

        // start healthy, all reachable
        site.triggerClusterHealthCheck();
        assertEquals(Status.HEALTHY, storage.state.status());
        assertTrue(site.isHealthy());

        // site B goes down
        management.connection = new SiteConnection(SITE_A, Set.of(SITE_A));

        site.triggerClusterHealthCheck();
        assertEquals(Status.SUSPECTING, storage.state.status());
        assertTrue(site.isHealthy());

        // still down after one round
        site.triggerClusterHealthCheck();
        assertEquals(Status.UNHEALTHY, storage.state.status());
        assertTrue(site.isHealthy());
        assertFalse(management.disconnectedSites.isEmpty());

        // site B comes back, caches online
        management.connection = new SiteConnection(SITE_A, Set.of(SITE_A, SITE_B));
        management.siteStatuses = Map.of(SITE_B, "online");

        site.triggerClusterHealthCheck();
        assertEquals(Status.RECOVERING, storage.state.status());

        // second round of recovery
        site.triggerClusterHealthCheck();
        assertEquals(Status.HEALTHY, storage.state.status());
        assertTrue(site.isHealthy());
    }

    @Test
    public void testFullLifecycleSuspectingRecoversQuickly() {
        var management = new StubInfinispanManagement(SITE_A);
        management.connection = new SiteConnection(SITE_A, Set.of(SITE_A));
        var site = createSite(management);

        // round 1: HEALTHY → SUSPECTING
        site.triggerClusterHealthCheck();
        assertEquals(Status.SUSPECTING, storage.state.status());

        // site recovers before second check
        management.connection = new SiteConnection(SITE_A, Set.of(SITE_A, SITE_B));

        // round 2: SUSPECTING → HEALTHY (quick recovery, no disconnect)
        site.triggerClusterHealthCheck();
        assertEquals(Status.HEALTHY, storage.state.status());
        assertTrue(management.disconnectedSites.isEmpty());
    }

    @Test
    public void testNonActiveSiteFullLifecycle() {
        // SITE_B won the CAS to SUSPECTING
        storage.state = SiteState.suspecting(SITE_B);
        var management = new StubInfinispanManagement(SITE_A);
        management.connection = new SiteConnection(SITE_A, Set.of(SITE_A));
        var site = createSite(management);

        // round 1: non-active site waits
        site.triggerClusterHealthCheck();
        assertTrue(site.isHealthy());

        // SITE_B transitions to UNHEALTHY before SITE_A can steal
        storage.state = SiteState.unhealthy(SITE_B);

        site.triggerClusterHealthCheck();
        assertFalse(site.isHealthy());

        // SITE_B starts recovery
        storage.state = SiteState.recovering(SITE_B);

        site.triggerClusterHealthCheck();
        assertFalse(site.isHealthy());

        // SITE_B completes recovery
        storage.state = SiteState.healthy();

        site.triggerClusterHealthCheck();
        assertTrue(site.isHealthy());
    }

    @Test
    public void testMultipleSiteStatusesPartialOffline() {
        storage.state = SiteState.unhealthy(SITE_A);
        var management = new StubInfinispanManagement(SITE_A);
        management.siteStatuses = Map.of(SITE_B, "online", "site-c", "offline");
        var site = createSite(management);

        site.triggerClusterHealthCheck();

        assertEquals(Status.UNHEALTHY, storage.state.status());
    }

    @Test
    public void testSuspectingDbStateChangedExternallyWaitsAnotherRound() {
        storage.state = SiteState.suspecting(SITE_A);
        var management = new StubInfinispanManagement(SITE_A);
        management.connection = new SiteConnection(SITE_A, Set.of(SITE_A));
        var site = createSite(management);

        // round 1: records transitionState
        site.triggerClusterHealthCheck();

        // external update: someone else changed the DB state (new revision)
        storage.state = SiteState.suspecting(SITE_A);

        // round 2: siteState != transitionState (different revision), waits again
        site.triggerClusterHealthCheck();
        assertEquals(Status.SUSPECTING, storage.state.status());

        // round 3: now transitionState matches, will proceed
        site.triggerClusterHealthCheck();
        assertEquals(Status.UNHEALTHY, storage.state.status());
    }

    // --- Two-site tests ---

    @Test
    public void testTwoSitesBothDetectFailureOnlyOneWinsSuspecting() {
        var mgmtA = new StubInfinispanManagement(SITE_A);
        mgmtA.connection = new SiteConnection(SITE_A, Set.of(SITE_A));
        var siteA = createSite(mgmtA);

        var mgmtB = new StubInfinispanManagement(SITE_B);
        mgmtB.connection = new SiteConnection(SITE_B, Set.of(SITE_B));
        var siteB = createSite(mgmtB);

        // SITE_A detects failure first and wins CAS
        siteA.triggerClusterHealthCheck();
        assertEquals(Status.SUSPECTING, storage.state.status());
        assertEquals(SITE_A, storage.state.activeSite());

        // SITE_B tries but CAS fails (expected HEALTHY, actual SUSPECTING)
        siteB.triggerClusterHealthCheck();
        // SITE_B sees SUSPECTING(activeSite=A), enters non-active path, waits
        assertEquals(Status.SUSPECTING, storage.state.status());
        assertEquals(SITE_A, storage.state.activeSite());
        assertTrue(siteA.isHealthy());
        assertTrue(siteB.isHealthy());
    }

    @Test
    public void testTwoSitesActiveSiteCrashesLoserStealsAndTakesOver() {
        var mgmtA = new StubInfinispanManagement(SITE_A);
        mgmtA.connection = new SiteConnection(SITE_A, Set.of(SITE_A));
        var siteA = createSite(mgmtA);

        var mgmtB = new StubInfinispanManagement(SITE_B);
        mgmtB.connection = new SiteConnection(SITE_B, Set.of(SITE_B));
        var siteB = createSite(mgmtB);

        // SITE_A detects failure first
        siteA.triggerClusterHealthCheck();
        assertEquals(SITE_A, storage.state.activeSite());

        // SITE_B sees SUSPECTING(A), round 1: waits
        siteB.triggerClusterHealthCheck();

        // SITE_A crashes (no more checks from A)

        // SITE_B round 2: state unchanged, steals
        siteB.triggerClusterHealthCheck();
        assertEquals(Status.SUSPECTING, storage.state.status());
        assertEquals(SITE_B, storage.state.activeSite());

        // SITE_B round 3: now active, proceeds to UNHEALTHY
        siteB.triggerClusterHealthCheck();
        assertEquals(Status.UNHEALTHY, storage.state.status());
        assertEquals(SITE_B, storage.state.activeSite());
        assertFalse(mgmtB.disconnectedSites.isEmpty());
    }

    @Test
    public void testTwoSitesActiveSiteProgressesLoserDoesNotSteal() {
        var mgmtA = new StubInfinispanManagement(SITE_A);
        mgmtA.connection = new SiteConnection(SITE_A, Set.of(SITE_A));
        var siteA = createSite(mgmtA);

        var mgmtB = new StubInfinispanManagement(SITE_B);
        mgmtB.connection = new SiteConnection(SITE_B, Set.of(SITE_B));
        var siteB = createSite(mgmtB);

        // SITE_A detects and wins CAS to SUSPECTING
        siteA.triggerClusterHealthCheck();

        // SITE_B round 1: waits
        siteB.triggerClusterHealthCheck();
        assertEquals(SITE_A, storage.state.activeSite());

        // SITE_A progresses to UNHEALTHY
        siteA.triggerClusterHealthCheck();
        assertEquals(Status.UNHEALTHY, storage.state.status());

        // SITE_B round 2: state changed (now UNHEALTHY), no steal, goes down
        siteB.triggerClusterHealthCheck();
        assertFalse(siteB.isHealthy());
        assertTrue(siteA.isHealthy());
    }

    // --- onBecameHealthy callback tests ---

    @Test
    public void testOnBecameHealthyInvokedWhenHealthySwitchesFromFalseToTrue() {
        var counter = new AtomicInteger(0);
        var management = new StubInfinispanManagement(SITE_A);
        var site = createSite(management, counter::incrementAndGet);

        assertFalse(site.isHealthy());

        site.triggerClusterHealthCheck();

        assertTrue(site.isHealthy());
        assertEquals(1, counter.get());
    }

    @Test
    public void testOnBecameHealthyNotInvokedWhenAlreadyHealthy() {
        var counter = new AtomicInteger(0);
        var management = new StubInfinispanManagement(SITE_A);
        var site = createSite(management, counter::incrementAndGet);

        site.triggerClusterHealthCheck();
        assertEquals(1, counter.get());

        // second check: already healthy, should not invoke again
        site.triggerClusterHealthCheck();
        assertEquals(1, counter.get());
    }

    @Test
    public void testOnBecameHealthyInvokedAgainAfterHealthyBecomeFalse() {
        var counter = new AtomicInteger(0);
        var management = new StubInfinispanManagement(SITE_A);
        var site = createSite(management, counter::incrementAndGet);

        // become healthy
        site.triggerClusterHealthCheck();
        assertTrue(site.isHealthy());
        assertEquals(1, counter.get());

        // site goes down, trigger failure
        management.throwOnSiteStatus = true;
        site.triggerClusterHealthCheck();
        assertFalse(site.isHealthy());

        // site comes back, healthy again
        management.throwOnSiteStatus = false;
        site.triggerClusterHealthCheck();
        assertTrue(site.isHealthy());
        assertEquals(2, counter.get());
    }

    // --- Stubs ---

    private static class StubInfinispanManagement implements InfinispanManagement {
        SiteConnection connection;
        Map<String, String> siteStatuses;
        List<String> disconnectedSites = new ArrayList<>();
        boolean throwOnSiteStatus = false;

        StubInfinispanManagement(String localSite) {
            var remoteSite = SITE_A.equals(localSite) ? SITE_B : SITE_A;
            connection = new SiteConnection(localSite, Set.of(SITE_A, SITE_B));
            siteStatuses = Map.of(remoteSite, "online");
        }

        @Override
        public Map<String, String> siteStatus() throws ExecutionException {
            if (throwOnSiteStatus) {
                throw new ExecutionException("simulated failure", new RuntimeException());
            }
            return siteStatuses;
        }

        @Override
        public SiteConnection siteConnection() {
            return connection;
        }

        @Override
        public void disconnect(Collection<String> remoteSites) {
            disconnectedSites.addAll(remoteSites);
        }

        @Override
        public void close() {
        }
    }

    private static class StubSiteStorage implements SiteStorage {
        SiteState state = SiteState.healthy();
        boolean casFailure = false;

        @Override
        public SiteState get() {
            return state;
        }

        @Override
        public boolean compareAndSet(SiteState expectedState, SiteState newState) {
            if (casFailure) {
                return false;
            }
            if (state.equals(expectedState)) {
                state = newState;
                return true;
            }
            return false;
        }
    }

    private static class ExecutionException extends java.util.concurrent.ExecutionException {
        ExecutionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
