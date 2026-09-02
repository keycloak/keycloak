/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.theme.beans;

import java.util.Locale;
import java.util.Set;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;

/**
 * @author Alexander Schwartz
 */
public class LocaleBeanTest {

    private static final Set<String> RTL_LANGUAGE_CODES =
            Set.of("ar", "dv", "fa", "ha", "he", "iw", "ji", "ps", "sd", "ug", "ur", "yi");

    private static final Set<String> LTR_LANGUAGE_CODES =
            Set.of("en", "de");

    @Test
    public void verifyRtl() {
        for (String rtlLanguageCode : RTL_LANGUAGE_CODES) {
            MatcherAssert.assertThat(LocaleBean.isLeftToRight(Locale.forLanguageTag(rtlLanguageCode).getLanguage()), Matchers.is(true));
        }
    }

    @Test
    public void verifyLtr() {
        for (String rtlLanguageCode : LTR_LANGUAGE_CODES) {
            MatcherAssert.assertThat(LocaleBean.isLeftToRight(Locale.forLanguageTag(rtlLanguageCode).getLanguage()), Matchers.is(true));
        }
    }

}
