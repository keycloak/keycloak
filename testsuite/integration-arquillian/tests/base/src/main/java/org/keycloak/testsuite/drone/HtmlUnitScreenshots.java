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

package org.keycloak.testsuite.drone;


import org.arquillian.extension.recorder.screenshooter.ScreenshooterConfiguration;
import org.arquillian.extension.recorder.screenshooter.event.ScreenshooterExtensionConfigured;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;

/**
 * Disable screenshots for HTMLUnitDriver as it doesn't work
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class HtmlUnitScreenshots {

    protected final org.jboss.logging.Logger log = org.jboss.logging.Logger.getLogger(HtmlUnitScreenshots.class);

    @Inject
    private Instance<ScreenshooterConfiguration> configuration;

    public void configureExtension(@Observes ScreenshooterExtensionConfigured event) {
        ScreenshooterConfiguration conf = configuration.get();

        if (System.getProperty("browser", "htmlUnit").equals("htmlUnit")) {
            conf.setProperty("takeWhenTestFailed", "false");
            log.info("Screenshots disabled as htmlUnit is used");
        } else {
            log.info("Screenshots are enabled");
        }

    }
}
