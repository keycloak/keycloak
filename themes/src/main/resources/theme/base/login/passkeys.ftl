<#macro conditionalUIData>
    <#if enableWebAuthnConditionalUI?has_content>
        <form id="webauth" action="${url.loginAction}" method="post">
            <input type="hidden" id="clientDataJSON" name="clientDataJSON"/>
            <input type="hidden" id="authenticatorData" name="authenticatorData"/>
            <input type="hidden" id="signature" name="signature"/>
            <input type="hidden" id="credentialId" name="credentialId"/>
            <input type="hidden" id="userHandle" name="userHandle"/>
            <input type="hidden" id="error" name="error"/>
        </form>
        <#if authenticators??>
            <form id="authn_select" class="${properties.kcFormClass!}">
                <#list authenticators.authenticators as authenticator>
                    <input type="hidden" name="authn_use_chk" value="${authenticator.credentialId}"/>
                </#list>
            </form>
        </#if>
        <script type="module">
           import { authenticateByWebAuthn } from "${url.resourcesPath}/js/webauthnAuthenticate.js";
           import { initAuthenticate } from "${url.resourcesPath}/js/passkeysConditionalAuth.js";

           const args = {
               isUserIdentified : ${isUserIdentified},
               challenge : '${challenge}',
               userVerification : '${userVerification}',
               rpId : '${rpId}',
               createTimeout : ${createTimeout?c}
           };

           document.addEventListener("DOMContentLoaded", (event) => initAuthenticate({errmsg : "${msg("passkey-unsupported-browser-text")?no_esc}", ...args}));
           const authButton = document.getElementById('authenticateWebAuthnButton');
           if (authButton) {
               authButton.addEventListener("click", (event) => {
                   event.preventDefault();
                   authenticateByWebAuthn({errmsg : "${msg("webauthn-unsupported-browser-text")?no_esc}", ...args});
               }, { once: true });
           }
        </script>
        <a id="authenticateWebAuthnButton" href="#" class="${properties.kcButtonSecondaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcMarginTopClass!}">
            ${kcSanitize(msg("webauthn-doAuthenticate"))?no_esc}
        </a>
    </#if>
</#macro>
