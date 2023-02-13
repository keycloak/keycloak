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

import freemarker.template.SimpleScalar;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;
import org.keycloak.theme.TemplatingUtil;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

/**
 * @author <a href="mailto:gerbermichi@me.com">Michael Gerber</a>
 */
public class MessageFormatterMethod implements TemplateMethodModelEx {
    private final Properties messages;
    private final Locale locale;

    public MessageFormatterMethod(Locale locale, Properties messages) {
        this.locale = locale;
        this.messages = messages;
    }

    @Override
    public Object exec(List list) throws TemplateModelException {
        if (list.size() >= 1) {
            // resolve any remaining ${} expressions
            List<Object> resolved = resolve(list.subList(1, list.size()));
            String key = list.get(0).toString();
            return new MessageFormat(messages.getProperty(key,key),locale).format(resolved.toArray());
        } else {
            return null;
        }
    }

    private List<Object> resolve(List<Object> list) {
        ArrayList<Object> result = new ArrayList<>();
        for (Object item: list) {
            if (item instanceof SimpleScalar) {
                item = ((SimpleScalar) item).getAsString();
            }
            if (item instanceof String) {
                result.add(TemplatingUtil.resolveVariables((String) item, messages));
            } else {
                result.add(item);
            }
        }
        return result;
    }
}
