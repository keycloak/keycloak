/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.sessions.infinispan.initializer;

import java.time.Instant;
import java.util.concurrent.TimeUnit;
import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class CacheInitializer {

    private static final Logger log = Logger.getLogger(CacheInitializer.class);

    public void loadSessions() {
        Instant loadingMustContinueBy = Instant.now().plusSeconds(getStalledTimeoutInSeconds());
        boolean loadingStalledInPreviousStep = false;
        int lastProgressIndicator = 0;
        while (!isFinished()) {
            if (!isCoordinator()) {
                try {
                    TimeUnit.SECONDS.sleep(1);

                    final int progressIndicator = getProgressIndicator();
                    final boolean loadingStalled = lastProgressIndicator == progressIndicator;
                    if (loadingStalled) {
                        if (loadingStalledInPreviousStep) {
                            if (Instant.now().isAfter(loadingMustContinueBy)) {
                                throw new RuntimeException("Loading sessions has stalled for " + getStalledTimeoutInSeconds() + " seconds, possibly caused by split-brain");
                            }

                            log.tracef("Loading sessions stalled. Waiting until %s", loadingMustContinueBy);
                        } else {
                            loadingMustContinueBy = Instant.now().plusSeconds(getStalledTimeoutInSeconds());
                            loadingStalledInPreviousStep = true;
                        }
                    } else {
                        loadingStalledInPreviousStep = false;
                    }

                    lastProgressIndicator = progressIndicator;
                } catch (InterruptedException ie) {
                    log.error("Interrupted", ie);
                    throw new RuntimeException("Loading sessions failed", ie);
                }
            } else {
                startLoading();
            }
        }
    }


    protected abstract boolean isFinished();

    protected abstract boolean isCoordinator();

    /**
     * Returns an integer which captures current progress. If there is a progress in loading,
     * this indicator must be different most of the time so that it does not hit 30-seconds
     * limit.
     * @see #stalledTimeoutInSeconds
     * @return
     */
    protected abstract int getProgressIndicator();

    /**
     * Just coordinator will run this
     */
    protected abstract void startLoading();

    protected abstract int getStalledTimeoutInSeconds();
}
