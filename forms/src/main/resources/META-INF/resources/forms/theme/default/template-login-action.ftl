<#macro registrationLayout bodyClass isErrorPage=false>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"  "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">

<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <title>
        <#nested "title">
    </title>
    <link rel="icon" href="${template.formsPath}/theme/${template.theme}/img/favicon.ico">
    <link href="${template.themeConfig.styles}" rel="stylesheet" />
    <style type="text/css">
        body.rcue-login-register {
            background-image: url("${template.themeConfig.background}");
        }
    </style>
</head>

<body class="rcue-login-register ${bodyClass}">
    <div class="feedback-aligner">
        <#if message?has_content && message.warning>
        <div class="feedback warning show">
            <p><strong>${rb.getString('actionWarningHeader')} ${rb.getString(message.summary)}</strong><br/>${rb.getString('actionFollow')}</p>
        </div>
        </#if>
    </div>
    <#if (template.themeConfig.logo)?has_content>
        <h1>
            <a href="#" title="Go to the login page"><img src="${template.themeConfig.logo}" alt="Red Hat Logo" /></a>
        </h1>
    </#if>

    <div class="content">
        <h2>
            <#nested "header">
        </h2>

        <div class="background-area">
            <div class="form-area clearfix">
                <div class="section app-form">
                    <#if !isErrorPage && message?has_content>
                        <#if message.error>
                            <div class="feedback error bottom-left show">
                                <p>
                                    <strong id="loginError">${rb.getString(message.summary)}</strong><br/>${rb.getString('emailErrorInfo')}
                                </p>
                            </div>
                        <#elseif message.success>
                            <div class="feedback success bottom-left show">
                                <p>
                                    <strong>${rb.getString('successHeader')}</strong> ${rb.getString(message.summary)}
                                </p>
                            </div>
                        </#if>
                    </#if>

                    <h3>Application login area</h3>
                    <#nested "form">
                </div>

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