<#import "template.ftl" as layout>
<#import "buttons.ftl" as buttons>
<@layout.registrationLayout ; section>
    <#if section = "header">
        ${kcSanitize(msg("webauthn-error-title"))?no_esc}
    <#elseif section = "form">

        <script type="text/javascript">
            refreshPage = () => {
                document.getElementById('isSetRetry').value = 'retry';
                document.getElementById('executionValue').value = '${execution}';
                document.getElementById('kc-error-credential-form').requestSubmit();
            }
        </script>

        <form id="kc-error-credential-form" class="${properties.kcFormClass!}" action="${url.loginAction}"
              method="post" hidden="hidden">
            <input type="hidden" id="executionValue" name="authenticationExecution"/>
            <input type="hidden" id="isSetRetry" name="isSetRetry"/>

            <@buttons.actionGroup horizontal=true>
                <@buttons.button id="kc-try-again" name="try-again" label="doTryAgain" class=["kcButtonPrimaryClass","kcButtonBlockClass"] onclick="refreshPage()" />

                <#if isAppInitiatedAction??>
                    <form action="${url.loginAction}" class="${properties.kcFormClass!}" id="kc-webauthn-settings-form"
                          method="post">
                        <@buttons.button id="cancelWebAuthnAIA" name="cancel-aia" label="doCancel" class=["kcButtonSecondaryClass","kcButtonBlockClass"]/>
                    </form>
                </#if>
            </@buttons.actionGroup>
        </form>
    </#if>
</@layout.registrationLayout>