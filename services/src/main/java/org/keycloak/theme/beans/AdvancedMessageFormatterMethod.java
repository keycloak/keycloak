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

package org.keycloak.theme.beans;

import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

/**
 * @author <a href="mailto:gerbermichi@me.com">Michael Gerber</a>
 */
public class AdvancedMessageFormatterMethod implements TemplateMethodModelEx {
    private final Properties messages;
    private final Locale locale;

    public AdvancedMessageFormatterMethod(Locale locale, Properties messages) {
        this.locale = locale;
        this.messages = messages;
    }

    @Override
    public Object exec(List list) throws TemplateModelException {
        if (list.size() >= 1) {
            String key = list.get(0).toString();
            if (key.startsWith("${") && key.endsWith("}")) {
                key = key.substring(2, key.length() - 1);
                return new MessageFormat(messages.getProperty(key, key), locale).format(list.subList(1, list.size()).toArray());
            } else {
                return key;
            }
        }
        return null;
    }
}
