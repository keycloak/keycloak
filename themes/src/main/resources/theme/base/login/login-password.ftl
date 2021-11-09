<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('password'); section>
    <#if section = "header">
        ${msg("doLogIn")}
    <#elseif section = "form">
        <div id="kc-form">
            <div id="kc-form-wrapper">
                <form id="kc-form-login" class="${properties.kcFormClass!}" onsubmit="login.disabled = true; return true;" action="${url.loginAction}"
                      method="post">
                    <div class="${properties.kcFormGroupClass!} no-bottom-margin">
                        <hr class="pf-c-divider" />
                        <div class="pf-c-form__group-label">
                            <label for="password" class="${properties.kcLabelClass!}">
                                <span class="pf-c-form__label-text">${msg("password")}</span>
                            </label>
                        </div>
                        <div class="pf-c-form__group-control">
                            <input tabindex="2" id="password" class="${properties.kcInputClass!}" name="password"
                                type="password" autocomplete="on"
                                aria-invalid="<#if messagesPerField.existsError('password')>true</#if>"
                            />
                            <#if messagesPerField.existsError('password')>
                                <span id="input-error-password" class="${properties.kcInputErrorMessageClass!}" aria-live="polite">
                                    ${kcSanitize(messagesPerField.get('password'))?no_esc}
                                </span>
                            </#if>
                        </div>
                    </div>
                    <#if realm.resetPasswordAllowed>
                        <div class="${properties.kcFormGroupClass!} ${properties.kcFormSettingClass!}">
                            <div id="kc-form-options"></div>
                            <div class="${properties.kcFormOptionsWrapperClass!}">
                                <span>
                                    <a tabindex="5" href="${url.loginResetCredentialsUrl}">
                                        ${msg("doForgotPassword")}
                                    </a>
                                </span>
                            </div>
                        </div>
                    </#if>
                    <div class="${properties.kcFormGroupClass!}">
                    <input tabindex="4" class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}" name="login" id="kc-login" type="submit" value="${msg("doLogIn")}"/>
                  </div>
            </form>
        </div>
      </div>
    </#if>

</@layout.registrationLayout>
