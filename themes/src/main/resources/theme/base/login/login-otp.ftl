<#import "template.ftl" as layout>
<#import "field.ftl" as field>
<#import "buttons.ftl" as buttons>
<#import "otp-credential.ftl" as otpCredentialCard>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('totp'); section>
<!-- template: login-otp.ftl -->

    <#if section="header">
        ${msg("loginOtpTitle")}
    <#elseif section="form">
        <form id="kc-otp-login-form" class="${properties.kcFormClass!}" onsubmit="login.disabled = true; return true;" action="${url.loginAction}" method="post">
            <#if otpLogin.userOtpCredentials?size gt 1>
                <div class="${properties.kcFormGroupClass!}" role="radiogroup" aria-labelledby="kc-otp-credential-selection-label">
                    <div class="${properties.kcFormGroupLabelClass!}">
                        <span id="kc-otp-credential-selection-label" class="${properties.kcFormLabelClass!}">${msg("loginOtpSelectAuthenticator")}</span>
                    </div>
                    <div class="${properties.kcInputWrapperClass!} ${properties.kcOtpCredentialListClass!}">
                        <@otpCredentialCard.cards credentials=otpLogin.userOtpCredentials selectedId=otpLogin.selectedCredentialId!'' />
                    </div>
                </div>
            <#else>
                <input id="selectedCredentialId" type="hidden" name="selectedCredentialId" value="${otpLogin.selectedCredentialId!''}">
            </#if>

            <@field.input name="otp" label=msg("loginOtpOneTime") autocomplete="one-time-code" inputmode="numeric" fieldName="totp" autofocus=(otpLogin.userOtpCredentials?size lte 1) />

            <@buttons.loginButton />
        </form>
    </#if>
</@layout.registrationLayout>
