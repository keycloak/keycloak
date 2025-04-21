<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "header">
        ${msg("confirmOverrideIdpTitle")}
    <#elseif section = "form">
        <form id="kc-register-form" action="${url.loginAction}" method="post">
            ${msg("pageExpiredMsg1")} <a id="loginRestartLink" href="${url.loginRestartFlowUrl}">${msg("doClickHere")}</a>

            <button type="submit" class="${properties.kcButtonClass!} ${properties.kcButtonDefaultClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}" name="submitAction" id="confirmOverride" value="confirmOverride">${msg("confirmOverrideIdpContinue", idpDisplayName)}</button>
        </form>
    </#if>
</@layout.registrationLayout>
