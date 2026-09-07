package org.keycloak.tests.i18n;

import org.keycloak.models.RealmModel;
import org.keycloak.services.util.LocaleUtil;
import org.keycloak.testframework.annotations.InjectHttpClient;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.theme.Theme;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Locale;

/**
 * A locale received from a client ends up as a key of the theme message cache, which is held for the lifetime of the
 * server. Verify that only locales supported by the theme or the realm are used for message lookups, so that the
 * number of locales the cache can ever hold stays bounded.
 * <p>
 * On this release the parent locale chain of a message lookup ends at English rather than at the realm default locale,
 * so an unresolved locale resolves to English. That keeps the messages a client receives unchanged by the clamping.
 */
@KeycloakIntegrationTest
public class ThemeMessageLocaleTest {

    @InjectRealm(config = LocaleRealmConfig.class)
    ManagedRealm managedRealm;

    @InjectRealm(ref = "german-default", config = GermanDefaultRealmConfig.class)
    ManagedRealm germanDefaultRealm;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @InjectHttpClient
    CloseableHttpClient httpClient;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @Test
    public void unsupportedLocaleIsResolvedToSupportedLocale() {
        String realmId = managedRealm.getId();

        runOnServer.run(session -> {
            RealmModel realm = session.realms().getRealm(realmId);
            session.getContext().setRealm(realm);
            Theme theme = session.theme().getTheme("base", Theme.Type.LOGIN);

            // Well-formed BCP 47 tags which neither the theme nor the realm supports. Each one used to be taken at
            // face value and retained as a key of the theme message cache, letting an unauthenticated client grow
            // that cache until the heap was exhausted.
            for (int i = 0; i < 100; i++) {
                String unsupported = "zz-x-" + i;
                Locale resolved = LocaleUtil.resolveSupportedLocale(realm, theme, unsupported);

                Assertions.assertNotEquals(Locale.forLanguageTag(unsupported), resolved,
                        "An unsupported locale must never be used to look up messages");
                Assertions.assertEquals(Locale.ENGLISH, resolved);
            }

            // A locale the realm supports is still honored, and a more specific variant of it resolves to the
            // supported locale rather than falling back.
            Assertions.assertEquals(Locale.GERMAN, LocaleUtil.resolveSupportedLocale(realm, theme, "de"));
            Assertions.assertEquals(Locale.GERMAN, LocaleUtil.resolveSupportedLocale(realm, theme, "de-CH"));

            // A locale only the theme declares stays available, so removing a locale from the realm does not make the
            // translations of the theme unreachable.
            Assertions.assertEquals(Locale.FRENCH, LocaleUtil.resolveSupportedLocale(realm, theme, "fr"));

            Assertions.assertEquals(Locale.ENGLISH, LocaleUtil.resolveSupportedLocale(realm, theme, null));
        });
    }

    @Test
    public void localizationTextsFallBackForUnsupportedLocale() throws IOException {
        String localizationUrl = keycloakUrls.getBase() + "/resources/" + managedRealm.getName() + "/login/";

        // An unsupported locale must still return usable content rather than a 404, and that content is the one of
        // the locale it resolves to, so nothing specific to the requested tag is ever produced or retained.
        Assertions.assertEquals(getBody(localizationUrl + "en"), getBody(localizationUrl + "zz-x-00000001"));
    }

    @Test
    public void unsupportedLocaleIsResolvedToEnglishRatherThanRealmDefault() {
        String realmId = germanDefaultRealm.getId();

        runOnServer.run(session -> {
            RealmModel realm = session.realms().getRealm(realmId);
            session.getContext().setRealm(realm);
            Theme theme = session.theme().getTheme("base", Theme.Type.LOGIN);

            // The default locale of this realm is not English, which is what makes the fallback observable. On this
            // release it has to stay English, matching where the parent locale chain of a message lookup ends, so
            // that clamping the requested locale does not change which messages a client receives.
            Assertions.assertEquals(Locale.ENGLISH, LocaleUtil.resolveSupportedLocale(realm, theme, "zz-x-00000001"));
            Assertions.assertEquals(Locale.ENGLISH, LocaleUtil.resolveSupportedLocale(realm, theme, null));

            // A locale the realm supports is still honored.
            Assertions.assertEquals(Locale.GERMAN, LocaleUtil.resolveSupportedLocale(realm, theme, "de"));
        });
    }

    @Test
    public void localizationTextsFallBackToEnglishForRealmWithOtherDefaultLocale() throws IOException {
        String localizationUrl = keycloakUrls.getBase() + "/resources/" + germanDefaultRealm.getName() + "/login/";

        // The same fallback seen through the endpoint the report was filed against: an unsupported locale serves the
        // English texts, and not those of the realm default locale.
        Assertions.assertEquals(getBody(localizationUrl + "en"), getBody(localizationUrl + "zz-x-00000001"));
        Assertions.assertNotEquals(getBody(localizationUrl + "de"), getBody(localizationUrl + "zz-x-00000001"));
    }

    private String getBody(String url) throws IOException {
        try (CloseableHttpResponse response = httpClient.execute(new HttpGet(url))) {
            Assertions.assertEquals(200, response.getStatusLine().getStatusCode());
            return EntityUtils.toString(response.getEntity());
        }
    }

    private static class LocaleRealmConfig implements RealmConfig {

        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            return realm.internationalizationEnabled(true)
                    .supportedLocales("en", "de")
                    .defaultLocale("en");
        }
    }

    private static class GermanDefaultRealmConfig implements RealmConfig {

        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            return realm.internationalizationEnabled(true)
                    .supportedLocales("en", "de")
                    .defaultLocale("de");
        }
    }
}
