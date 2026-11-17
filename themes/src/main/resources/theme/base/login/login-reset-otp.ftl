<#import "template.ftl" as layout>
<#import "buttons.ftl" as buttons>
<#import "otp-credential.ftl" as otpCredentialCard>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('totp'); section>
    <#if section="header">
        ${msg("doLogIn")}
    <#elseif section="form">
        <form id="kc-otp-reset-form" class="${properties.kcFormClass!}" action="${url.loginAction}"
              method="post">
            <div class="${properties.kcFormGroupClass!}" role="radiogroup" aria-labelledby="kc-otp-reset-form-description">
                <div class="${properties.kcFormGroupLabelClass!}">
                    <p id="kc-otp-reset-form-description">${msg("otp-reset-description")}</p>
                </div>
                <div class="${properties.kcInputWrapperClass!} ${properties.kcOtpCredentialListClass!}">
                    <@otpCredentialCard.cards credentials=configuredOtpCredentials.userOtpCredentials selectedId=configuredOtpCredentials.selectedCredentialId!'' />
                </div>
            </div>
            <@buttons.actionGroup id="kc-form-buttons">
                <@buttons.button id="kc-otp-reset-form-submit" label="doSubmit" class=["kcButtonLargeClass"] value=msg("doSubmit") />
            </@buttons.actionGroup>
        </form>
    </#if>
</@layout.registrationLayout>
