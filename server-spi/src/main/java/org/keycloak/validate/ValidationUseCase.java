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
package org.keycloak.validate;

/**
 * Denotes the use-case in which the Validation takes place to allow generic validations to perform use-case specific actions.
 * <p>
 * This is an interface instead of an {@code enum}, to allow users to provide their own {@link ValidationUseCase} implementations.
 */
public interface ValidationUseCase {

    /**
     * The name of the use case.
     *
     * @return
     */
    String name();

    /**
     * The action type
     *
     * @return
     */
    ActionType getActionType();

    /**
     * Common {@link ValidationUseCase ValidationUseCases} within Keycloak.
     */
    enum Common implements ValidationUseCase {

        USER_REGISTRATION(ActionType.Common.CREATE),

        USER_PROFILE_UPDATE(ActionType.Common.UPDATE);

        /**
         * Holds the {@link ActionType} associated with the use case.
         */
        private final ActionType actionType;

        Common(ActionType actionType) {
            this.actionType = actionType;
        }

        @Override
        public ActionType getActionType() {
            return actionType;
        }
    }

}