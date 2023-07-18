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

package org.keycloak.testsuite.adapter.page.fuse;

import org.keycloak.testsuite.adapter.page.AppServerContextRoot;

import java.net.MalformedURLException;
import java.net.URL;
import org.keycloak.testsuite.util.DroneUtils;

/**
 *
 * @author tkyjovsk
 */
public abstract class AbstractFuseExample extends AppServerContextRoot {

    public abstract String getContext();

    private URL url;

    @Override
    public URL getInjectedUrl() {
        if (url == null) {
            try {
                url = new URL(super.getInjectedUrl().toExternalForm() + "/" + getContext());
            } catch (MalformedURLException ex) {
                throw new IllegalStateException(ex);
            }
        }
        return url;
    }
    
    /*
     *  non-javadoc
     *
     *  When run tests with phantomjs customer or prutuct portal page isn't properly
     *  loaded. This method reloads page in such case.
     */
    @Override
    public void navigateTo() {
        super.navigateTo();
        
        if (DroneUtils.getCurrentDriver().getPageSource().contains("<html><head></head><body></body></html>")) {
            log.debug("Page wasn't properly loaded - redirecting.");
            super.navigateTo();
        }
    }
}
