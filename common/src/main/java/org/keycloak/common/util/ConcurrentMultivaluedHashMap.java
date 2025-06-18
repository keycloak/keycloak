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

package org.keycloak.common.util;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@SuppressWarnings("serial")
public class ConcurrentMultivaluedHashMap<K, V> extends ConcurrentHashMap<K, List<V>> implements MultivaluedMap<K, V>
{
    public ConcurrentMultivaluedHashMap() {
    }

    public ConcurrentMultivaluedHashMap(Map<K, List<V>> map) {
        if (map == null) {
            throw new IllegalArgumentException("Map can not be null");
        }
        putAll(map);
    }

   @Override
   public List<V> createListInstance() {
      return new CopyOnWriteArrayList<>();
   }

}
