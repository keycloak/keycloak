package org.keycloak.util;

import java.util.HashSet;
import java.util.Set;

import org.keycloak.Config;

import static org.keycloak.Config.Scope;

public class ConfigUtil {

    public static class CompositeScope implements Scope {

        private Scope[] scopes;

        public CompositeScope(Scope... scopes) {
            this.scopes = scopes == null ? new Scope[0] : scopes;
        }

        @Override
        public String get(String key) {
            return this.get(key, null);
        }

        @Override
        public String get(String key, String defaultValue) {
            for (Scope scope : this.scopes) {
                String value = scope.get(key);
                if (value != null) {
                    return value;
                }
            }
            return defaultValue;
        }

        @Override
        public String[] getArray(String key) {
            for (Scope scope : this.scopes) {
                String[] value = scope.getArray(key);
                if (value != null) {
                    return value;
                }
            }
            return null;
        }

        @Override
        public Integer getInt(String key) {
            return this.getInt(key, null);
        }

        @Override
        public Integer getInt(String key, Integer defaultValue) {
            for (Scope scope : this.scopes) {
                Integer value = scope.getInt(key);
                if (value != null) {
                    return value;
                }
            }
            return defaultValue;
        }

        @Override
        public Long getLong(String key) {
            return this.getLong(key, null);
        }

        @Override
        public Long getLong(String key, Long defaultValue) {
            for (Scope scope : this.scopes) {
                Long value = scope.getLong(key);
                if (value != null) {
                    return value;
                }
            }
            return defaultValue;
        }

        @Override
        public Boolean getBoolean(String key) {
            return this.getBoolean(key, null);
        }

        @Override
        public Boolean getBoolean(String key, Boolean defaultValue) {
            for (Scope scope : this.scopes) {
                Boolean value = scope.getBoolean(key);
                if (value != null) {
                    return value;
                }
            }
            return defaultValue;
        }

        @Override
        public Scope scope(String... scope) {
            Scope[] subScopes = new Scope[this.scopes.length];
            for (int i = 0; i < this.scopes.length; i++) {
                subScopes[i] = this.scopes[i].scope(scope);
            }
            return new CompositeScope(subScopes);
        }

        @Override
        public Set<String> getPropertyNames() {
            Set<String> propertyNames = new HashSet<>();
            for (Scope scope : this.scopes) {
                Set<String> scopePropertyNames = scope.getPropertyNames();
                if (scopePropertyNames != null) {
                    propertyNames.addAll(scopePropertyNames);
                }
            }
            return propertyNames;
        }
    }

    public static Scope getProviderScope(final String spiName, final String provider, final String profile) {
        Scope rootScope = Config.scope(spiName, provider);
        Scope profileScope = rootScope.scope("profile", profile);
        return new CompositeScope(profileScope, rootScope);
    }
}
