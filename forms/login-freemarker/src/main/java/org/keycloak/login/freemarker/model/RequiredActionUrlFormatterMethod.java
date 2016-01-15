package org.keycloak.login.freemarker.model;

import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.services.Urls;

import java.net.URI;
import java.util.List;

/**
 */
public class RequiredActionUrlFormatterMethod implements TemplateMethodModelEx {
    private final String realm;
    private final URI baseUri;

    public RequiredActionUrlFormatterMethod(RealmModel realm, URI baseUri) {
        this.realm = realm.getName();
        this.baseUri = baseUri;
    }

    @Override
    public Object exec(List list) throws TemplateModelException {
        String action = list.get(0).toString();
        String relativePath = list.get(1).toString();
        String url = Urls.requiredActionBase(baseUri).path(relativePath).build(realm, action).toString();
        return url;
    }
}
