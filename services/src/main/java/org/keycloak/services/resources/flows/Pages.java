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
package org.keycloak.services.resources.flows;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class Pages {

    public final static String ACCESS = "/forms/access.ftl";

    public final static String ACCOUNT = "/forms/account.ftl";

    public final static String LOGIN = "/forms/login.ftl";

    public final static String LOGIN_TOTP = "/forms/login-totp.ftl";

    public final static String LOGIN_CONFIG_TOTP = "/forms/login-config-totp.ftl";

    public final static String LOGIN_VERIFY_EMAIL = "/forms/login-verify-email.ftl";

    public final static String OAUTH_GRANT = "/forms/login-oauth-grant.ftl";

    public final static String PASSWORD = "/forms/password.ftl";

    public final static String LOGIN_RESET_PASSWORD = "/forms/login-reset-password.ftl";

    public final static String LOGIN_UPDATE_PASSWORD = "/forms/login-update-password.ftl";

    public final static String REGISTER = "/forms/register.ftl";

    public final static String ERROR = "/forms/error.ftl";

    public final static String SOCIAL = "/forms/social.ftl";

    public final static String TOTP = "/forms/totp.ftl";

    public final static String LOGIN_UPDATE_PROFILE = "/forms/login-update-profile.ftl";

}
