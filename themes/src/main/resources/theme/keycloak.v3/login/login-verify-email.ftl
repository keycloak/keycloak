<#import "template.ftl" as layout>
<#import "buttons.ftl" as buttons>
<@layout.registrationLayout displayInfo=!isAppInitiatedAction??; section>
<!-- template: login-verify-email.ftl -->
    <#if section = "header">
        ${msg("emailVerifyTitle")}
    <#elseif section = "form">
        <p class="instruction">
            <#if verifyEmail??>
                ${msg("emailVerifyInstruction1",verifyEmail)}
            <#else>
                ${msg("emailVerifyInstruction4",user.email)}
            </#if>
        </p>
        <#if isAppInitiatedAction??>
            <form id="kc-verify-email-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
                <@buttons.actionGroup horizontal=true>
                    <#if verifyEmail??>
                        <@buttons.button id="kc-resend" label="emailVerifyResend" type="secondary"/>
                    <#else>
                        <@buttons.button id="kc-send" label="emailVerifySend"/>
                    </#if>
                    <@buttons.button id="kc-cancel" label="doCancel" type="secondary" name="cancel-aia" value="true"/>
                </@buttons.actionGroup>
            </form>
        </#if>
    <#elseif section = "info">
        <#if !isAppInitiatedAction??>
            <p class="instruction">
                ${msg("emailVerifyInstruction2")}
                <br/>
                <a href="${url.loginAction}">${msg("doClickHere")}</a> ${msg("emailVerifyInstruction3")}
            </p>
        </#if>
    </#if>
</@layout.registrationLayout>
