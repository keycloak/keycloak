/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models.map.storage.file.common;

import java.util.LinkedList;
import java.util.function.Supplier;

/**
 * A special stack suited for tracking the parser of a block language, and maintaining
 * contextual information for block nesting position in the YAML file.
 * <p>
 * The intention is as follows:
 * Initially, it contains a single {@link BlockContext} instance which represents
 * the root context of the YAML tree. Every sequence item and mapping value
 * in the YAML file leads to pushing a new {@link BlockContext} onto the stack
 * which is created by the topmost {@link BlockContext#getContext(java.lang.String)}
 * method of the topmost {@link BlockContext}. This context is removed from the stack
 * once parsing of the respective sequence item or mapping pair is finished.
 *
 * @author hmlnarik
 */
public class BlockContextStack extends LinkedList<BlockContext<?>> {

    public BlockContextStack(BlockContext<?> rootElement) {
        push(rootElement);
    }

    /**
     * Pushes the subcontext to the stack.
     * <p>
     * The subcontext is created by calling {@link BlockContext#getContext(java.lang.String)}
     * method. If this method returns {@code null}, the control reverts to producing
     * the subcontext using {@code nullProducer} which must return a valid {@link BlockContext}
     * object (it <b>must not</b> return {@code null).
     *
     * @param name
     * @param nullProducer
     * @return
     */
    public BlockContext<?> push(String name, Supplier<BlockContext<?>> nullProducer) {
        BlockContext<?> context = peek().getContext(name);
        if (context == null) {
            context = nullProducer.get();
        }
        push(context);
        return context;
    }
}
