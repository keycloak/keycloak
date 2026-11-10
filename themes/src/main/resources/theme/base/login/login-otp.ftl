<#import "template.ftl" as layout>
<#import "field.ftl" as field>
<#import "buttons.ftl" as buttons>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('totp'); section>
<!-- template: login-otp.ftl -->

    <#if section="header">
        ${msg("doLogIn")}
    <#elseif section="form">
        <form id="kc-otp-login-form" class="${properties.kcFormClass!}" onsubmit="login.disabled = true; return true;" action="${url.loginAction}" method="post">
            <input id="selectedCredentialId" type="hidden" name="selectedCredentialId" value="${otpLogin.selectedCredentialId!''}">
            <#if otpLogin.userOtpCredentials?size gt 1>
                <#assign selectedCredentialId = otpLogin.selectedCredentialId!''>
                <#assign hasSelectedCredential = otpLogin.userOtpCredentials?filter(c -> c.id == selectedCredentialId)?has_content>
                <div class="${properties.kcFormGroupClass!}">
                    <div class="${properties.kcInputWrapperClass!}" role="radiogroup" aria-label="${msg("otp-display-name")}">
                        <#list otpLogin.userOtpCredentials as otpCredential>
                            <#assign selected = otpCredential.id == selectedCredentialId>
                            <div id="kc-otp-credential-${otpCredential?index}" class="${properties.kcLoginOTPListClass!}<#if selected> ${properties.kcLoginOTPListSelectedClass!}</#if>"
                                    role="radio" aria-checked="${selected?c}" tabindex="<#if selected || (!hasSelectedCredential && otpCredential?is_first)>0<#else>-1</#if>"
                                    data-otp-credential="${otpCredential.id}">
                                <span class="${properties.kcLoginOTPListItemHeaderClass!}">
                                    <span class="${properties.kcLoginOTPListItemIconBodyClass!}">
                                      <i class="${properties.kcLoginOTPListItemIconClass!}" aria-hidden="true"></i>
                                    </span>
                                    <span class="${properties.kcLoginOTPListItemTitleClass!}">${otpCredential.userLabel}</span>
                                </span>
                            </div>
                        </#list>
                    </div>
                </div>
            </#if>

            <@field.input name="otp" label=msg("loginOtpOneTime") autocomplete="one-time-code" fieldName="totp" autofocus=true />

            <@buttons.loginButton />
        </form>
        <#if otpLogin.userOtpCredentials?size gt 1>
        <script>
            <#outputformat "JavaScript">
            (() => {
                const selectedClass = ${properties.kcLoginOTPListSelectedClass!?c};
                const input = document.getElementById("selectedCredentialId");
                const options = Array.from(document.querySelectorAll("[data-otp-credential]"));
                const select = (option, focus) => {
                    input.value = option.dataset.otpCredential;
                    for (const other of options) {
                        const checked = other === option;
                        other.setAttribute("aria-checked", String(checked));
                        other.tabIndex = checked ? 0 : -1;
                        if (selectedClass && checked) {
                            other.classList.add(selectedClass);
                        } else if (selectedClass) {
                            other.classList.remove(selectedClass);
                        }
                    }
                    if (focus) {
                        option.focus();
                    }
                };
                options.forEach((option, index) => {
                    option.addEventListener("click", () => select(option, false));
                    option.addEventListener("keydown", (event) => {
                        const step = { ArrowDown: 1, ArrowRight: 1, ArrowUp: -1, ArrowLeft: -1 }[event.key];
                        if (event.key === " ") {
                            event.preventDefault();
                            select(option, false);
                        } else if (step) {
                            event.preventDefault();
                            select(options[(index + step + options.length) % options.length], true);
                        }
                    });
                });
            })();
            </#outputformat>
        </script>
        </#if>
    </#if>
</@layout.registrationLayout>