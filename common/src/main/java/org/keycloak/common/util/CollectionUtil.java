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
import java.util.Iterator;

/**
 * @author <a href="mailto:jeroen.rosenberg@gmail.com">Jeroen Rosenberg</a>
 */
public class CollectionUtil {

    public static String join(Collection<String> strings) {
        return join(strings, ", ");
    }

    public static String join(Collection<String> strings, String separator) {
        Iterator<String> iter = strings.iterator();
        StringBuilder sb = new StringBuilder();
        if(iter.hasNext()){
            sb.append(iter.next());
            while(iter.hasNext()){
                sb.append(separator).append(iter.next());
            }
        }
        return sb.toString();
    }

    // Return true if all items from col1 are in col2 and viceversa. Order is not taken into account
    public static <T> boolean collectionEquals(Collection<T> col1, Collection<T> col2) {
        if (col1.size() != col2.size()) {
            return false;
        }

        for (T item : col1) {
            if (!col2.contains(item)) {
                return false;
            }
        }

        return true;
    }
}
