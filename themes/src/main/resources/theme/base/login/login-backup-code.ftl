<#import "template.ftl" as layout>
<@layout.registrationLayout; section>

    <#if section = "header">
        ${msg("auth-backup-code-header")}
    <#elseif section = "form">
        <form id="kc-backup-code-login-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
            <div class="${properties.kcFormGroupClass!}">
                <div class="${properties.kcLabelWrapperClass!}">
                    <label for="backupCode" class="${properties.kcLabelClass!}">${msg("auth-backup-code-prompt", backupCodes.codeNumber?c)}</label>
                </div>

                <div class="${properties.kcInputWrapperClass!}">
                    <input id="backupCode" name="backupCode" autocomplete="off" type="text" class="${properties.kcInputClass!}" autofocus/>
                </div>
            </div>

            <div class="${properties.kcFormGroupClass!}">
                <div id="kc-form-options" class="${properties.kcFormOptionsClass!}">
                    <div class="${properties.kcFormOptionsWrapperClass!}">
                    </div>
                </div>

                <div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
                    <input
                        class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}"
                        name="login" id="kc-login" type="submit" value="${msg("doLogIn")}" />
                </div>
            </div>
        </form>
    </#if>
</@layout.registrationLayout>