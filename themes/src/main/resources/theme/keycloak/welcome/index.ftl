<!--
  ~ Copyright 2016 Red Hat, Inc. and/or its affiliates
  ~ and other contributors as indicated by the @author tags.
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~ http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  -->
<!DOCTYPE html>

<html>
<head>
    <title>Welcome to ${productName}</title>

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
        <h1>Welcome to <strong>${productName}</strong></h1>
      </div>
      <div class="row">
        <#if adminConsoleEnabled>
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
                Centrally manage all aspects of the ${productName} server
              </div>
            </div>
          </div>
        </div>
        </#if> <#-- adminConsoleEnabled -->
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
