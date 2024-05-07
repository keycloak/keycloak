/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.storage.attributes;

import org.keycloak.component.ComponentFactory;

/**
 * @author Ben Cresitello-Dittmar
 *
 * Factory for creating instances of {@link AttributeMapper}.
 *
 * @param <T> The type of mapper that this factory will create
 */
public interface AttributeMapperFactory<T extends AttributeMapper> extends ComponentFactory<T, AttributeMapper> {}
