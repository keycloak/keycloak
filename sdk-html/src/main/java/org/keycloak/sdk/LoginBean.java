package org.keycloak.sdk;

import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import javax.imageio.spi.ServiceRegistry;
import javax.servlet.http.HttpServletRequest;

import org.keycloak.services.models.RealmModel;
import org.keycloak.services.models.RequiredCredentialModel;

@ManagedBean(name = "login")
@RequestScoped
public class LoginBean {

    private RealmModel realm;

    private String name;

    private String loginAction;

    private String socialLoginUrl;

    private String username;

    private List<RequiredCredential> requiredCredentials;

    private List<Property> hiddenProperties;

    private List<SocialProvider> providers;

    private String theme;

    private String themeUrl;

    private Map<String, Object> themeConfig;

    private String error;

    @PostConstruct
    public void init() {
        FacesContext ctx = FacesContext.getCurrentInstance();

        HttpServletRequest request = (HttpServletRequest) ctx.getExternalContext().getRequest();

        realm = (RealmModel) request.getAttribute(RealmModel.class.getName());
        
        if (RealmModel.DEFAULT_REALM.equals(realm.getName())) {
            name = "Keycloak";
        } else {
            name = realm.getName();
        }

        loginAction = ((URI) request.getAttribute("KEYCLOAK_LOGIN_ACTION")).toString();
        socialLoginUrl = ((URI) request.getAttribute("KEYCLOAK_SOCIAL_LOGIN")).toString();

        username = (String) request.getAttribute("username");

        addRequiredCredentials();
        addHiddenProperties(request, "client_id", "scope", "state", "redirect_uri");
        addSocialProviders();
        addErrors(request);

        // TODO Get theme name from realm
        theme = "default";
        themeUrl = FacesContext.getCurrentInstance().getExternalContext().getRequestContextPath() + "/sdk/theme/" + theme;

        themeConfig = new HashMap<String, Object>();

        themeConfig.put("styles", themeUrl + "/styles.css");

        if (RealmModel.DEFAULT_REALM.equals(realm.getName())) {
            themeConfig.put("logo", themeUrl + "/img/red-hat-logo.png");
            themeConfig.put("background", themeUrl + "/img/login-screen-background.jpg");
        } else {
            themeConfig.put("background", themeUrl + "/img/customer-login-screen-bg2.jpg");
            themeConfig.put("displayPoweredBy", true);
        }
    }

    public Map<String, Object> getThemeConfig() {
        return themeConfig;
    }

    public String getName() {
        return name;
    }

    public String getLoginAction() {
        return loginAction;
    }

    public String getError() {
        return error;
    }

    public List<Property> getHiddenProperties() {
        return hiddenProperties;
    }

    public List<RequiredCredential> getRequiredCredentials() {
        return requiredCredentials;
    }

    public String getTheme() {
        return theme;
    }

    public String getThemeUrl() {
        return themeUrl;
    }

    public String getUsername() {
        return username;
    }

    public boolean isSocial() {
        // TODO Check if social is enabled in realm
        return true && providers.size() > 0;
    }

    public boolean isRegistrationAllowed() {
        return realm.isRegistrationAllowed();
    }

    private void addHiddenProperties(HttpServletRequest request, String... names) {
        hiddenProperties = new LinkedList<Property>();
        for (String name : names) {
            Object v = request.getAttribute(name);
            if (v != null) {
                hiddenProperties.add(new Property(name, (String) v));
            }
        }
    }

    private void addRequiredCredentials() {
        requiredCredentials = new LinkedList<RequiredCredential>();
        for (RequiredCredentialModel m : realm.getRequiredCredentials()) {
            if (m.isInput()) {
                requiredCredentials.add(new RequiredCredential(m.getType(), m.isSecret()));
            }
        }
    }

    private void addSocialProviders() {
        // TODO Add providers configured for realm instead of all providers
        providers = new LinkedList<SocialProvider>();
        for (Iterator<org.keycloak.social.SocialProvider> itr = ServiceRegistry
                .lookupProviders(org.keycloak.social.SocialProvider.class); itr.hasNext();) {
            org.keycloak.social.SocialProvider p = itr.next();
            providers.add(new SocialProvider(p.getId(), p.getName()));
        }
    }

    private void addErrors(HttpServletRequest request) {
        error = (String) request.getAttribute("KEYCLOAK_LOGIN_ERROR_MESSAGE");
    }

    public class Property {
        private String name;
        private String value;

        public Property(String name, String value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public String getValue() {
            return value;
        }
    }

    public class RequiredCredential {
        private String type;
        private boolean secret;

        public RequiredCredential(String type, boolean secure) {
            this.type = type;
            this.secret = secure;
        }

        public String getName() {
            return type;
        }

        public String getLabel() {
            return type;
        }

        public String getInputType() {
            return secret ? "password" : "text";
        }
    }

    public List<SocialProvider> getProviders() {
        return providers;
    }

    public class SocialProvider {
        private String id;
        private String name;

        public SocialProvider(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getLoginUrl() {
            StringBuilder sb = new StringBuilder();
            sb.append(socialLoginUrl);
            sb.append("?provider_id=" + id);
            for (Property p : hiddenProperties) {
                sb.append("&" + p.getName() + "=" + p.getValue());
            }
            return sb.toString();
        }
    }

}
