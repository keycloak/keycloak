<#import "template.ftl" as layout>
<#import "field.ftl" as field>
<#import "buttons.ftl" as buttons>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('totp'); section>
<!-- template: phone-otp-code.ftl -->
    <#if section="header">
        ${msg("phoneOtpCodeTitle")}
    <#elseif section="form">
        <form id="kc-phone-otp-code-form" class="${properties.kcFormClass!}" onsubmit="login.disabled = true; return true;" action="${url.loginAction}" method="post">
            <#if phoneNumber??>
                <div class="${properties.kcFormGroupClass}">
                    <div class="${properties.kcFormHelperTextClass}">
                        <div class="${properties.kcInputHelperTextClass}">
                            <div class="${properties.kcInputHelperTextItemClass}">
                                <span class="${properties.kcInputHelperTextItemTextClass}">${msg("phoneOtpCodeInstruction", phoneNumber)}</span>
                            </div>
                        </div>
                    </div>
                </div>
            </#if>

            <@field.input name="otp" label=msg("loginOtpOneTime") autocomplete="one-time-code" fieldName="totp" autofocus=true />

            <@buttons.actionGroup>
                <@buttons.button id="kc-login" name="login" label="doLogIn" class=["kcButtonPrimaryClass", "kcButtonBlockClass"] />
                <@buttons.button id="kc-resend" name="resend" label="phoneOtpResend" class=["kcButtonSecondaryClass", "kcButtonBlockClass"] />
            </@buttons.actionGroup>
        </form>
    </#if>
</@layout.registrationLayout>
