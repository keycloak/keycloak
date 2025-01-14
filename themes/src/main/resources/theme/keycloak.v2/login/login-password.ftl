<#import "template.ftl" as layout>
<#import "field.ftl" as field>
<#import "buttons.ftl" as buttons>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('password'); section>
<!-- template: login-password.ftl -->
    <#if section = "header">
        ${msg("doLogIn")}
    <#elseif section = "form">
        <div id="kc-form">
            <div id="kc-form-wrapper">
                <form id="kc-form-login" class="${properties.kcFormClass!}" onsubmit="login.disabled = true; return true;" action="${url.loginAction}" method="post">
                    <@field.password name="password" label=msg("password") forgotPassword=realm.resetPasswordAllowed autofocus=true autocomplete="current-password" />
                    <@buttons.loginButton />
                </form>
            </div>
        </div>
    </#if>

</@layout.registrationLayout>
