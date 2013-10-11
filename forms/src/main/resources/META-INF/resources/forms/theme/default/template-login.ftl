<#macro registrationLayout bodyClass>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"  "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">

<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <title>
        <#nested "title">
    </title>
    <link href="${template.themeConfig.styles}" rel="stylesheet" />
    <style type="text/css">
        body.rcue-login-register {
            background-image: url("${template.themeConfig.background}");
        }
    </style>
</head>

<body class="rcue-login-register ${bodyClass}">
    <#if (template.themeConfig.logo)?has_content>
        <h1>
            <a href="#" title="Go to the home page"><img src="${template.themeConfig.logo}" alt="Logo" /></a>
        </h1>
    </#if>

    <div class="content">
        <h2>
            <#nested "header">
        </h2>

        <div class="background-area">
            <div class="form-area ${(realm.social)?string('social','')} clearfix">
                <div class="section app-form">
                    <h3>Application login area</h3>
                    <#nested "form">
                </div>

                <#if error?has_content>
                    <div class="
                     error bottom-left show">
                        <p>
                            <strong id="loginError">${rb.getString(error.summary)}</strong>
                        </p>
                    </div>
                </#if>

                <#if social.displaySocialProviders>
                    <div class="section social-login"> <span>or</span>
                        <h3>Social login area</h3>
                        <p>${rb.getString('logInWith')}</p>
                        <ul>
                            <#list social.providers as p>
                                <li><a href="${p.loginUrl}" class="zocial ${p.id}"> <span class="text">${p.name}</span></a></li>
                            </#list>
                        </ul>
                    </div>
                </#if>

                <div class="section info-area">
                    <h3>Info area</h3>
                    <#nested "info">
                </div>
            </div>
        </div>

        <#if template.themeConfig['displayPoweredBy']>
            <p class="powered">
                <a href="#">${rb.getString('poweredByKeycloak')}</a>
            </p>
        </#if>
    </div>

    <#nested "content">

</body>
</html>
</#macro>