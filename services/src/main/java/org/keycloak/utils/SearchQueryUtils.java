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

package org.keycloak.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class SearchQueryUtils {

    public static final String SEARCH_ID_PREFIX = "id:";

    public static final String SEARCH_USERNAME_PREFIX = "username:";

    public static final String SEARCH_EMAIL_PREFIX = "email:";

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    public enum UserSearchPrefix {
        ID(SEARCH_ID_PREFIX, UserProvider::getUserById),
        USERNAME(SEARCH_USERNAME_PREFIX, UserProvider::getUserByUsername),
        EMAIL(SEARCH_EMAIL_PREFIX, UserProvider::getUserByEmail);

        private final String prefix;
        private final UserLookup lookup;

        UserSearchPrefix(String prefix, UserLookup lookup) {
            this.prefix = prefix;
            this.lookup = lookup;
        }

        public String getPrefix() {
            return prefix;
        }

        public UserModel lookup(UserProvider users, RealmModel realm, String term) {
            return lookup.apply(users, realm, term);
        }

        public String[] splitTerms(String search) {
            return WHITESPACE.split(search.substring(prefix.length()).trim());
        }

        public static UserSearchPrefix matching(String search) {
            for (UserSearchPrefix p : values()) {
                if (search.startsWith(p.prefix)) {
                    return p;
                }
            }
            return null;
        }

        @FunctionalInterface
        private interface UserLookup {
            UserModel apply(UserProvider users, RealmModel realm, String term);
        }
    }

    public static Map<String, String> getFields(final String query) {
        Map<String, String> ret = new HashMap<>();
        char[] chars = query.trim().toCharArray();
        for (int i = 0; i < chars.length; i++) {
            boolean inQuotes = false;
            boolean internal = false;
            String name = "";
            while (i < chars.length && chars[i] != ':') {
                if (chars[i] == '\\') {
                    if (chars[i+1] == '\"') {
                        i++;
                    }
                    else if (chars[i+1] == '\\') {
                        i+=2;
                        continue;
                    }
                }
                else if (chars[i] == '\"') {
                        if(!inQuotes && name.length() > 0) {
                            internal = true;
                        }
                        else if(internal) {
                            internal = false;
                        }
                        else {
                            inQuotes = !inQuotes;
                            i++;
                            continue;
                        }
                }
                else if(chars[i] == ' ' && !inQuotes) {
                    break;
                }
                name += chars[i];
                i++;
            }
            if(i == chars.length || chars[i] == ' ') {
                continue;
            }
            i++;
            inQuotes = false;
            internal = false;
            String value = "";
            while (i < chars.length) {
                if (chars[i] == '\\') {
                    if (chars[i+1] == '\"') {
                        i++;
                    }
                    else if (chars[i+1] == '\\') {
                        i+=2;
                        continue;
                    }
                }
                else if (chars[i] == '\"') {
                    if(!inQuotes && value.length() > 0) {
                        internal = true;
                    }
                    else if(internal) {
                        internal = false;
                    }
                    else {
                        inQuotes = !inQuotes;
                        i++;
                        continue;
                    }
                }
                else if(chars[i] == ' ' && !inQuotes) {
                    break;
                }
                value += chars[i];
                i++;
            }
            ret.put(name, value);
        }
        return ret;
    }
}
