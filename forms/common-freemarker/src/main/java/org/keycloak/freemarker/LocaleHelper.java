package org.keycloak.freemarker;

import org.jboss.logging.Logger;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.*;

/**
 * @author <a href="mailto:gerbermichi@me.com">Michael Gerber</a>
 */
public class LocaleHelper {
    public final static String LOCALE_COOKIE = "KEYCLOAK_LOCALE";
    public static final String UI_LOCALES_PARAM = "ui_locales";
    public static final String KC_LOCALE_PARAM = "kc_locale";

    private final static Logger LOGGER = Logger.getLogger(LocaleHelper.class);

    public static Locale getLocale(RealmModel realm, UserModel user) {
        return getLocale(realm, user, null, null);
    }

    public static Locale getLocale(RealmModel realm, UserModel user, UriInfo uriInfo, HttpHeaders httpHeaders) {
        if(!realm.isInternationalizationEnabled()){
            return Locale.ENGLISH;
        }

        //0. kc_locale query parameter
         if(uriInfo != null && uriInfo.getQueryParameters().containsKey(KC_LOCALE_PARAM)){
            String localeString = uriInfo.getQueryParameters().getFirst(KC_LOCALE_PARAM);
            Locale locale =  findLocale(realm.getSupportedLocales(), localeString);
            if(locale != null){
                if(user != null){
                    user.setAttribute(UserModel.LOCALE, locale.toLanguageTag());
                }
                return locale;
            }else{
                LOGGER.infof("Locale %s is not supported.", localeString);
            }
        }
         
        //1. Locale cookie
        if(httpHeaders != null && httpHeaders.getCookies().containsKey(LOCALE_COOKIE)){
            String localeString = httpHeaders.getCookies().get(LOCALE_COOKIE).getValue();
            Locale locale =  findLocale(realm.getSupportedLocales(), localeString);
            if(locale != null){
                if(user != null && user.getAttribute(UserModel.LOCALE) == null){
                    user.setAttribute(UserModel.LOCALE, locale.toLanguageTag());
                }
                return locale;
            }else{
                LOGGER.infof("Locale %s is not supported.", localeString);
            }
        }

        //2. User profile
        if(user != null && user.getAttributes().containsKey(UserModel.LOCALE)){
            String localeString = user.getAttribute(UserModel.LOCALE);
            Locale locale =  findLocale(realm.getSupportedLocales(), localeString);
            if(locale != null){

                return locale;
            }else{
                LOGGER.infof("Locale %s is not supported.", localeString);
            }
        }

        //3. ui_locales query parameter
        if(uriInfo != null && uriInfo.getQueryParameters().containsKey(UI_LOCALES_PARAM)){
            String localeString = uriInfo.getQueryParameters().getFirst(UI_LOCALES_PARAM);
            Locale locale =  findLocale(realm.getSupportedLocales(), localeString.split(" "));
            if(locale != null){
                return locale;
            }else{
                LOGGER.infof("Locale %s is not supported.", localeString);
            }
        }

        //4. Accept-Language http header
        if(httpHeaders !=null && httpHeaders.getAcceptableLanguages() != null && !httpHeaders.getAcceptableLanguages().isEmpty()){
            for(Locale l : httpHeaders.getAcceptableLanguages()){
                String localeString = l.toLanguageTag();
                Locale locale =  findLocale(realm.getSupportedLocales(), localeString);
                if(locale != null){
                    return locale;
                }else{
                    LOGGER.infof("Locale %s is not supported.", localeString);
                }
            }
        }

        //5. Default realm locale
        if(realm.getDefaultLocale() != null){
            return Locale.forLanguageTag(realm.getDefaultLocale());
        }

        return Locale.ENGLISH;
    }

    public static void updateLocaleCookie(Response.ResponseBuilder builder, Locale locale, RealmModel realm, UriInfo uriInfo, String path) {
        if (locale == null) {
            return;
        }
        boolean secure = realm.getSslRequired().isRequired(uriInfo.getRequestUri().getHost());
        builder.cookie(new NewCookie(LocaleHelper.LOCALE_COOKIE, locale.toLanguageTag(), path, null, null, 31536000, secure));
    }



    public static Locale findLocale(Set<String> supportedLocales, String ... localeStrings) {
        for(String localeString : localeStrings){
            Locale result = null;
            Locale search = Locale.forLanguageTag(localeString);
            for(String languageTag : supportedLocales) {
                Locale locale = Locale.forLanguageTag(languageTag);
                if(locale.getLanguage().equals(search.getLanguage())){
                    if(locale.getCountry().equals("") && result == null){
                        result = locale;
                    }
                    if(locale.getCountry().equals(search.getCountry())){
                        return locale;
                    }
                }
            }
            if(result != null){
                return result;
            }
        }
        return null;
    }
}
