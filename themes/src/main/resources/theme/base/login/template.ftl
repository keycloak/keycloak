<#macro registrationLayout bodyClass="" displayInfo=false displayMessage=true displayRequiredFields=false showAnotherWayIfPresent=true>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"  "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" class="${properties.kcHtmlClass!}">

    <head>
        <meta charset="utf-8">
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
        <meta name="robots" content="noindex, nofollow">

        <#if properties.meta?has_content>
            <#list properties.meta?split(' ') as meta>
                <meta name="${meta?split('==')[0]}" content="${meta?split('==')[1]}"/>
            </#list>
        </#if>
        <title>${msg("loginTitle",(realm.displayName!''))}</title>
        <link rel="icon" href="${url.resourcesPath}/img/favicon.ico" />
        <#if properties.stylesCommon?has_content>
            <#list properties.stylesCommon?split(' ') as style>
                <link href="${url.resourcesCommonPath}/${style}" rel="stylesheet" />
            </#list>
        </#if>
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
        <#if scripts??>
            <#list scripts as script>
                <script src="${script}" type="text/javascript"></script>
            </#list>
        </#if>
    </head>

    <body class="${properties.kcBodyClass!}">
        <div class="${properties.kcLoginClass!}">
            <div class="pf-c-login__container">
                <header id="kc-header" class="${properties.kcHeaderClass!} ${properties.kcAlignTextCenter!} ${properties.kc4xlBottomPadding!}">
                    <h1>
                        ${kcSanitize(msg("loginTitleHtml",(realm.displayNameHtml!'')))?no_esc}
                    </h1>
                </header>
                <main class="pf-c-login__main">
                    <header class="pf-c-login__main-header">
                        <#if realm.internationalizationEnabled  && locale.supported?size gt 1>
                            <script type="text/javascript">
                                const toggleIntlMenu = () => {
                                    const dropdownButton = document.getElementById('kc-current-locale-link');
                                    if (dropdownButton && dropdownButton.hasAttribute('aria-expanded')) {
                                        if (dropdownButton.ariaExpanded === 'false') {
                                            dropdownButton.ariaExpanded = 'true';
                                        } else {
                                            dropdownButton.ariaExpanded = 'false';
                                        }
                                        const localeList = document.querySelector('.pf-c-dropdown__menu');
                                        if (localeList) {
                                            if(localeList.hasAttribute('hidden')) {
                                                localeList.removeAttribute('hidden');
                                            } else {
                                                localeList.setAttribute('hidden', '');
                                            }
                                        }
                                    }
                                }
                            </script>
                            <div class="${properties.kcLocaleMainClass!}">
                                <button
                                    class="pf-c-dropdown__toggle"
                                    id="kc-current-locale-link"
                                    aria-expanded="false"
                                    type="button"
                                    onclick="toggleIntlMenu()"
                                >
                                    <span class="pf-c-dropdown__toggle-text">${locale.current}</span>
                                    <span class="pf-c-dropdown__toggle-icon">
                                        <i class="fas fa-caret-down" aria-hidden="true"></i>
                                    </span>
                                </button>
                                <ul class="${properties.kcLocaleListClass!}" hidden>
                                    <#list locale.supported as l>
                                        <li class="${properties.kcLocaleListItemClass!}">
                                            <a class="${properties.kcLocaleItemClass!}" href="${l.url}">${l.label}</a>
                                        </li>
                                    </#list>
                                </ul>
                            </div>
                            
                        </#if>
                        <#if !(auth?has_content && auth.showUsername() && !auth.showResetCredentials())>
                            <#if displayRequiredFields>
                                <div class="${properties.kcContentWrapperClass!}">
                                    <h1 id="kc-page-title" class="pf-c-title pf-m-3xl"><#nested "header"></h1>
                                    <span class="${properties.kcSubHeaderClass}"><#nested "subHeader"></span>
                                    <div class="${properties.kcLabelWrapperClass!} subtitle">
                                        <span class="subtitle"><span class="required">*</span> ${msg("requiredFields")}</span>
                                    </div>
                                </div>
                            <#else>
                                <h1 id="kc-page-title" class="pf-c-title pf-m-3xl"><#nested "header"></h1>
                                <span class="${properties.kcSubHeaderClass}"><#nested "subHeader"></span>
                            </#if>
                        <#else>
                            <#if displayRequiredFields>
                                <div class="${properties.kcContentWrapperClass!}">
                                    <div class="${properties.kcLabelWrapperClass!} subtitle">
                                        <span class="subtitle"><span class="required">*</span> ${msg("requiredFields")}</span>
                                    </div>
                                    <div class="col-md-10">
                                        <#nested "show-username">
                                        <div id="kc-username" class="${properties.kcFormGroupClass!}">
                                            <label id="kc-attempted-username">${auth.attemptedUsername}</label>
                                            <a id="reset-login" href="${url.loginRestartFlowUrl}">
                                                <div class="kc-login-tooltip">
                                                    <i class="${properties.kcResetFlowIcon!}"></i>
                                                    <span class="kc-tooltip-text">${msg("restartLoginTooltip")}</span>
                                                </div>
                                            </a>
                                        </div>
                                    </div>
                                </div>
                            <#else>
                                <#nested "show-username">
                                <div id="kc-username" class="${properties.kcFormGroupClass!}">
                                    <label id="kc-attempted-username">${auth.attemptedUsername}</label>
                                    <a id="reset-login" href="${url.loginRestartFlowUrl}">
                                        <div class="kc-login-tooltip">
                                            <i class="${properties.kcResetFlowIcon!}"></i>
                                            <span class="kc-tooltip-text">${msg("restartLoginTooltip")}</span>
                                        </div>
                                    </a>
                                </div>
                            </#if>
                        </#if>
                    </header>
                    <div class="pf-c-login__main-body">
                        <#-- App-initiated actions should not see warning messages about the need to complete the action -->
                        <#-- during login.                                                                               -->
                        <#if displayMessage && message?has_content && (message.type != 'warning' || !isAppInitiatedAction??)>
                            <div class="${properties.kcAlertClass!} pf-m-<#if message.type = 'error'>danger<#else>${message.type}</#if>" >
                                <div class="pf-c-alert__icon">
                                    <#if message.type = 'success'><i class="${properties.kcFeedbackSuccessIcon!}"></i></#if>
                                    <#if message.type = 'warning'><i class="${properties.kcFeedbackWarningIcon!}"></i></#if>
                                    <#if message.type = 'error'><i class="${properties.kcFeedbackErrorIcon!}"></i></#if>
                                    <#if message.type = 'info'><i class="${properties.kcFeedbackInfoIcon!}"></i></#if>
                                </div>
                                <p class="${properties.kcAlertTitleRaw!}">
                                    <#if message.type = 'error' && message.summary?contains("timed out")>
                                        <span class="${properties.kcScreenReader!}"><#nested "errorDesc"></span>
                                        ${msg("timeoutErrorTitle")}
                                    <#elseif message.type = 'error'>
                                        <span class="${properties.kcScreenReader!}"><#nested "errorDesc"></span>
                                        <#nested "errorDesc">
                                    </#if>
                                </p>
                                <div class="pf-c-alert__description">
                                    <p>${kcSanitize(message.summary)?no_esc}</p>
                                </div>
                                <br>
                            </div>
                            
                            <@dump_object object=message/>
                        </#if>

                        <#nested "form">

                        <#if auth?has_content && auth.showTryAnotherWayLink() && showAnotherWayIfPresent>
                            <form id="kc-select-try-another-way-form" class="${properties.kcFormGroupTryAnotherWayClass}" action="${url.loginAction}" method="post">
                                <div class="${properties.kcFormGroupClass!}">
                                    <input type="hidden" name="tryAnotherWay" value="on"/>
                                    <a href="#" id="try-another-way"
                                        onclick="document.forms['kc-select-try-another-way-form'].submit();return false;">${msg("doTryAnotherWay")}</a>
                                </div>
                            </form>
                        </#if>
                    </div>
                    <footer class="pf-c-login__main-footer">
                        
                            <#if displayInfo>
                                <div class="pf-c-login__main-footer-band">
                                    <#nested "info">
                                </div>
                            </#if>
                        
                    </footer>
                </main>
            </div>
        </div>
    </body>
</html>
</#macro>

<#macro dump_object object debug=false>
  <#compress>
    <#if object??>
      <#attempt>
        <#if object?is_node>
          <#if object?node_type == "text">${object?json_string}
          <#else>${object?node_name}<#if object?node_type=="element" && object.@@?has_content><#list object.@@ as attr>
            "${attr?node_name}":"${attr?json_string}"</#list></#if>
             <#if object?children?has_content><#list object?children as item>
                <@dump_object object=item/></#list><#else>${object}</#if>"${object?node_name}"</#if>
             <#elseif object?is_method>
               "#method"
             <#elseif object?is_sequence>
               [<#list object as item><@dump_object object=item/><#if !item?is_last>, </#if></#list>]
             <#elseif object?is_hash_ex>
               {<#list object as key, item>"${key?json_string}":<@dump_object object=item/><#if !item?is_last>, </#if></#list>}
             <#else>
              "${object?string?json_string}"
             </#if>
      <#recover>
        <#if !debug>"<!-- </#if>LOG: Could not parse object <#if debug><pre>${.error}</pre><#else>-->"</#if>
      </#attempt>
    <#else>
      null
    </#if>
  </#compress>
</#macro>
