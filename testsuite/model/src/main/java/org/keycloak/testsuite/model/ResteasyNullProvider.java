/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.model;

import org.keycloak.common.util.ResteasyProvider;

/**
 * @author <a href="mailto:mkanis@redhat.com">Martin Kanis</a>
 */
public class ResteasyNullProvider implements ResteasyProvider {

    @Override
    public <R> R getContextData(Class<R> type) {
        return null;
    }

    @Override
    public void pushDefaultContextObject(Class type, Object instance) {

    }

    @Override
    public void pushContext(Class type, Object instance) {

    }

    @Override
    public void clearContextData() {

    }
}
