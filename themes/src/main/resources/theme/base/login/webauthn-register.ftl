<#import "template.ftl" as layout>
<#import "password-commons.ftl" as passwordCommons>

<@layout.registrationLayout; section>
    <#if section = "title">
        title
    <#elseif section = "header">
        <span class="${properties.kcWebAuthnKeyIcon!}"></span>
        ${kcSanitize(msg("webauthn-registration-title"))?no_esc}
    <#elseif section = "form">

        <form id="register" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
            <div class="${properties.kcFormGroupClass!}">
                <input type="hidden" id="clientDataJSON" name="clientDataJSON"/>
                <input type="hidden" id="attestationObject" name="attestationObject"/>
                <input type="hidden" id="publicKeyCredentialId" name="publicKeyCredentialId"/>
                <input type="hidden" id="authenticatorLabel" name="authenticatorLabel"/>
                <input type="hidden" id="transports" name="transports"/>
                <input type="hidden" id="error" name="error"/>
                <@passwordCommons.logoutOtherSessions/>
            </div>
        </form>

        <script type="module">
            import { registerByWebAuthn } from "${url.resourcesPath}/js/webauthnRegister.js";
            const registerButton = document.getElementById('registerWebAuthn');
            registerButton.addEventListener("click", function() {
                const input = {
                    challenge : '${challenge}',
                    userid : '${userid}',
                    username : '${username}',
                    signatureAlgorithms : [<#list signatureAlgorithms as sigAlg>${sigAlg?c},</#list>],
                    rpEntityName : '${rpEntityName}',
                    rpId : '${rpId}',
                    attestationConveyancePreference : '${attestationConveyancePreference}',
                    authenticatorAttachment : '${authenticatorAttachment}',
                    requireResidentKey : '${requireResidentKey}',
                    userVerificationRequirement : '${userVerificationRequirement}',
                    createTimeout : ${createTimeout},
                    excludeCredentialIds : '${excludeCredentialIds}',
                    initLabel : "${msg("webauthn-registration-init-label")?no_esc}",
                    initLabelPrompt : "${msg("webauthn-registration-init-label-prompt")?no_esc}",
                    errmsg : "${msg("webauthn-unsupported-browser-text")?no_esc}"
                };
                registerByWebAuthn(input);
            });
        </script>

        <input type="submit"
               class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}"
               id="registerWebAuthn" value="${msg("doRegisterSecurityKey")}"/>

        <#if !isSetRetry?has_content && isAppInitiatedAction?has_content>
            <form action="${url.loginAction}" class="${properties.kcFormClass!}" id="kc-webauthn-settings-form"
                  method="post">
                <button type="submit"
                        class="${properties.kcButtonClass!} ${properties.kcButtonDefaultClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}"
                        id="cancelWebAuthnAIA" name="cancel-aia" value="true">${msg("doCancel")}
                </button>
            </form>
        </#if>

    </#if>
</@layout.registrationLayout>
