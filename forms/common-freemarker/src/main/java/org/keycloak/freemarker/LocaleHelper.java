package org.keycloak.freemarker;

import org.jboss.logging.Logger;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;
import java.util.*;

/**
 * Created by michigerber on 23.02.15.
 */
public class LocaleHelper {
    private final static String LOCALE_COOKIE = "KEYCLOAK_LOCALE";
    private final static Logger LOGGER = Logger.getLogger(LocaleHelper.class);
    private static final String LOCALE_PARAM = "ui_locale";

    public static Locale getLocale(RealmModel realm, UserModel user, UriInfo uriInfo, HttpHeaders httpHeaders) {

        //1. Locale cookie
        if(httpHeaders != null && httpHeaders.getCookies().containsKey(LOCALE_COOKIE)){
            String localeString = httpHeaders.getCookies().get(LOCALE_COOKIE).getValue();
            Locale locale =  findLocale(localeString, realm.getSupportedLocales());
            if(locale != null){
                return locale;
            }else{
                LOGGER.infof("Locale %s is not supported.", localeString);
            }
        }

        //2. User profile
        if(user != null && user.getAttributes().containsKey(UserModel.LOCALE)){
            String localeString = user.getAttribute(UserModel.LOCALE);
            Locale locale =  findLocale(localeString, realm.getSupportedLocales());
            if(locale != null){
                return locale;
            }else{
                LOGGER.infof("Locale %s is not supported.", localeString);
            }
        }

        //3. ui_locales query parameter
        if(uriInfo != null && uriInfo.getQueryParameters().containsKey(LOCALE_PARAM)){
            String localeString = uriInfo.getQueryParameters().getFirst(LOCALE_PARAM);
            Locale locale =  findLocale(localeString, realm.getSupportedLocales());
            if(locale != null){
                return locale;
            }else{
                LOGGER.infof("Locale %s is not supported.", localeString);
            }
        }

        //4. Accept-Language http header
        if(httpHeaders !=null && httpHeaders.getLanguage() != null){
            String localeString =httpHeaders.getLanguage().toLanguageTag();
            Locale locale =  findLocale(localeString, realm.getSupportedLocales());
            if(locale != null){
                return locale;
            }else{
                LOGGER.infof("Locale %s is not supported.", localeString);
            }
        }

        //5. Default realm locale
        return Locale.forLanguageTag(realm.getDefaultLocale());
    }

    private static Locale findLocale(String localeString, Set<String> supportedLocales) {
        List<Locale> locales = new ArrayList<Locale>();
        for(String l : supportedLocales) {
            locales.add(Locale.forLanguageTag(l));
        }
        return Locale.lookup(Locale.LanguageRange.parse(localeString),locales);
    }
}
