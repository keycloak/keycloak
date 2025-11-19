/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.operator;

import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import org.keycloak.operator.controllers.KeycloakDistConfigurator;
import org.keycloak.operator.controllers.WatchedResources;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;
import org.keycloak.operator.crds.v2alpha1.realmimport.KeycloakRealmImport;
import org.keycloak.operator.update.UpdateType;

import java.util.Optional;

public final class ContextUtils {

    // context keys
    public static final String KEYCLOAK = "keycloak";
    public static final String OLD_DEPLOYMENT_KEY = "current_stateful_set";
    public static final String NEW_DEPLOYMENT_KEY = "desired_new_stateful_set";
    public static final String UPDATE_TYPE_KEY = "update_type";
    public static final String UPDATE_REASON_KEY = "update_reason";
    public static final String OPERATOR_CONFIG_KEY = "operator_config";
    public static final String WATCHED_RESOURCES_KEY = "watched_resources";
    public static final String DIST_CONFIGURATOR_KEY = "dist_configurator";

    private ContextUtils() {}


    public static void storeCurrentStatefulSet(Context<?> context, StatefulSet statefulSet) {
        context.managedWorkflowAndDependentResourceContext().put(OLD_DEPLOYMENT_KEY, statefulSet);
    }

    public static Optional<StatefulSet> getCurrentStatefulSet(Context<?> context) {
        return context.managedWorkflowAndDependentResourceContext().get(OLD_DEPLOYMENT_KEY, StatefulSet.class);
    }

    public static void storeDesiredStatefulSet(Context<?> context, StatefulSet statefulSet) {
        context.managedWorkflowAndDependentResourceContext().put(NEW_DEPLOYMENT_KEY, statefulSet);
    }

    public static StatefulSet getDesiredStatefulSet(Context<?> context) {
        return context.managedWorkflowAndDependentResourceContext().getMandatory(NEW_DEPLOYMENT_KEY, StatefulSet.class);
    }

    public static void storeUpdateType(Context<?> context, UpdateType updateType, String reason) {
        context.managedWorkflowAndDependentResourceContext().put(UPDATE_TYPE_KEY, updateType);
        context.managedWorkflowAndDependentResourceContext().put(UPDATE_REASON_KEY, reason);
    }

    public static Optional<UpdateType> getUpdateType(Context<?> context) {
        return context.managedWorkflowAndDependentResourceContext().get(UPDATE_TYPE_KEY, UpdateType.class);
    }

    public static String getUpdateReason(Context<?> context) {
        return context.managedWorkflowAndDependentResourceContext().getMandatory(UPDATE_REASON_KEY, String.class);
    }

    public static void storeOperatorConfig(Context<?> context, Config operatorConfig) {
        context.managedWorkflowAndDependentResourceContext().put(OPERATOR_CONFIG_KEY, operatorConfig);
    }

    public static Config getOperatorConfig(Context<?> context) {
        return context.managedWorkflowAndDependentResourceContext().getMandatory(OPERATOR_CONFIG_KEY, Config.class);
    }

    public static void storeWatchedResources(Context<?> context, WatchedResources watchedResources) {
        context.managedWorkflowAndDependentResourceContext().put(WATCHED_RESOURCES_KEY, watchedResources);
    }

    public static WatchedResources getWatchedResources(Context<?> context) {
        return context.managedWorkflowAndDependentResourceContext().getMandatory(WATCHED_RESOURCES_KEY, WatchedResources.class);
    }

    public static void storeDistConfigurator(Context<?> context, KeycloakDistConfigurator distConfigurator) {
        context.managedWorkflowAndDependentResourceContext().put(DIST_CONFIGURATOR_KEY, distConfigurator);
    }

    public static KeycloakDistConfigurator getDistConfigurator(Context<?> context) {
        return context.managedWorkflowAndDependentResourceContext().getMandatory(DIST_CONFIGURATOR_KEY, KeycloakDistConfigurator.class);
    }

    public static void storeKeycloak(Context<KeycloakRealmImport> context, Keycloak existingKeycloak) {
        context.managedWorkflowAndDependentResourceContext().put(KEYCLOAK, existingKeycloak);
    }

    public static Keycloak getKeycloak(Context<?> context) {
        return context.managedWorkflowAndDependentResourceContext().getMandatory(KEYCLOAK, Keycloak.class);
    }
}
