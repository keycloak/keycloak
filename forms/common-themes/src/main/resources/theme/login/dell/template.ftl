<#macro registrationLayout bodyClass="" displayInfo=false displayMessage=true>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"  "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" class="${properties.kcHtmlClass!}">

<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <#if properties.meta?has_content>
        <#list properties.meta?split(' ') as meta>
            <meta name="${meta?split('==')[0]}" content="${meta?split('==')[1]}"/>
        </#list>
    </#if>
    <title><#nested "title"></title>
    <link rel="icon" href="${url.resourcesPath}/img/favicon.ico" />
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

<body class="ng-cloak">
<#if realm.password && social.providers??>
<script type="text/javascript">
    var domainMap = [];
    <#list social.providers as p>
          domainMap['${p.name}'] = "${p.loginUrl}";
    </#list>
</script>
</#if>
<div class="cui-start-screen cui-theme-blue" ng-controller="LoginController as login">
    <div class="cui-start-screen-container">
        <div class="cui-start-screen-row">
            <div class="cui-start-screen-cell">
                <div class="cui-start-screen-body">
                    <cui-icon icon="dell-halo" class="cui-start-screen-dell-logo"></cui-icon>
                    <h1 class="cui-start-screen-application-name">
                        <#if client.application??>
                            <#if realm.name = 'master'>
                                Dell Identity Broker
                            <#else>
                                Single Sign On (SSO)
                            </#if>
                        <#elseif client.oauthClient??>
                            ${realm.name} ${rb.loginOauthTitle}
                        <#else>
                            Dell Identity Broker
                        </#if>
                    </h1>
                   <div id="kc-container" class="${properties.kcContainerClass!}">
                        <div id="kc-container-wrapper" class="${properties.kcContainerWrapperClass!}">

                            <div id="kc-header" class="${properties.kcHeaderClass!}">
                                <div id="kc-header-wrapper" class="${properties.kcHeaderWrapperClass!}"></div>
                            </div>
                            <#if displayMessage && message?has_content>
                            <div id="kc-feedback" class="feedback-${message.type} ${properties.kcFeedBackClass!}">
                                <div id="kc-feedback-wrapper">
                                    <span class="kc-feedback-text">${message.summary}</span>
                                </div>
                            </div>
                            <#else>
                            <div controller="MessageController as mesage">
                                <div id="kc-feedback" class="cui-start-screen-message" ng-if="message.showMessage" />
                                    <span>{{message.messageText}}</span>
                                </div>
                            </div>
                            </#if>
                            <div id="kc-content" class="${properties.kcContentClass!}">
                                <div id="kc-content-wrapper" class="${properties.kcContentWrapperClass!}">
                                    <div id="kc-form" class="${properties.kcFormAreaClass!}">
                                        <div id="kc-form-wrapper" class="${properties.kcFormAreaWrapperClass!}">
                                        <#nested "form">
                                    </div>
                                </div>

                                <#if displayInfo>
                                <div id="kc-info" class="${properties.kcInfoAreaClass!}">
                                    <div id="kc-info-wrapper" class="${properties.kcInfoAreaWrapperClass!}">
                                        <#nested "info">
                                    </div>
                                </div>
                                </#if>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="cui-start-screen-push"></div>
                </div>
            </div>
        </div>
    </div>
    <div class="cui-start-screen-copyright ng-binding">&copy; 2015 Dell Inc. ALL RIGHTS RESERVED</div>
</body>
</html>
</#macro>
