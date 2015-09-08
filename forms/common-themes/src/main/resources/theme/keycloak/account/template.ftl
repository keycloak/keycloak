<#macro mainLayout active bodyClass>
<!doctype html>
<html>
<head>
    <meta charset="utf-8">
    <title>${msg("accountManagementTitle")}</title>
    <link rel="icon" href="${url.resourcesPath}/lib/rcue/img/favicon.ico">
    <!-- iPad retina icon -->
    <link rel="apple-touch-icon-precomposed" sizes="152x152" href="${url.resourcesPath}/lib/rcue/img/apple-touch-icon-precomposed-152.png">
    <!-- iPad retina icon (iOS < 7) -->
    <link rel="apple-touch-icon-precomposed" sizes="144x144" href="${url.resourcesPath}/lib/rcue/img/apple-touch-icon-precomposed-144.png">
    <!-- iPad non-retina icon -->
    <link rel="apple-touch-icon-precomposed" sizes="76x76" href="${url.resourcesPath}/lib/rcue/img/apple-touch-icon-precomposed-76.png">
    <!-- iPad non-retina icon (iOS < 7) -->
    <link rel="apple-touch-icon-precomposed" sizes="72x72" href="${url.resourcesPath}/lib/rcue/img/apple-touch-icon-precomposed-72.png">
    <!-- iPhone 6 Plus icon -->
    <link rel="apple-touch-icon-precomposed" sizes="120x120" href="${url.resourcesPath}/lib/rcue/img/apple-touch-icon-precomposed-180.png">
    <!-- iPhone retina icon (iOS < 7) -->
    <link rel="apple-touch-icon-precomposed" sizes="114x114" href="${url.resourcesPath}/lib/rcue/img/apple-touch-icon-precomposed-114.png">
    <!-- iPhone non-retina icon (iOS < 7) -->
    <link rel="apple-touch-icon-precomposed" sizes="57x57" href="${url.resourcesPath}/lib/rcue/img/apple-touch-icon-precomposed-57.png">
    <#if properties.styles?has_content>
        <#list properties.styles?split(' ') as style>
            <link href="${url.resourcesPath}/${style}" rel="stylesheet" />
        </#list>
    </#if>
    <#if properties.scripts?has_content>
        <#list properties.scripts?split(' ') as script>
            <script type="text/javascript" src="${url.resourcesPath}/${script}"></script>
        </#list>
    </#if>
</head>
<body class="admin-console user ${bodyClass}">
        <nav class="navbar navbar-default navbar-pf" role="navigation">
            <div class="navbar-header">
            <#if referrer?has_content && referrer.url?has_content>
                <a class="navbar-brand" href="${referrer.url}">
            <#else>
                <a class="navbar-brand" href="/auth/admin">
            </#if>
              <img src="${url.resourcesPath}/img/brand.svg" alt="Red Hat&reg; JBoss&reg; Identity and Access Management" />
            </a>
            </div>
            <div class="collapse navbar-collapse navbar-collapse-1">
            <ul class="nav navbar-nav navbar-utility">
                <#if realm.internationalizationEnabled>
                    <li class="dropdown" id="kc-locale-dropdown">
                        <a href="#" class="dropdown-toggle" data-toggle="dropdown" id="kc-current-locale-link">${locale.current}</a>
                        <ul class="dropdown-menu">
                            <#list locale.supported as l>
                                <li><a href="${l.url}">${l.label}</a></li>
                            </#list>
                        </ul>
                    </li>
              </#if>
              <li class="dropdown">
                <a href="#" class="dropdown-toggle" data-toggle="dropdown">
                  <span class="pficon pficon-user"></span>
                    <#if account.firstName?has_content || account.lastName?has_content>
                        <#if account.firstName?has_content && account.lastName?has_content>
                            ${(account.firstName + " " +  account.lastName)?capitalize?html}
                        <#elseif account.firstName?has_content> 
                            ${account.firstName?capitalize?html}
                        <#else>
                            ${account.lastName?capitalize?html}
                        </#if>
                    <#else>
                        ${(account.username!'')?capitalize?html}
                    </#if>
                  <b class="caret"></b>
                </a>
                <ul class="dropdown-menu">
                    <li><a href="${url.logoutUrl}">${msg("doSignOut")}</a></li>
                </ul>
              </li>
            </ul>
            <ul class="nav navbar-nav navbar-primary">
              <li>
                <#if referrer?has_content && referrer.url?has_content>
                <a href="${referrer.url}" id="referrer">Home</a>
                <#else>
                <a href="/auth/admin">Home</a>
                </#if>
              </li>
            </ul>
            </div>
        </nav>

    <div class="container">
        <div class="bs-sidebar col-sm-3  ng-scope">
            <ul>
                <li class="<#if active=='account'>active</#if>"><a href="${url.accountUrl}">${msg("account")}</a></li>
                <#if features.passwordUpdateSupported><li class="<#if active=='password'>active</#if>"><a href="${url.passwordUrl}">${msg("password")}</a></li></#if>
                <li class="<#if active=='totp'>active</#if>"><a href="${url.totpUrl}">${msg("authenticator")}</a></li>
                <#if features.identityFederation><li class="<#if active=='social'>active</#if>"><a href="${url.socialUrl}">${msg("federatedIdentity")}</a></li></#if>
                <li class="<#if active=='sessions'>active</#if>"><a href="${url.sessionsUrl}">${msg("sessions")}</a></li>
                <li class="<#if active=='applications'>active</#if>"><a href="${url.applicationsUrl}">${msg("applications")}</a></li>
                <#if features.log><li class="<#if active=='log'>active</#if>"><a href="${url.logUrl}">${msg("log")}</a></li></#if>
            </ul>
        </div>

        <div class="col-md-9 content-area">
            <#if message?has_content>
                <div class="alert alert-${message.type}">
                    <#if message.type=='success' ><span class="pficon pficon-ok"></span></#if>
                    <#if message.type=='error' ><span class="pficon pficon-error-octagon"></span><span class="pficon pficon-error-exclamation"></span></#if>
                    ${message.summary}
                </div>
            </#if>

            <#nested "content">
        </div>
    </div>

</body>
</html>
</#macro>
