<#import "template.ftl" as layout>
<@layout.registrationLayout; section>

    <#if section = "header">
        ${msg("recovery-code-config-header")}
    <#elseif section = "form">
        <ol id="kc-recovery-codes-list">
            <#list recoveryAuthnCodesConfigBean.generatedRecoveryAuthnCodesList as code>
                <li>${code[0..3]}-${code[4..7]}-${code[8..]}</li>
            </#list>
        </ol>

        <p>${msg("recovery-code-config-description")}</p>

        <form action="${url.loginAction}" class="${properties.kcFormClass!}" id="kc-recovery-codes-settings-form" method="post">
            <input type="hidden" name="generatedRecoveryAuthnCodes" value="${recoveryAuthnCodesConfigBean.generatedRecoveryAuthnCodesAsString}" />
            <input type="hidden" name="generatedAt" value="${recoveryAuthnCodesConfigBean.generatedAt?c}" />
            <input type="hidden" name="userLabel" value="${msg("recovery-code-config-user-label")}" />

            <#if isAppInitiatedAction??>
                <input type="submit"
                       class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!}"
                       id="saveRecoveryAuthnCodesBtn" value="${msg("doSubmit")}"
                />
                <button type="submit"
                        class="${properties.kcButtonClass!} ${properties.kcButtonDefaultClass!} ${properties.kcButtonLargeClass!} ${properties.kcButtonLargeClass!}"
                        id="cancelRecoveryAuthnCodesBtn" name="cancel-aia" value="true" />${msg("doCancel")}
                </button>
            <#else>
                <input type="submit"
                       class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}"
                       id="saveRecoveryAuthnCodesBtn" value="${msg("doSubmit")}"
                />
            </#if>
        </form>
    </#if>
</@layout.registrationLayout>
