package org.keycloak.freemarker.beans;

import java.text.MessageFormat;
import java.util.Locale;

/**
 * @author <a href="mailto:gerbermichi@me.com">Michael Gerber</a>
 */
public class TextFormatterBean {
    private Locale locale;

    public TextFormatterBean(Locale locale) {
        this.locale = locale;
    }

    public String format(String pattern, Object ... parameters){
        return new MessageFormat(pattern.replace("'","''"),locale).format(parameters);
    }
}
