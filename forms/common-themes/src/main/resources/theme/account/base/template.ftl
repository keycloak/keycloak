<#macro mainLayout active bodyClass>
<!doctype html>
<html>
<head>
    <meta charset="utf-8">
    <title>Account Management</title>
    <link rel="icon" href="${url.resourcesPath}/img/favicon.ico">
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

    <#if message?has_content>
    <div class="feedback-aligner">
        <#if message.success>
        <div class="feedback success show"><p><strong>${rb.successHeader}</strong> ${message.summary}</p></div>
        </#if>
        <#if message.error>
        <div class="feedback error show"><p><strong>${rb.errorHeader}</strong> ${message.summary}</p></div>
        </#if>
    </div>
    </#if>

<div class="header rcue">
    <div class="navbar utility">
        <div class="navbar-inner clearfix container">
            <h1><a href="#"><strong>Keycloak</strong> Central Login</a></h1>
            <ul class="nav pull-right">
                <li>
                    <a href="${url.logoutUrl}">Sign Out</a>
                </li>
            </ul>
        </div>
    </div>
</div><!-- End .header -->

<div class="container">
    <div class="row">
        <div class="bs-sidebar col-md-3 clearfix">
            <ul>
                <li class="<#if active=='account'>active</#if>"><a href="${url.accountUrl}">Account</a></li>
                <li class="<#if active=='password'>active</#if>"><a href="${url.passwordUrl}">Password</a></li>
                <li class="<#if active=='totp'>active</#if>"><a href="${url.totpUrl}">Authenticator</a></li>
                <#--<li class="<#if active=='social'>active</#if>"><a href="${url.socialUrl}">Social Accounts</a></li>-->
                <#--<li class="<#if active=='access'>active</#if>"><a href="${url.accessUrl}">Authorized Access</a></li>-->
            </ul>
        </div>

        <div id="content-area" class="col-md-9" role="main">
            <div id="content">
                <#nested "content">
            </div>
        </div>
        <div id="container-right-bg"></div>
    </div>
</div>
</body>
</html>
</#macro>