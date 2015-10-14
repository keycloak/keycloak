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
package org.keycloak.email.freemarker.beans;

import org.jboss.logging.Logger;
import org.keycloak.models.UserModel;

import javax.ws.rs.core.MultivaluedMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class ProfileBean {

    private static final Logger logger = Logger.getLogger(ProfileBean.class);

    private UserModel user;
    private final Map<String, String> attributes = new HashMap<>();

    public ProfileBean(UserModel user) {
        this.user = user;

        if (user.getAttributes() != null) {
            for (Map.Entry<String, List<String>> attr : user.getAttributes().entrySet()) {
                List<String> attrValue = attr.getValue();
                if (attrValue != null && attrValue.size() > 0) {
                    attributes.put(attr.getKey(), attrValue.get(0));
                }

                if (attrValue != null && attrValue.size() > 1) {
                    logger.warnf("There are more values for attribute '%s' of user '%s' . Will display just first value", attr.getKey(), user.getUsername());
                }
            }
        }
    }

    public String getUsername() { return user.getUsername(); }

    public String getFirstName() {
        return user.getFirstName();
    }

    public String getLastName() {
        return user.getLastName();
    }

    public String getEmail() {
        return user.getEmail();
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }
}
