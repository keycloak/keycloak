package org.keycloak.sdk;

import java.net.URI;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.PostConstruct;
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

    private String style = "saas";

    private String clientId;

    private String scope;

    private String state;

    private String redirectUri;

    private String loginAction;

    private String socialLoginUrl;

    private String themeUrl;

    private List<SocialProvider> providers;
    
    private List<RequiredCredential> requiredCredentials; 

    private RealmModel realm;

    private String username;

    private String baseUrl;

    @PostConstruct
    public void init() {
        HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();

        realm = (RealmModel) request.getAttribute(RealmModel.class.getName());

        clientId = (String) request.getAttribute("client_id");
        scope = (String) request.getAttribute("scope");
        state = (String) request.getAttribute("state");
        redirectUri = (String) request.getAttribute("redirect_uri");

        loginAction = ((URI) request.getAttribute("KEYCLOAK_LOGIN_ACTION")).toString();

        socialLoginUrl = ((URI) request.getAttribute("KEYCLOAK_SOCIAL_LOGIN")).toString();

        username = (String) request.getAttribute("username");

        providers = new LinkedList<SocialProvider>();
        for (Iterator<org.keycloak.social.SocialProvider> itr = ServiceRegistry
                .lookupProviders(org.keycloak.social.SocialProvider.class); itr.hasNext();) {
            org.keycloak.social.SocialProvider p = itr.next();
            providers.add(new SocialProvider(p.getId(), p.getName()));
        }

        requiredCredentials = new LinkedList<RequiredCredential>();
        for (RequiredCredentialModel m : realm.getRequiredCredentials()) {
            if (m.isInput()) {
                requiredCredentials.add(new RequiredCredential(m.getType(), m.isSecret()));
            }
        }

        baseUrl = FacesContext.getCurrentInstance().getExternalContext().getRequestContextPath() + "/sdk";
        themeUrl = baseUrl + "/theme/" + style;

    }

    public List<RequiredCredential> getRequiredCredentials() {
        return requiredCredentials;
    }

    public String getStylesheet() {
        return themeUrl + "/styles.css";
    }

    public String getLoginTemplate() {
        return "theme/" + style + "/login.xhtml";
    }

    public String getLoginAction() {
        return loginAction;
    }

    public String getStyle() {
        return style;
    }

    public String getName() {
        return realm.getName();
    }

    public String getClientId() {
        return clientId;
    }

    public String getScope() {
        return scope;
    }

    public String getState() {
        return state;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public String getUsername() {
        return username;
    }

    public String getThemeUrl() {
        return themeUrl;
    }

    public String socialLoginUrl(String id) {
        StringBuilder sb = new StringBuilder();
        sb.append(socialLoginUrl);
        sb.append("?provider_id=" + id);
        sb.append("&client_id=" + clientId);
        if (scope != null) {
            sb.append("&scope=" + scope);
        }
        if (state != null) {
            sb.append("&state=" + state);
        }
        sb.append("&redirect_uri=" + redirectUri);
        return sb.toString();
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
            sb.append("&client_id=" + clientId);
            if (scope != null) {
                sb.append("&scope=" + scope);
            }
            if (state != null) {
                sb.append("&state=" + state);
            }
            sb.append("&redirect_uri=" + redirectUri);
            return sb.toString();
        }

        public String getIconUrl() {
            return themeUrl + "/icons/" + id + ".png";
        }
    }
    
    public class RequiredCredential {
        private String type;
        private boolean secret;

        public RequiredCredential(String type, boolean secure) {
            this.type = type;
            this.secret = secure;
        }

        public String getType() {
            return type;
        }

        public boolean isSecret() {
            return secret;
        }
    }

}
