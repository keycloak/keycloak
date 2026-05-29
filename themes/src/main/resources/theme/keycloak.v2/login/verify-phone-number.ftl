<#import "template.ftl" as layout>
<#import "field.ftl" as field>
<#import "buttons.ftl" as buttons>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('phoneNumber'); section>
<!-- template: verify-phone-number.ftl -->
    <#if section="header">
        ${msg("verifyPhoneNumberTitle")}
    <#elseif section="form">
        <form id="kc-verify-phone-form" class="${properties.kcFormClass!}" onsubmit="login.disabled = true; return true;" action="${url.loginAction}" method="post">
            <@field.input name="phoneNumber" label=msg("phoneNumber") value=(phoneNumber!'') autocomplete="tel" fieldName="phoneNumber" autofocus=true />

            <div class="${properties.kcFormGroupClass}">
                <div class="${properties.kcFormHelperTextClass}">
                    <div class="${properties.kcInputHelperTextClass}">
                        <div class="${properties.kcInputHelperTextItemClass}">
                            <span class="${properties.kcInputHelperTextItemTextClass}">${msg("verifyPhoneNumberInstruction")}</span>
                        </div>
                    </div>
                </div>
            </div>

            <@buttons.actionGroup>
                <@buttons.button id="kc-login" name="login" label="doContinue" class=["kcButtonPrimaryClass", "kcButtonBlockClass"] />
            </@buttons.actionGroup>
        </form>
    </#if>
</@layout.registrationLayout>
