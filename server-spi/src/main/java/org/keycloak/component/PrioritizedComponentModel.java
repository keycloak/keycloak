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
package org.keycloak.component;

import java.util.Comparator;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class PrioritizedComponentModel extends ComponentModel {
    public static final String PRIORITY = "priority";
    public static Comparator<ComponentModel> comparator = new Comparator<ComponentModel>() {
        @Override
        public int compare(ComponentModel o1, ComponentModel o2) {
            return parsePriority(o1) - parsePriority(o2);
        }
    };

    public PrioritizedComponentModel(ComponentModel copy) {
        super(copy);
    }

    public PrioritizedComponentModel() {
    }

    public static int parsePriority(ComponentModel component) {
        String priority = component.getConfig().getFirst(PRIORITY);
        if (priority == null) return 0;
        return Integer.valueOf(priority);

    }

    public int getPriority() {
        return parsePriority(this);

    }

    public void setPriority(int priority) {
        getConfig().putSingle("priority", Integer.toString(priority));
    }
}
