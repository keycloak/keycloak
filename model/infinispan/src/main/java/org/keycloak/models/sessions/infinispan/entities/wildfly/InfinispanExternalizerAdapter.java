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
 *
 */

package org.keycloak.models.sessions.infinispan.entities.wildfly;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.wildfly.clustering.marshalling.Externalizer;

/**
 * Abstract subclass for Wildfly externalizers. It is adapter from {@link org.infinispan.commons.marshall.Externalizer}
 * to {@link org.wildfly.clustering.marshalling.Externalizer}
 *
 * TODO: Remove this class (and probably whole package org.keycloak.models.sessions.infinispan.entities.wildfly once
 * migrating to Wildfly 21 and infinispan protobuf marshallers)
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class InfinispanExternalizerAdapter<T> implements Externalizer<T> {

    private final Class<T> targetClass;
    private final org.infinispan.commons.marshall.Externalizer<T> delegate;


    InfinispanExternalizerAdapter(Class<T> targetClass, org.infinispan.commons.marshall.Externalizer<T> delegate) {
        this.targetClass = targetClass;
        this.delegate = delegate;
    }

    @Override
    public void writeObject(ObjectOutput output, T object) throws IOException {
        delegate.writeObject(output, object);
    }

    @Override
    public T readObject(ObjectInput input) throws IOException, ClassNotFoundException {
        return delegate.readObject(input);
    }

    @Override
    public Class<T> getTargetClass() {
        return targetClass;
    }
}
