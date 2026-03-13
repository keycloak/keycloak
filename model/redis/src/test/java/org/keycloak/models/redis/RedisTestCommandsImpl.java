/*
 * Copyright 2026 Capital One Financial Corporation and/or its affiliates
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

package org.keycloak.models.redis;

import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.cluster.api.sync.RedisAdvancedClusterCommands;

import java.util.List;

/**
 * Implementation of RedisTestCommands that delegates to either standalone or cluster Redis commands.
 * 
 * Note: Unchecked casts are safe here because we control the command type via the isCluster flag.
 */
@SuppressWarnings("unchecked")
public class RedisTestCommandsImpl implements RedisTestCommands {
    
    private final Object commands;
    private final boolean isCluster;
    
    public RedisTestCommandsImpl(Object commands, boolean isCluster) {
        this.commands = commands;
        this.isCluster = isCluster;
    }
    
    @Override
    public String setex(String key, long seconds, String value) {
        if (isCluster) {
            return ((RedisAdvancedClusterCommands<String, String>) commands).setex(key, seconds, value);
        } else {
            return ((RedisCommands<String, String>) commands).setex(key, seconds, value);
        }
    }
    
    @Override
    public String get(String key) {
        if (isCluster) {
            return ((RedisAdvancedClusterCommands<String, String>) commands).get(key);
        } else {
            return ((RedisCommands<String, String>) commands).get(key);
        }
    }
    
    @Override
    public Long del(String... keys) {
        if (isCluster) {
            return ((RedisAdvancedClusterCommands<String, String>) commands).del(keys);
        } else {
            return ((RedisCommands<String, String>) commands).del(keys);
        }
    }
    
    @Override
    public Boolean expire(String key, long seconds) {
        if (isCluster) {
            return ((RedisAdvancedClusterCommands<String, String>) commands).expire(key, seconds);
        } else {
            return ((RedisCommands<String, String>) commands).expire(key, seconds);
        }
    }
    
    @Override
    public Long exists(String... keys) {
        if (isCluster) {
            return ((RedisAdvancedClusterCommands<String, String>) commands).exists(keys);
        } else {
            return ((RedisCommands<String, String>) commands).exists(keys);
        }
    }
    
    @Override
    public Long ttl(String key) {
        if (isCluster) {
            return ((RedisAdvancedClusterCommands<String, String>) commands).ttl(key);
        } else {
            return ((RedisCommands<String, String>) commands).ttl(key);
        }
    }
    
    @Override
    public List<String> keys(String pattern) {
        if (isCluster) {
            return ((RedisAdvancedClusterCommands<String, String>) commands).keys(pattern);
        } else {
            return ((RedisCommands<String, String>) commands).keys(pattern);
        }
    }
    
    @Override
    public String set(String key, String value) {
        if (isCluster) {
            return ((RedisAdvancedClusterCommands<String, String>) commands).set(key, value);
        } else {
            return ((RedisCommands<String, String>) commands).set(key, value);
        }
    }
    
    @Override
    public Boolean setnx(String key, String value) {
        if (isCluster) {
            return ((RedisAdvancedClusterCommands<String, String>) commands).setnx(key, value);
        } else {
            return ((RedisCommands<String, String>) commands).setnx(key, value);
        }
    }
    
    @Override
    public java.util.Map<String, String> hgetall(String key) {
        if (isCluster) {
            return ((RedisAdvancedClusterCommands<String, String>) commands).hgetall(key);
        } else {
            return ((RedisCommands<String, String>) commands).hgetall(key);
        }
    }
    
    @Override
    public String type(String key) {
        if (isCluster) {
            return ((RedisAdvancedClusterCommands<String, String>) commands).type(key);
        } else {
            return ((RedisCommands<String, String>) commands).type(key);
        }
    }
}
