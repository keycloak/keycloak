<#import "template.ftl" as layout>
<#import "field.ftl" as field>
<#import "buttons.ftl" as buttons>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('totp'); section>
<!-- template: login-otp.ftl -->

    <#if section="header">
        ${msg("doLogIn")}
    <#elseif section="form">
        <form id="kc-otp-login-form" class="${properties.kcFormClass!}" onsubmit="login.disabled = true; return true;" action="${url.loginAction}" method="post">
            <#if otpLogin.userOtpCredentials?size gt 1>
                <div class="${properties.kcFormGroupClass!}">
                    <div class="${properties.kcInputWrapperClass!}" role="radiogroup" aria-label="${msg("loginOtpCredentialSelection")}">
                        <#list otpLogin.userOtpCredentials as otpCredential>
                            <#assign otpCredentialSelected = otpCredential.id == (otpLogin.selectedCredentialId!'')>
                            <input id="kc-otp-credential-${otpCredential?index}" class="${properties.kcLoginOTPListInputClass!}" type="radio"
                                    name="selectedCredentialId" value="${otpCredential.id}" <#if otpCredentialSelected>checked</#if>>
                            <label for="kc-otp-credential-${otpCredential?index}"
                                    class="${properties.kcLoginOTPListClass!}<#if otpCredentialSelected> ${properties.kcLoginOTPListSelectedClass!}</#if>">
                                <span class="${properties.kcLoginOTPListItemHeaderClass!}">
                                    <span class="${properties.kcLoginOTPListItemIconBodyClass!}">
                                      <i class="${properties.kcLoginOTPListItemIconClass!}" aria-hidden="true"></i>
                                    </span>
                                    <span class="${properties.kcLoginOTPListItemTitleClass!}">${otpCredential.userLabel}</span>
                                </span>
                            </label>
                        </#list>
                    </div>
                </div>
            <#else>
                <input id="selectedCredentialId" type="hidden" name="selectedCredentialId" value="${otpLogin.selectedCredentialId!''}">
            </#if>

            <@field.input name="otp" label=msg("loginOtpOneTime") autocomplete="one-time-code" fieldName="totp" autofocus=true />

            <@buttons.loginButton />
        </form>
        <script>
            <#outputformat "JavaScript">
            // keep the tile styling in sync with the checked radio
            document.querySelectorAll("input[name='selectedCredentialId'][type='radio']").forEach(radio => radio.addEventListener("change", () => {
                document.querySelectorAll("input[name='selectedCredentialId'][type='radio']").forEach(r =>
                    document.querySelector("label[for='" + r.id + "']").classList.toggle(${properties.kcLoginOTPListSelectedClass!?c}, r.checked));
            }));
            </#outputformat>
        </script>
    </#if>
</@layout.registrationLayout>