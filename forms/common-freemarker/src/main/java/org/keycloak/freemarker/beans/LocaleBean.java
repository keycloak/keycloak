package org.keycloak.freemarker.beans;

import org.keycloak.freemarker.LocaleHelper;
import org.keycloak.models.RealmModel;

import javax.ws.rs.core.UriBuilder;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class LocaleBean {

    private String current;
    private List<Locale> supported;

    public LocaleBean(RealmModel realm, java.util.Locale current, UriBuilder uriBuilder, Properties messages) {
        this.current = messages.getProperty("locale_" + current.toLanguageTag(), current.toLanguageTag());

        supported = new LinkedList<>();
        for (String l : realm.getSupportedLocales()) {
            String label = messages.getProperty("locale_" + l, l);
            String url = uriBuilder.replaceQueryParam(LocaleHelper.KC_LOCALE_PARAM, l).build().toString();
            supported.add(new Locale(label, url));
        }
    }

    public String getCurrent() {
        return current;
    }

    public List<Locale> getSupported() {
        return supported;
    }

    public static class Locale {

        private String label;
        private String url;

        public Locale(String label, String url) {
            this.label = label;
            this.url = url;
        }

        public String getUrl() {
            return url;
        }

        public String getLabel() {
            return label;
        }

    }

}
