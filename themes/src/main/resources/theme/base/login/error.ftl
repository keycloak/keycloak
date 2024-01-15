<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=false; section>
    <#if section = "header">
        ${kcSanitize(msg("errorTitle"))?no_esc}
    <#elseif section = "form">
        <div id="kc-error-message">
            <p class="instruction">${kcSanitize(message.summary)?no_esc}</p>
            <#if message.summary = msg("cookieNotFoundMessage")>
                <p class="instruction" id="secure-context-error" style="display: none;">${kcSanitize(msg("cookieNotWorkingInUnsecureContext"))?no_esc}</p>
                <script type="module">
                    if (globalThis && !globalThis.isSecureContext) {
                        document.getElementById('secure-context-error').style.display = '';
                    }
                </script>
            </#if>
            <#if skipLink??>
            <#else>
                <#if client?? && client.baseUrl?has_content>
                    <p><a id="backToApplication" href="${client.baseUrl}">${kcSanitize(msg("backToApplication"))?no_esc}</a></p>
                </#if>
            </#if>
        </div>
    </#if>
</@layout.registrationLayout>