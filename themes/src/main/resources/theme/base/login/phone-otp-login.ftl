<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('phoneNumber'); section>
    <#if section="header">
        ${msg("phoneOtpLoginTitle")}
    <#elseif section="form">
        <form id="kc-phone-otp-login-form" class="${properties.kcFormClass!}" onsubmit="login.disabled = true; return true;" action="${url.loginAction}"
            method="post">
            <div class="${properties.kcFormGroupClass!}">
                <div class="${properties.kcLabelWrapperClass!}">
                    <label for="phoneNumber" class="${properties.kcLabelClass!}">${msg("phoneNumber")}</label>
                </div>
                <div class="${properties.kcInputWrapperClass!}">
                    <input id="phoneNumber" name="phoneNumber" type="tel" class="${properties.kcInputClass!}"
                           value="${(phoneNumber!'')}" autocomplete="tel" autofocus
                           aria-invalid="<#if messagesPerField.existsError('phoneNumber')>true</#if>" dir="ltr" />

                    <#if messagesPerField.existsError('phoneNumber')>
                        <span id="input-error-phone-number" class="${properties.kcInputErrorMessageClass!}" aria-live="polite">
                            ${kcSanitize(messagesPerField.get('phoneNumber'))?no_esc}
                        </span>
                    </#if>
                </div>
            </div>

            <div class="${properties.kcFormGroupClass!}">
                <div id="kc-form-options" class="${properties.kcFormOptionsClass!}">
                    <div class="${properties.kcFormOptionsWrapperClass!}">
                        ${msg("phoneOtpLoginInstruction")}
                    </div>
                </div>

                <div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
                    <input
                        class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}"
                        name="login" id="kc-login" type="submit" value="${msg("doContinue")}" />
                </div>
            </div>
        </form>
    </#if>
</@layout.registrationLayout>
