<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('recoveryCodeInput'); section>

    <#if section = "header">
        ${msg("auth-recovery-code-header")}
    <#elseif section = "form">
        <form id="kc-recovery-code-login-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
            <div class="${properties.kcFormGroupClass!}">
                <div class="${properties.kcLabelWrapperClass!}">
                    <label for="recoveryCodeInput" class="${properties.kcLabelClass!}">${msg("auth-recovery-code-prompt", recoveryAuthnCodesInputBean.codeNumber?c)}</label>
                </div>

                <div class="${properties.kcInputWrapperClass!}">
                    <input tabindex="1" id="recoveryCodeInput"
                           name="recoveryCodeInput"
                           aria-invalid="<#if messagesPerField.existsError('recoveryCodeInput')>true</#if>"
                           autocomplete="off"
                           type="text"
                           class="${properties.kcInputClass!}"
                           autofocus
                           dir="ltr"/>

                    <#if messagesPerField.existsError('recoveryCodeInput')>
                        <span id="input-error" class="${properties.kcInputErrorMessageClass!}" aria-live="polite">
                            ${kcSanitize(messagesPerField.get('recoveryCodeInput'))?no_esc}
                        </span>
                    </#if>
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