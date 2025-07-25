<#import "template.ftl" as layout>
<#import "password-commons.ftl" as passwordCommons>
<#import "buttons.ftl" as buttons>

<@layout.registrationLayout; section>
    <#if section = "title">
        title
    <#elseif section = "header">
        <span class="${properties.kcWebAuthnKeyIcon!}"></span>
        ${kcSanitize(msg("webauthn-registration-title"))?no_esc}
    <#elseif section = "form">
    <div class="${properties.kcFormClass!}">
        <form id="register" action="${url.loginAction}" method="post" >
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
                    createTimeout : ${createTimeout?c},
                    excludeCredentialIds : '${excludeCredentialIds}',
                    initLabel : "${msg("webauthn-registration-init-label")?no_esc}",
                    initLabelPrompt : "${msg("webauthn-registration-init-label-prompt")?no_esc}",
                    errmsg : "${msg("webauthn-unsupported-browser-text")?no_esc}"
                };
                registerByWebAuthn(input);
            },  { once: true });
        </script>

            <@buttons.actionGroup horizontal=true>
                <@buttons.button id="registerWebAuthn" label="doRegisterSecurityKey" class=["kcButtonPrimaryClass","kcButtonBlockClass"]/>
                <#if !isSetRetry?has_content && isAppInitiatedAction?has_content>
                    <form class="${properties.kcFormClass!}" action="${url.loginAction}"
                          id="kc-webauthn-settings-form" method="post">
                        <@buttons.button id="cancelWebAuthnAIA" name="cancel-aia" label="doCancel" class=["kcButtonSecondaryClass","kcButtonBlockClass"]/>
                    </form>
                </#if>
            </@buttons.actionGroup>
    </div>
    </#if>
</@layout.registrationLayout>
