package org.keycloak.freemarker.beans;

import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

import java.text.MessageFormat;
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
        String key = list.get(0).toString();
        if (list.size() >= 1) {
            return new MessageFormat(messages.getProperty(key,key),locale).format(list.subList(1, list.size()).toArray());
        } else {
            return null;
        }
    }
}
