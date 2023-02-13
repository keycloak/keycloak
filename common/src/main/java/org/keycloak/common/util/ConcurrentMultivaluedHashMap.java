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

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@SuppressWarnings("serial")
public class ConcurrentMultivaluedHashMap<K, V> extends ConcurrentHashMap<K, List<V>>
{
   public void putSingle(K key, V value)
   {
      List<V> list = createListInstance();
      list.add(value);
      put(key, list); // Just override with new List instance
   }

   public void addAll(K key, V... newValues)
   {
      for (V value : newValues)
      {
         add(key, value);
      }
   }

   public void addAll(K key, List<V> valueList)
   {
      for (V value : valueList)
      {
         add(key, value);
      }
   }

   public void addFirst(K key, V value)
   {
      List<V> list = get(key);
      if (list == null)
      {
         add(key, value);
      }
      else
      {
         list.add(0, value);
      }
   }
   public final void add(K key, V value)
   {
      getList(key).add(value);
   }


   public final void addMultiple(K key, Collection<V> values)
   {
      getList(key).addAll(values);
   }

   public V getFirst(K key)
   {
      List<V> list = get(key);
      return list == null ? null : list.get(0);
   }

   public final List<V> getList(K key)
   {
      List<V> list = get(key);

      if (list == null) {
         list = createListInstance();
         List<V> existing = putIfAbsent(key, list);
         if (existing != null) {
            list = existing;
         }
      }

      return list;
   }

   public void addAll(ConcurrentMultivaluedHashMap<K, V> other)
   {
      for (Entry<K, List<V>> entry : other.entrySet())
      {
         getList(entry.getKey()).addAll(entry.getValue());
      }
   }

   protected List<V> createListInstance() {
      return new CopyOnWriteArrayList<>();
   }

}
