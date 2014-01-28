<#macro registrationLayout bodyClass isSeparator=false forceSeparator=false>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"  "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">

<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <title><#nested "title"></title>
    <link rel="icon" href="${url.resourcesPath}/img/favicon.ico">
    <#if properties.styles?has_content>
        <#list properties.styles?split(' ') as style>
            <link href="${url.resourcesPath}/${style}" rel="stylesheet" />
        </#list>
    </#if>
    <#if properties.scripts?has_content>
        <#list properties.scripts?split(' ') as script>
            <script src="${url.resourcesPath}/${script}" type="text/javascript"></script>
        </#list>
    </#if>
</head>

<body class="rcue-login-register ${bodyClass}">
    <div class="rcue-logo"></div>

    <div class="content">
        <h2>
            <#nested "header">
        </h2>

        <div class="background-area">
            <#if !forceSeparator && realm?has_content>
                <#assign drawSeparator = realm.registrationAllowed>
            <#else>
                <#assign drawSeparator = isSeparator>
            </#if>
            <div class="form-area ${(realm.social && bodyClass != "register")?string('social','')} ${(drawSeparator)?string('separator','')} clearfix">
                <div class="section app-form">
                    <#if message?has_content>
                        <div class="feedback ${message.type} bottom-left show">
                            <p><strong>${message.summary}</strong></p>
                        </div>
                    </#if>
                    <#nested "form">
                </div>

                <#if social.displaySocialProviders>
                    <div class="section social-login"> <span>or</span>
                        <p>${rb.logInWith}</p>
                        <ul>
                            <#list social.providers as p>
                                <li><a href="${p.loginUrl}" class="zocial ${p.id}"> <span class="text">${p.name}</span></a></li>
                            </#list>
                        </ul>
                    </div>
                </#if>

                <div class="section info-area">
                    <#nested "info">
                </div>
            </div>
        </div>

        <p class="powered">
            <a href="http://www.keycloak.org">${rb.poweredByKeycloak}</a>
        </p>
    </div>

    <#nested "content">

</body>
</html>
</#macro>