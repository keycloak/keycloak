<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=social.displayInfo; section>
    <#if section = "title">
        ${msg("imperonateTitle",(realm.name!''))}
    <#elseif section = "header">
        ${msg("impersonateTitleHtml",(realm.name!''))}
    <#elseif section = "form">
            <form id="kc-form-login" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
                <input type="hidden" id="stateChecker" name="stateChecker" value="${stateChecker}">
                <#if realmList??>
                <div class="${properties.kcFormGroupClass!}">
                    <div class="${properties.kcLabelWrapperClass!}">
                        <label for="realm" class="${properties.kcLabelClass!}">${msg("realmChoice")}</label>
                    </div>

                    <div class="${properties.kcInputWrapperClass!}">
                        <select  class="${properties.kcInputClass!}" id="selectRealm" name="realm">
                            <#list realmList as r>
                            <option value="${r}">${r}</option>
                            </#list>
                        </select>
                    </div>
                </div>
                </#if>
                <div class="${properties.kcFormGroupClass!}">
                    <div class="${properties.kcLabelWrapperClass!}">
                        <label for="username" class="${properties.kcLabelClass!}"><#if !realm.registrationEmailAsUsername>${msg("usernameOrEmail")}<#else>${msg("email")}</#if></label>
                    </div>

                    <div class="${properties.kcInputWrapperClass!}">
                        <input id="username" class="${properties.kcInputClass!}" name="username" value="" type="text" autofocus />
                    </div>
                </div>
                <div class="${properties.kcFormGroupClass!}">
                    <div class="${properties.kcLabelWrapperClass!}">
                        <label for="username" class="${properties.kcLabelClass!}"></label>
                    </div>
                    <div class="${properties.kcInputWrapperClass!}">
                            <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!}" name="impersonate" id="kc-impersonate" type="submit" value="${msg("doImpersonate")}"/>
                            <input class="${properties.kcButtonClass!} ${properties.kcButtonDefaultClass!} ${properties.kcButtonLargeClass!}" name="cancel" id="kc-cancel" type="submit" value="${msg("doCancel")}"/>
                        </div>
                </div>
            </form>
    </#if>
</@layout.registrationLayout>
