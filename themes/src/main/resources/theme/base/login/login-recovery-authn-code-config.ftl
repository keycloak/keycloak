<#import "template.ftl" as layout>
<@layout.registrationLayout; section>

<#if section = "header">
    ${msg("recovery-code-config-header")}
<#elseif section = "form">
    <!-- warning -->
    <div class="pf-c-alert pf-m-warning ${properties.kcRecoveryCodesWarning}" aria-label="Warning alert">
        <div class="pf-c-alert__icon">
            <i class="pficon-warning-triangle-o" aria-hidden="true"></i>
        </div>
        <h4 class="pf-c-alert__title">
            <span class="pf-screen-reader">Warning alert:</span>
            ${msg("recovery-code-config-warning-title")}
        </h4>
        <div class="pf-c-alert__description">
            <p>${msg("recovery-code-config-warning-message")}</p>
        </div>
    </div>

    <ol id="kc-recovery-codes-list" class="${properties.kcRecoveryCodesList!}">
        <#list recoveryAuthnCodesConfigBean.generatedRecoveryAuthnCodesList as code>
            <li><span>${code?counter}:</span> ${code[0..3]}-${code[4..7]}-${code[8..]}</li>
        </#list>
    </ol>

    <!-- actions -->
    <div class="${properties.kcRecoveryCodesActions}">
        <button id="printRecoveryCodes" class="pf-c-button pf-m-link" type="button">
            <i class="pficon-print"></i> ${msg("recovery-codes-print")}
        </button>
        <button id="downloadRecoveryCodes" class="pf-c-button pf-m-link" type="button">
            <i class="pficon-save"></i> ${msg("recovery-codes-download")}
        </button>
        <button id="copyRecoveryCodes" class="pf-c-button pf-m-link" type="button">
            <i class="pficon-blueprint"></i> ${msg("recovery-codes-copy")}
        </button>
    </div>

    <!-- confirmation checkbox -->
    <div class="${properties.kcCheckClass} ${properties.kcRecoveryCodesConfirmation}">
        <input class="${properties.kcCheckInputClass}" type="checkbox" id="kcRecoveryCodesConfirmationCheck" name="kcRecoveryCodesConfirmationCheck" 
        onchange="document.getElementById('saveRecoveryAuthnCodesBtn').disabled = !this.checked;"
        />
        <label class="${properties.kcCheckLabelClass}" for="kcRecoveryCodesConfirmationCheck">${msg("recovery-codes-confirmation-message")}</label>
    </div>

    <form action="${url.loginAction}" class="${properties.kcFormClass!}" id="kc-recovery-codes-settings-form" method="post">
        <input type="hidden" name="generatedRecoveryAuthnCodes" value="${recoveryAuthnCodesConfigBean.generatedRecoveryAuthnCodesAsString}" />
        <input type="hidden" name="generatedAt" value="${recoveryAuthnCodesConfigBean.generatedAt?c}" />
        <input type="hidden" name="userLabel" value="${msg("recovery-code-config-user-label")}" />

        <#if isAppInitiatedAction??>
            <input type="submit"
            class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!}"
            id="saveRecoveryAuthnCodesBtn" value="${msg("recovery-codes-action-complete")}"
            disabled
            />
            <button type="submit"
                class="${properties.kcButtonClass!} ${properties.kcButtonDefaultClass!} ${properties.kcButtonLargeClass!} ${properties.kcButtonLargeClass!}"
                id="cancelRecoveryAuthnCodesBtn" name="cancel-aia" value="true" />${msg("recovery-codes-action-cancel")}
            </button>
        <#else>
            <input type="submit"
            class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}"
            id="saveRecoveryAuthnCodesBtn" value="${msg("recovery-codes-action-complete")}"
            disabled
            />
        </#if>
    </form>

    <script>
        /* copy to clipboard utility function
        // TODO: after we drop support for IE11 change this to use navigator.clipboard 
        // - https://github.com/w3c/clipboard-apis/blob/main/explainer.adoc#writing-to-the-clipboard */
        function copyToClipboard(text) {
            const listener = function(ev) {
                ev.preventDefault();
                ev.clipboardData.setData('text/plain', text);
            };
            document.addEventListener('copy', listener);
            document.execCommand('copy');
            document.removeEventListener('copy', listener);
        };

        function copyRecoveryCodes() {
            var recoveryCodes = document.querySelector(".kc-recovery-codes-list");
            console.log(recoveryCodes);
            copyToClipboard(recoveryCodes);
            setTimeout(function() {
                console.log("copied");
            }, 1500);
        }

        var copyButton = document.getElementById("copyRecoveryCodes");
        copyButton && copyButton.addEventListener("click", copyRecoveryCodes, null);
    </script>
</#if>
</@layout.registrationLayout>
