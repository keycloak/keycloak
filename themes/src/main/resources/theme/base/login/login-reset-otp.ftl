<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('totp'); section>
    <#if section="header">
        ${msg("doLogIn")}
    <#elseif section="form">
        <form id="kc-otp-reset-form" class="${properties.kcFormClass!}" action="${url.loginAction}"
              method="post">
            <div class="${properties.kcFormGroupClass!}">
                <div class="${properties.kcInputWrapperClass!}">
                    <p id="kc-otp-reset-form-description">${msg("otp-reset-description")}</p>

                    <#list configuredOtpCredentials.userOtpCredentials as otpCredential>
                        <input id="kc-otp-credential-${otpCredential?index}" class="${properties.kcLoginOTPListInputClass!}" type="radio" name="selectedCredentialId" value="${otpCredential.id}" <#if otpCredential.id == configuredOtpCredentials.selectedCredentialId>checked="checked"</#if>>
                        <label for="kc-otp-credential-${otpCredential?index}" class="${properties.kcLoginOTPListClass!}" tabindex="${otpCredential?index}">
                                    <span class="${properties.kcLoginOTPListItemHeaderClass!}">
                                        <span class="${properties.kcLoginOTPListItemIconBodyClass!}">
                                          <i class="${properties.kcLoginOTPListItemIconClass!}" aria-hidden="true"></i>
                                        </span>
                                        <span class="${properties.kcLoginOTPListItemTitleClass!}">${otpCredential.userLabel}</span>
                                    </span>
                        </label>
                    </#list>

                    <div class="${properties.kcFormGroupClass!}">
                        <div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
                            <input id="kc-otp-reset-form-submit" class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}" type="submit" value="${msg("doSubmit")}"/>
                        </div>
                    </div>
                </div>
            </div>
        </form>
    </#if>
</@layout.registrationLayout>
