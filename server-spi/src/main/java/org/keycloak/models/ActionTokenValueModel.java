/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models;

import org.keycloak.storage.SearchableModelField;

import java.util.Map;

/**
 * This model represents contents of an action token shareable among Keycloak instances in the cluster.
 * @author hmlnarik
 */
public interface ActionTokenValueModel {

    class SearchableFields {
        public static final SearchableModelField<ActionTokenValueModel> ID                          = new SearchableModelField<>("id", String.class);
        public static final SearchableModelField<ActionTokenValueModel> OBJECT_KEY                  = new SearchableModelField<>("objectKey", String.class);
        public static final SearchableModelField<ActionTokenValueModel> USER_ID                     = new SearchableModelField<>("userId", String.class);
        public static final SearchableModelField<ActionTokenValueModel> ACTION_ID                   = new SearchableModelField<>("actionId", String.class);
        public static final SearchableModelField<ActionTokenValueModel> ACTION_VERIFICATION_NONCE   = new SearchableModelField<>("actionVerificationNonce", String.class);
    }
    
    /**
     * Returns unmodifiable map of all notes.
     * @return see description. Returns empty map if no note is set, never returns {@code null}.
     */
    Map<String,String> getNotes();

    /**
     * Returns value of the given note (or {@code null} when no note of this name is present)
     * @return see description
     */
    String getNote(String name);
}
