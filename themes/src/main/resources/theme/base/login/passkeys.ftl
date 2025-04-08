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
        <script type="module">
           import { authenticateByWebAuthn } from "${url.resourcesPath}/js/webauthnAuthenticate.js";
           import { initAuthenticate } from "${url.resourcesPath}/js/passkeysConditionalAuth.js";

           const args = {
               isUserIdentified : ${isUserIdentified},
               challenge : '${challenge}',
               userVerification : '${userVerification}',
               rpId : '${rpId}',
               createTimeout : ${createTimeout},
               errmsg : "${msg("passkey-unsupported-browser-text")?no_esc}"
           };

           document.addEventListener("DOMContentLoaded", (event) => initAuthenticate(args));
        </script>
    </#if>
</#macro>
