/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.provider.quarkus;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.keycloak.platform.PlatformProvider;

public class QuarkusPlatform implements PlatformProvider {

    Runnable startupHook;
    Runnable shutdownHook;

    private AtomicBoolean started = new AtomicBoolean(false);
    private List<Throwable> deferredExceptions = new CopyOnWriteArrayList<>();

    @Override
    public void onStartup(Runnable startupHook) {
        this.startupHook = startupHook;
    }

    @Override
    public void onShutdown(Runnable shutdownHook) {
        this.shutdownHook = shutdownHook;
    }

    @Override
    public void exit(Throwable cause) {
        throw new RuntimeException(cause);
    }

    /**
     * Called when Quarkus platform is started
     */
    public void started() {
        this.started.set(true);
    }

    public boolean isStarted() {
        return started.get();
    }

    /**
     * Add the exception, which  won't be thrown right-away, but should be thrown later after QuarkusPlatform is initialized (including proper logging)
     *
     * @param t
     */
    public void addDeferredException(Throwable t) {
        deferredExceptions.add(t);
    }

    public List<Throwable> getDeferredExceptions() {
        return deferredExceptions;
    }

}
