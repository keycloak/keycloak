<#import "template.ftl" as layout>
<#import "buttons.ftl" as buttons>
<@layout.registrationLayout; section>
    <#if section = "header">
        ${msg("confirmOverrideIdpTitle")}
    <#elseif section = "form">
        <form class="${properties.kcFormClass!}" id="kc-register-form" action="${url.loginAction}" method="post">
            <p>${msg("pageExpiredMsg1")} <a id="loginRestartLink" href="${url.loginRestartFlowUrl}">${msg("doClickHere")}</a></p>

            <@buttons.actionGroup id="kc-form-buttons">
                <@buttons.button name="submitAction" id="confirmOverride" label="" labelText=msg("confirmOverrideIdpContinue", idpDisplayName) class=["kcButtonLargeClass"] value="confirmOverride" />
            </@buttons.actionGroup>
        </form>
    </#if>
</@layout.registrationLayout>
