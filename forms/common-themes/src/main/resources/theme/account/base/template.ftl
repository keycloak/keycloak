<#macro mainLayout active bodyClass>
<!doctype html>
<html>
<head>
    <meta charset="utf-8">
    <title>Keycloak Account Management</title>
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

    <header class="navbar navbar-default navbar-pf navbar-main header">
        <div>
            <nav class="navbar" role="navigation">
            		<div class="container">
                		<div class="navbar-header">
		                    <div class="navbar-title">
		                    </div>
	                  </div>  

		                <div class="navbar-collapse">
		                    <ul class="nav navbar-nav navbar-utility">
		                        <#if referrer?has_content && referrer.baseUrl?has_content><li><a href="${referrer.baseUrl}">Back to ${referrer.name}</a></li></#if>
		                        <li><a href="${url.logoutUrl}">Sign Out</a></li>
		                    </ul>
		                </div>
		            </div>
            </nav>
        </div>
    </header>

    <div class="container">
        <div class="bs-sidebar col-sm-3  ng-scope">
            <ul>
                <li class="<#if active=='account'>active</#if>"><a href="${url.accountUrl}">Account</a></li>
                <li class="<#if active=='password'>active</#if>"><a href="${url.passwordUrl}">Password</a></li>
                <li class="<#if active=='totp'>active</#if>"><a href="${url.totpUrl}">Authenticator</a></li>
                <#if isSocialRealm?has_content><li class="<#if active=='social'>active</#if>"><a href="${url.socialUrl}">Social</a></li></#if>
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