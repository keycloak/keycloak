package org.keycloak.locale;

import org.junit.Test;
import org.keycloak.forms.login.freemarker.model.LoginRealmBeanTest;
import org.keycloak.models.RealmModel;
import org.keycloak.theme.beans.LocaleBean;

import java.lang.reflect.Proxy;
import java.util.Locale;
import java.util.Properties;
import java.util.stream.Stream;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LocaleBeanIsRtlTest {

    @Test
    public void isRtl() {
        RealmModel realmModel = (RealmModel) Proxy.newProxyInstance(LoginRealmBeanTest.class.getClassLoader(), new Class[]{RealmModel.class}, (proxy, method, args) -> {

            if (method.getName().matches("getSupportedLocalesStream")) {
                return Stream.empty();
            }

            return null;
        });

        Properties messages = new Properties();

        Locale locale = Locale.forLanguageTag("ar");
        LocaleBean localeBean = new LocaleBean(realmModel, locale, null, messages);
        assertTrue(localeBean.isRtl());

        locale = Locale.forLanguageTag("he");
        localeBean = new LocaleBean(realmModel, locale, null, messages);
        assertTrue(localeBean.isRtl());

        locale = Locale.forLanguageTag("en");
        localeBean = new LocaleBean(realmModel, locale, null, messages);
        assertFalse(localeBean.isRtl());

        locale = Locale.forLanguageTag("fr");
        localeBean = new LocaleBean(realmModel, locale, null, messages);
        assertFalse(localeBean.isRtl());
    }
}
