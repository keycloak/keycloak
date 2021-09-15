<#import "template.ftl" as layout>
<@layout.registrationLayout; section>

    <#if section = "header">
        ${msg("backup-code-config-header")}
    <#elseif section = "form">
        <ol id="kc-backup-codes-list">
            <#list backupCodes.codes as code>
            <li>${code}</li>
            </#list>
        </ol>

        <p>${msg("backup-code-config-description")}</p>

        <form action="${url.loginAction}" class="${properties.kcFormClass!}" id="kc-backup-codes-settings-form" method="post">
            <input type="hidden" name="backupCodes" value="${backupCodes.backupCodesList}" />
            <input type="hidden" name="generatedAt" value="${backupCodes.generatedAt?c}" />

            <#if isAppInitiatedAction??>
                <input type="submit"
                       class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!}"
                       id="saveBackupCodesBtn" value="${msg("doSubmit")}"
                />
                <button type="submit"
                        class="${properties.kcButtonClass!} ${properties.kcButtonDefaultClass!} ${properties.kcButtonLargeClass!} ${properties.kcButtonLargeClass!}"
                        id="cancelBackupCodesBtn" name="cancel-aia" value="true" />${msg("doCancel")}
                </button>
            <#else>
                <input type="submit"
                       class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}"
                       id="saveBackupCodesBtn" value="${msg("doSubmit")}"
                />
            </#if>
        </form>
    </#if>
</@layout.registrationLayout>