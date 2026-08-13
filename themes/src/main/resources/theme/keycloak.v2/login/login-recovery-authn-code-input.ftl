<#import "template.ftl" as layout>
<#import "field.ftl" as field>
<#import "buttons.ftl" as buttons>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('recoveryCodeInput'); section>
<!-- template: login-recovery-authn-code-input.ftl -->
    <#if section = "header">
        ${msg("auth-recovery-code-header")}
    <#elseif section = "form">
        <form id="kc-recovery-code-login-form" class="${properties.kcFormClass!}" onsubmit="login.disabled = true; return true;" action="${url.loginAction}" method="post">
            <@field.input name="recoveryCodeInput" label=msg("auth-recovery-code-prompt", recoveryAuthnCodesInputBean.codeNumber?c) autofocus=true />

            <@buttons.loginButton />
        </form>
    </#if>
</@layout.registrationLayout>