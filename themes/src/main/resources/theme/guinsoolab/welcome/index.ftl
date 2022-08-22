<!--
  ~ JBoss, Home of Professional Open Source.
  ~ Copyright (c) 2011, Red Hat, Inc., and individual contributors
  ~ as indicated by the @author tags. See the copyright.txt file in the
  ~ distribution for a full listing of individual contributors.
  ~
  ~ This is free software; you can redistribute it and/or modify it
  ~ under the terms of the GNU Lesser General Public License as
  ~ published by the Free Software Foundation; either version 2.1 of
  ~ the License, or (at your option) any later version.
  ~
  ~ This software is distributed in the hope that it will be useful,
  ~ but WITHOUT ANY WARRANTY; without even the implied warranty of
  ~ MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  ~ Lesser General Public License for more details.
  ~
  ~ You should have received a copy of the GNU Lesser General Public
  ~ License along with this software; if not, write to the Free
  ~ Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
  ~ 02110-1301 USA, or see the FSF site: http://www.fsf.org.
  -->
<!DOCTYPE html>

<html>
<head>
    <title>Welcome to ${productNameFull}</title>

    <meta charset="utf-8">
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <meta name="robots" content="noindex, nofollow">

    <link rel="shortcut icon" href="${resourcesPath}/img/favicon.ico" />

    <#if properties.stylesCommon?has_content>
        <#list properties.stylesCommon?split(' ') as style>
            <link href="${resourcesCommonPath}/${style}" rel="stylesheet" />
        </#list>
    </#if>
    <#if properties.styles?has_content>
        <#list properties.styles?split(' ') as style>
            <link href="${resourcesPath}/${style}" rel="stylesheet" />
        </#list>
    </#if>
</head>

<body>
<div class="container-fluid">
  <div class="row">
    <div class="col-sm-10 col-sm-offset-1 col-md-8 col-md-offset-2 col-lg-8 col-lg-offset-2">
      <div class="welcome-header">
        <img src="${resourcesPath}/logo.png" alt="${productName}" border="0" />
        <h1>Welcome to <strong>${productNameFull}</strong></h1>
      </div>
      <div class="row">
        <div class="col-xs-12 col-sm-4">
          <div class="card-pf h-l">
            <#if successMessage?has_content>
                <p class="alert success">${successMessage}</p>
            <#elseif errorMessage?has_content>
                <p class="alert error">${errorMessage}</p>
                <h3><img src="welcome-content/user.png">Administration Console</h3>
            <#elseif bootstrap>
            <#if localUser>
                <h3><img src="welcome-content/user.png">Administration Console</h3>
                <p>Please create an initial admin user to get started.</p>
            <#else>
                <p class="welcome-message">
                    <img src="welcome-content/alert.png">You need local access to create the initial admin user. <br><br>Open <a href="${localAdminUrl}">${localAdminUrl}</a>
                    <br>${adminUserCreationMessage}.
                </p>
            </#if>
            </#if>

            <#if bootstrap && localUser>
                <form method="post" class="welcome-form">
                    <p>
                        <label for="username">Username</label>
                        <input id="username" name="username" />
                    </p>

                    <p>
                        <label for="password">Password</label>
                        <input id="password" name="password" type="password" />
                    </p>

                    <p>
                        <label for="passwordConfirmation">Password confirmation</label>
                        <input id="passwordConfirmation" name="passwordConfirmation" type="password" />
                    </p>

                    <input id="stateChecker" name="stateChecker" type="hidden" value="${stateChecker}" />

                    <button id="create-button" type="submit" class="btn btn-primary">Create</button>
                </form>
            </#if>
            <div class="welcome-primary-link">
              <h3><a href="${adminUrl}"><img src="welcome-content/user.png">Administration Console <i class="fa fa-angle-right link" aria-hidden="true"></i></a></h3>
              <div class="description">
                Centrally manage all aspects of the ${productNameFull} server
              </div>
            </div>
          </div>
        </div>
        <div class="col-xs-12 col-sm-4">
          <div class="card-pf h-l">
            <h3><a href="${properties.documentationUrl}"><img class="doc-img" src="welcome-content/admin-console.png">Documentation <i class="fa fa-angle-right link" aria-hidden="true"></i></a></h3>
            <div class="description">

              User Guide, Admin REST API and Javadocs

            </div>
          </div>
        </div>
        <div class="col-xs-12 col-sm-4">
        <#if properties.displayCommunityLinks = "true">
          <div class="card-pf h-m">
            <h3><a href="http://www.keycloak.org"><img src="welcome-content/keycloak-project.png">Keycloak Project <i class="fa fa-angle-right link" aria-hidden="true"></i></a></h3>
          </div>
          <div class="card-pf h-m">
            <h3><a href="https://groups.google.com/forum/#!forum/keycloak-user"><img src="welcome-content/mail.png">Mailing List <i class="fa fa-angle-right link" aria-hidden="true"></i></a></h3>
          </div>
          <div class="card-pf h-m">
            <h3><a href="https://github.com/keycloak/keycloak/issues"><img src="welcome-content/bug.png">Report an issue <i class="fa fa-angle-right link" aria-hidden="true"></i></a></h3>
          </div>
        </#if>
        </div>
      </div>
    </div>
  </div>
</div>
</body>
</html>
