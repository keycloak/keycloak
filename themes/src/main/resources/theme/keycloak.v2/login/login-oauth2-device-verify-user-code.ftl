<#import "template.ftl" as layout>
<#import "field.ftl" as field>
<#import "buttons.ftl" as buttons>
<@layout.registrationLayout; section>
<!-- template: login-oauth2-device-verify-user-code.ftl -->
    <#if section = "header">
        ${msg("oauth2DeviceVerificationTitle")}
    <#elseif section = "form">
        <form id="kc-user-verify-device-user-code-form" class="${properties.kcFormClass!}" action="${url.oauth2DeviceVerificationAction}" method="post">
            <@field.input name="device_user_code" label=msg("verifyOAuth2DeviceUserCode") autofocus=true />

            <@buttons.actionGroup>
                <@buttons.button id="kc-login" label="doSubmit" class=["kcButtonPrimaryClass", "kcButtonBlockClass"] />
            </@buttons.actionGroup>
        </form>
    </#if>
</@layout.registrationLayout>
