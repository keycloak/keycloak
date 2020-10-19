<#import "template.ftl" as layout>
    <@layout.registrationLayout displayMessage=!messagesPerField.existsError('totp'); section>
        <#if section="header">
            ${msg("doLogIn")}
            <#elseif section="form">
                <form id="kc-otp-login-form" class="${properties.kcFormClass!}" action="${url.loginAction}"
                    method="post">
                    <#if otpLogin.userOtpCredentials?size gt 1>
                        <div class="${properties.kcFormGroupClass!}">
                            <div class="${properties.kcInputWrapperClass!}">
                                <#list otpLogin.userOtpCredentials as otpCredential>
                                    <div class="${properties.kcLoginOTPListClass!}" tabindex="${otpCredential?index}">
                                    <input type="hidden" value="${otpCredential.id}">
                                        <div class="${properties.kcLoginOTPListItemHeaderClass!}">
                                            <div class="${properties.kcLoginOTPListItemIconBodyClass!}">
                                              <i class="${properties.kcLoginOTPListItemIconClass!}" aria-hidden="true"></i>
                                            </div>
                                            <div class="${properties.kcLoginOTPListItemTitleClass!}">${otpCredential.userLabel}</div>
                                        </div>
                                    </div>
                                </#list>
                            </div>
                        </div>
                    </#if>

                    <div class="${properties.kcFormGroupClass!}">
                        <div class="${properties.kcLabelWrapperClass!}">
                            <label for="otp" class="${properties.kcLabelClass!}">${msg("loginOtpOneTime")}</label>
                        </div>

                    <div class="${properties.kcInputWrapperClass!}">
                        <input id="otp" name="otp" autocomplete="off" type="text" class="${properties.kcInputClass!}"
                               autofocus aria-invalid="<#if messagesPerField.existsError('totp')>true</#if>"/>

                        <#if messagesPerField.existsError('totp')>
                            <span id="input-error-otp-code" class="${properties.kcInputErrorMessageClass!}"
                                  aria-live="polite">
                                ${kcSanitize(messagesPerField.get('totp'))?no_esc}
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
            <script type="text/javascript" src="${url.resourcesCommonPath}/node_modules/jquery/dist/jquery.min.js"></script>
            <script type="text/javascript">
            $(document).ready(function() {
                // Card Single Select
                $('.otp-tile').click(function() {
                  if ($(this).hasClass('pf-m-selected'))
                  { $(this).removeClass('pf-m-selected'); $(this).children().removeAttr('name'); }
                  else
                  { $('.otp-tile').removeClass('pf-m-selected');
                  $('.otp-tile').children().removeAttr('name');
                  $(this).addClass('pf-m-selected'); $(this).children().attr('name', 'selectedCredentialId'); }
                });

                var defaultCred = $('.otp-tile')[0];
                if (defaultCred) {
                    defaultCred.click();
                }
              });
            </script>
        </#if>
        </@layout.registrationLayout>