/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models.utils;

import java.util.Comparator;

import org.keycloak.models.UserModel;

/**
 * TODO KEYCLOAK-6799 unit test for the comparator.
 *
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class UserModelByCreatedTimestampComparator implements Comparator<UserModel> {
    
    public static UserModelByCreatedTimestampComparator INSTANCE = new UserModelByCreatedTimestampComparator();
    
    private UserModelByCreatedTimestampComparator() { }
    
    @Override
    public int compare(UserModel o1, UserModel o2) {
        if(o2.getCreatedTimestamp() == null) {
            return -1;
        } else if(o1.getCreatedTimestamp() == null) {
            return 1;
        } else {
            return o1.getCreatedTimestamp().compareTo(o2.getCreatedTimestamp());
        }
    };
};