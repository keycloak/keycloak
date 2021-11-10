<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('username') displayInfo=(realm.password && realm.registrationAllowed && !registrationDisabled??); section>
    <#if section = "header">
        ${msg("loginAccountTitle")}
    <#elseif section = "form">
        <div id="kc-form">
            <div id="kc-form-wrapper">
                <#if realm.password>
                    <form id="kc-form-login" class="${properties.kcFormClass!}" onsubmit="login.disabled = true; return true;" action="${url.loginAction}"
                          method="post">
                        <div class="${properties.kcFormGroupClass!}">
                            <label for="username" class="${properties.kcLabelClass!}">
                                <span class="pf-c-form__label-text">
                                    <#if !realm.loginWithEmailAllowed>
                                        ${msg("username")}
                                    <#elseif !realm.registrationEmailAsUsername>
                                        ${msg("usernameOrEmail")}
                                    <#else>
                                        ${msg("email")}
                                    </#if>
                                </span>
                            </label>

                            <#if usernameEditDisabled??>
                                <input tabindex="1" id="username"
                                       aria-invalid="<#if message?has_content && message.type = 'error'>true</#if>"
                                       class="${properties.kcInputClass!}" name="username"
                                       value="${(login.username!'')}"
                                       type="text" disabled/>
                            <#else>
                                <input tabindex="1" id="username"
                                       aria-invalid="<#if messagesPerField.existsError('username')>true</#if>"
                                       class="${properties.kcInputClass!}" name="username"
                                       value="${(login.username!'')}"
                                       type="text" autofocus autocomplete="off"/>
                            </#if>

                            <#if messagesPerField.existsError('username')>
                                <span id="input-error-username" class="${properties.kcInputErrorMessageClass!}" aria-live="polite">
                                    ${kcSanitize(messagesPerField.get('username'))?no_esc}
                                </span>
                            </#if>
                        </div>
                        <#if realm.rememberMe && !usernameEditDisabled??>
                            <div class="${properties.kcFormGroupClass!} ${properties.kcFormSettingClass!}">
                                <div id="kc-form-options">
                                        <div class="pf-c-check">
                                            <#if login.rememberMe??>
                                                <input class="pf-c-check__input" tabindex="3" id="rememberMe" name="rememberMe" type="checkbox"
                                                    checked> ${msg("rememberMe")}
                                            <#else>
                                                <input class="pf-c-check__input" tabindex="3" id="rememberMe" name="rememberMe"
                                                    type="checkbox"> 
                                            </#if>
                                            <label class="pf-c-check__label" for="rememberMe">${msg("rememberMe")}</label>
                                        </div>
                                </div>
                            </div>
                        </#if>

                        <div class="${properties.kcFormGroupClass!}">
                            <input tabindex="4"
                                   class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}"
                                   name="login" id="kc-login" type="submit" value="${msg("doLogIn")}"/>
                        </div>
                    </form>
                </#if>
            </div>
        </div>

            <#if realm.password && social.providers??>
                <div id="kc-social-providers" class="${properties.kcFormSocialAccountSectionClass!}">
                    <hr/>
                    <h4>${msg("identity-provider-login-label")}</h4>

                    <ul class="${properties.kcFormSocialAccountListClass!} <#if social.providers?size gt 3>${properties.kcFormSocialAccountListGridClass!}</#if>">
                        <#list social.providers as p>
                            <a id="social-${p.alias}" class="${properties.kcFormSocialAccountListButtonClass!} <#if social.providers?size gt 3>${properties.kcFormSocialAccountGridItem!}</#if>"
                               type="button" href="${p.loginUrl}">
                                <#if p.iconClasses?has_content>
                                    <i class="${properties.kcCommonLogoIdP!} ${p.iconClasses!}" aria-hidden="true"></i>
                                    <span class="${properties.kcFormSocialAccountNameClass!} kc-social-icon-text">${p.displayName}</span>
                                <#else>
                                    <span class="${properties.kcFormSocialAccountNameClass!}">${p.displayName}</span>
                                </#if>
                            </a>
                        </#list>
                    </ul>
                </div>
            </#if>

    <#elseif section = "info" >
        <#if realm.password && realm.registrationAllowed && !registrationDisabled??>
            <div id="kc-registration">
                <span>${msg("noAccount")} <a tabindex="6" href="${url.registrationUrl}">${msg("doRegister")}</a></span>
            </div>
        </#if>
    </#if>

</@layout.registrationLayout>
