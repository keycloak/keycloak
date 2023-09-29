package org.keycloak.services.resources;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.util.LocaleUtil;
import org.keycloak.theme.Theme;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static java.util.stream.Collectors.*;

@Path("/localisation")
@Produces(MediaType.APPLICATION_JSON)
public class LocaleResource {

    @Context
    protected KeycloakSession session;

    @GET
    @Path("{realm}/{theme}/{locale}")

    public List<KeySource> getLocalizationTexts(@PathParam("realm") String realmName, @PathParam("theme") String theme,
                                                @PathParam("locale") String localeString, @QueryParam("source") boolean showSource) throws IOException {
        RealmManager realmManager = new RealmManager(session);
        RealmModel realm = realmManager.getRealmByName(realmName);
        session.getContext().setRealm(realm);

        Theme theTheme = session.theme().getTheme(Theme.Type.valueOf(theme.toUpperCase()));
        final Locale locale = Locale.forLanguageTag(localeString);
        if (showSource) {
            Properties messagesByLocale = theTheme.getMessages("messages", locale);
            Set<KeySource> result = messagesByLocale.entrySet().stream().map(e ->
                    new KeySource((String) e.getKey(), (String) e.getValue(), Source.THEME)).collect(toSet());

            Map<Locale, Properties> realmLocalizationMessages = LocaleUtil.getRealmLocalizationTexts(realm, locale);
            for (Locale currentLocale = locale; currentLocale != null; currentLocale = LocaleUtil.getParentLocale(currentLocale)) {
                final List<KeySource> realmOverride = realmLocalizationMessages.get(currentLocale).entrySet().stream().map(e ->
                        new KeySource((String) e.getKey(), (String) e.getValue(), Source.REALM)).collect(toList());
                result.addAll(realmOverride);
            }

            return new ArrayList<>(result);
        }
        return theTheme.getEnhancedMessages(realm, locale).entrySet().stream().map(e ->
                new KeySource((String) e.getKey(), (String) e.getValue())).collect(toList());
    }
}

enum Source {
    THEME,
    REALM
}
class KeySource {
    private String key;
    private String value;
    private Source source;

    public KeySource(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public KeySource(String key, String value, Source source) {
        this(key, value);
        this.source = source;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public Source getSource() {
        return source;
    }
}
