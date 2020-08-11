/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.utils;

import org.jboss.logging.Logger;
import org.keycloak.executors.ExecutorsProvider;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Utility class for general helper methods used across the keycloak-services.
 */
public class ServicesUtils {

    private static final Logger logger = Logger.getLogger(ServicesUtils.class);

    public static <T, R> Function<? super T,? extends Stream<? extends R>> timeBound(KeycloakSession session,
                                                                                     long timeout,
                                                                                     Function<T, ? extends Stream<R>> func) {
        ExecutorService executor = session.getProvider(ExecutorsProvider.class).getExecutor("storage-provider-threads");
        return p -> {
            Callable<? extends Stream<R>> c = () -> func.apply(p);
            Future<? extends Stream<R>> future = executor.submit(c);
            try {
                return future.get(timeout, TimeUnit.MILLISECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                future.cancel(true);
                logger.debug("Function failed to return on time.", e);
                return Stream.empty();
            }
        };
    }

    public static GroupRepresentation groupToBriefRepresentation(GroupModel g) {
        return ModelToRepresentation.toRepresentation(g, false);
    }
}
