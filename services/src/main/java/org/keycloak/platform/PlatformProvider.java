/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.platform;

import java.io.File;

import org.keycloak.Config;

public interface PlatformProvider {

    String name();
    
    void onStartup(Runnable runnable);

    void onShutdown(Runnable runnable);

    void exit(Throwable cause);

    /**
     * @return tmp directory specific to target platform. Implementation can make sure to create "tmp" directory in case it does not exists.
     * The directory should be usually inside the corresponding server directory. In production, it should not be system directory like "/tmp" .
     */
    File getTmpDirectory();


    /**
     * Returns classloader to load script engine. Classloader should contain the implementation of {@link javax.script.ScriptEngineFactory}
     * and it's definition inside META-INF/services of the jar file(s), which will be provided by this classloader.
     *
     * This method can return null and in that case, the default Keycloak services classloader will be used for load script engine. Note that java versions earlier than 15 always contain
     * the "nashorn" script engine by default on the classpath (it is part of the Java platform itself) and hence for them it is always fine to return null (unless you want to override default engine)
     *
     * @param scriptProviderConfig Configuration scope of the "default" provider of "scripting" SPI. It can contain some config properties for the classloader (EG. file path)
     * @return classloader or null
     */
    ClassLoader getScriptEngineClassLoader(Config.Scope scriptProviderConfig);

}
