package org.keycloak.locale;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.models.UserModel;
import org.keycloak.storage.ReadOnlyException;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;

public class DefaultLocaleUpdaterProviderTest {

    @Test
    public void testLocaleCookieUpdatedEvenIfUserIsReadOnly() {
        final AtomicBoolean cookieUpdated = new AtomicBoolean(false);

        DefaultLocaleUpdaterProvider provider = new DefaultLocaleUpdaterProvider(null) {

            @Override
            public void updateUsersLocale(UserModel user, String locale) {
                if (locale != null) {
                    try {
                        user.setSingleAttribute(UserModel.LOCALE, locale);
                    } catch (ReadOnlyException e) {
                        // expected
                    }
                    updateLocaleCookie(locale);
                }
            }

            @Override
            public void updateLocaleCookie(String locale) {
                cookieUpdated.set(true);
            }
        };

        UserModel readOnlyUser = (UserModel) Proxy.newProxyInstance(
                UserModel.class.getClassLoader(),
                new Class[]{UserModel.class},
                (proxy, method, args) -> {
                    if ("setSingleAttribute".equals(method.getName())) {
                        throw new ReadOnlyException("User is read-only");
                    }
                    return null;
                }
        );

        provider.updateUsersLocale(readOnlyUser, "fr");

        Assert.assertTrue(cookieUpdated.get());
    }
}