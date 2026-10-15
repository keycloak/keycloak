<#import "template.ftl" as layout>
<#import "buttons.ftl" as buttons>
<@layout.registrationLayout displayInfo=true; section>
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
                <div class="${properties.kcFormGroupClass!}">
                    <div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
                        <#if verifyEmail??>
                            <@buttons.button label="emailVerifyResend" type="secondary" fullWidth=false class=["kcButtonLargeClass"] value=msg("emailVerifyResend") />
                        <#else>
                            <@buttons.button label="emailVerifySend" fullWidth=false class=["kcButtonLargeClass"] value=msg("emailVerifySend") />
                        </#if>
                        <@buttons.button name="cancel-aia" label="doCancel" type="secondary" fullWidth=false class=["kcButtonLargeClass"] value="true" formnovalidate="formnovalidate" />
                    </div>
                </div>
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
