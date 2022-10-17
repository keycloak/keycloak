<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('totp'); section>
    <#if section="header">
        ${msg("doLogIn")}
    <#elseif section="form">
        <form id="kc-otp-login-form" class="${properties.kcFormClass!}" action="${url.loginAction}"
            method="post" onsubmit="SubmitFunction()">
            <#if otpLogin.userOtpCredentials?size gt 1>
                <div class="${properties.kcFormGroupClass!}">
                    <div class="${properties.kcInputWrapperClass!}">
                        <#list otpLogin.userOtpCredentials as otpCredential>
                            <label class="${properties.kcLoginOTPListClass!}" tabindex="${otpCredential?index}" name="selector" onclick="clickfunc(this,'${otpCredential.id}','${otpCredential.digits}')" onload="clickfunc(this,'${otpCredential.id}','${otpCredential.digits}')" data-digits="${otpCredential.digits}" id="${otpCredential.id}">
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
            </#if>

            <input id="kc-otp-credential" class="${properties.kcLoginOTPListInputClass!} kc-otp-credential" type="hidden" name="selectedCredentialId">
            <div class="${properties.kcFormGroupClass!}">
                <div class="${properties.kcLabelWrapperClass!}">
                    <label for="otp" class="${properties.kcLabelClass!}">${msg("loginOtpOneTime")}</label>
                </div>

            <div class="${properties.kcInputWrapperClass!}">
                <div name="otp-elements" id="OTPInput" class="kc-otp-fields">
                </div>
                <input id="otp" name="otp" autocomplete="off" type="hidden" class="${properties.kcInputClass!}"
                       aria-invalid="<#if messagesPerField.existsError('totp')>true</#if>"/>

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
        <script>
            function clickfunc(element,id,digits){           
                document.getElementsByName("selector").forEach(ele => {
                    ele.dataset.selected=(ele==element);
                })
                const inp = document.getElementsByName("selectedCredentialId")[0]
                inp.setAttribute('value',id)
                generateElements(digits)
            }

            function generateElements(digits){
                const element = document.getElementById('OTPInput');
                var arr = []
                for (let i = 0; i < digits; i++) {
                  let inputField = document.createElement('input'); // Creates a new input element
                  inputField.className = "pf-c-form-control kc-otp-input";
                  inputField.style.cssText = "color: transparent; text-shadow: 0 0 0 gray;";
                  inputField.setAttribute("autocomplete", "off");
                  inputField.id = 'otp-field' + i; // Don't remove
                  inputField.name = 'otp-field' + i; // Don't remove
                  inputField.maxLength = 1;
                  arr.push(inputField);
                }
                element.replaceChildren(...arr);
                /*  This is for switching back and forth the input box for user experience */
                const inputs = document.querySelectorAll('#OTPInput > *[id]');
                for (let i = 0; i < inputs.length; i++) {
                  inputs[i].addEventListener('keydown', function(event) {
                    if (event.key === "Enter") return
                    if (event.key === "Backspace") {
                        if (i != 0) {
                          inputs[i - 1].focus();
                        }
                        inputs[i].value = '';
                    } else if (event.key === "ArrowLeft" && i !== 0) {
                      inputs[i - 1].focus();
                    } else if (event.key === "ArrowRight" && i !== inputs.length - 1) {
                      inputs[i + 1].focus();
                    } else if (event.key != "ArrowLeft" && event.key != "ArrowRight") {
                      inputs[i].setAttribute("type", "text");
                      inputs[i].value = '';
                    }
                  });
                inputs[i].addEventListener('input', function() {
                  if (i === inputs.length - 1 && inputs[i].value !== '') {
                    return true;
                  } else if (inputs[i].value !== '') {
                    inputs[i + 1].focus();
                  }
                });
                }
                inputs[0].focus()
            }
            function SubmitFunction(){
                var finalOTP = "";
                const OTPinputs = document.querySelectorAll("#OTPInput > input");
                OTPinputs.forEach(input =>{
                    finalOTP += input.value
                })
                document.getElementById("otp").value=finalOTP
            }
            function onMount(){
                const id = "${otpLogin.selectedCredentialId}"
                const element = document.getElementById(id)
                const digits = element.dataset.digits
                clickfunc(element,id,digits)
            }
            onMount()
        </script>
        <style>
        [data-selected="true"]{
            background-color:var(--pf-c-tile--focus--after--BackgroundColor);
            color:white;
            border-radius:5px;
        }
        .kc-otp-fields{
            display:flex;
            justify-content:space-between;
            align-items:center;
            height:60px;

        }
        .kc-otp-input{
            height:50px !important;
            width:50px;
            border-radius:5px;
            text-align:center;
        }
        </style>
    </#if>
</@layout.registrationLayout>
