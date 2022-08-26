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
package org.keycloak.models.map.storage.hotRod.common;

import org.infinispan.query.dsl.Query;

/**
 * @author <a href="mailto:mkanis@redhat.com">Martin Kanis</a>
 */
public class HotRodUtils {

    public static final int DEFAULT_MAX_RESULTS = Integer.MAX_VALUE >> 1;

    public static <T> Query<T> paginateQuery(Query<T> query, Integer first, Integer max) {
        if (first != null && first > 0) {
            query = query.startOffset(first);

            // workaround because of ISPN-13702 bug, see https://github.com/keycloak/keycloak/issues/10090
            if (max == null || max < 0) {
                max = DEFAULT_MAX_RESULTS;
            }
        }

        if (max != null && max >= 0) {
            query = query.maxResults(max);
        }

        return query;
    }
}
