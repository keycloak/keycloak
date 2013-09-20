/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.keycloak.forms;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class TemplateBean {

    private RealmBean realm;

    private String theme = "default";

    private String themeUrl;

    private Map<String, Object> themeConfig;

    private String formsPath;


    public TemplateBean(RealmBean realm, String contextPath) {
        formsPath = contextPath + "/forms";

        // TODO Get theme name from realm
        theme = "default";
        themeUrl = formsPath + "/theme/" + theme;

        themeConfig = new HashMap<String, Object>();

        themeConfig.put("styles", themeUrl + "/styles.css");

        // TODO move this into CSS
        if (realm.isSaas()) {
            themeConfig.put("logo", themeUrl + "/img/red-hat-logo.png");
            themeConfig.put("background", themeUrl + "/img/login-screen-background.jpg");
        } else {
            themeConfig.put("background", themeUrl + "/img/customer-login-screen-bg2.jpg");
            themeConfig.put("displayPoweredBy", true);
        }
    }

    public String getFormsPath() {
        return formsPath;
    }

    public Map<String, Object> getThemeConfig() {
        return themeConfig;
    }

    public String getTheme() {
        return theme;
    }

    public String getThemeUrl() {
        return themeUrl;
    }

    public RealmBean getRealm() {
        return realm;
    }

    public void setRealm(RealmBean realm) {
        this.realm = realm;
    }

}
