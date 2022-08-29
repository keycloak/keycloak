<#import "template.ftl" as layout>
<@layout.registrationLayout; section>

<#if section = "header">
    ${msg("recovery-code-config-header")}
<#elseif section = "form">
    <!-- warning -->
    <div class="pf-c-alert pf-m-warning pf-m-inline ${properties.kcRecoveryCodesWarning}" aria-label="Warning alert">
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
        <input type="hidden" id="userLabel" name="userLabel" value="${msg("recovery-codes-label-default")}" />

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
        /* copy recovery codes  */
        function copyRecoveryCodes() {
            var tmpTextarea = document.createElement("textarea");
            var codes = document.getElementById("kc-recovery-codes-list").getElementsByTagName("li");
            for (i = 0; i < codes.length; i++) {
                tmpTextarea.value = tmpTextarea.value + codes[i].innerText + "\n";
            }
            document.body.appendChild(tmpTextarea);
            tmpTextarea.select();
            document.execCommand("copy");
            document.body.removeChild(tmpTextarea);
        }

        var copyButton = document.getElementById("copyRecoveryCodes");
        copyButton && copyButton.addEventListener("click", function () {
            copyRecoveryCodes();
        });

        /* download recovery codes  */
        function formatCurrentDateTime() {
            var dt = new Date();
            var options = {
                month: 'long',
                day: 'numeric',
                year: 'numeric',
                hour: 'numeric',
                minute: 'numeric',
                timeZoneName: 'short'
            };

            return dt.toLocaleString('en-US', options);
        }

        function parseRecoveryCodeList() {
            var recoveryCodes = document.querySelectorAll(".kc-recovery-codes-list li");
            var recoveryCodeList = "";

            for (var i = 0; i < recoveryCodes.length; i++) {
                var recoveryCodeLiElement = recoveryCodes[i].innerText;
                recoveryCodeList += recoveryCodeLiElement + "\r\n";
            }

            return recoveryCodeList;
        }

        function buildDownloadContent() {
            var recoveryCodeList = parseRecoveryCodeList();
            var dt = new Date();
            var options = {
                month: 'long',
                day: 'numeric',
                year: 'numeric',
                hour: 'numeric',
                minute: 'numeric',
                timeZoneName: 'short'
            };

            return fileBodyContent =
                "${msg("recovery-codes-download-file-header")}\n\n" +
                recoveryCodeList + "\n" +
                "${msg("recovery-codes-download-file-description")}\n\n" +
                "${msg("recovery-codes-download-file-date")} " + formatCurrentDateTime();
        }

        function setUpDownloadLinkAndDownload(filename, text) {
            var el = document.createElement('a');
            el.setAttribute('href', 'data:text/plain;charset=utf-8,' + encodeURIComponent(text));
            el.setAttribute('download', filename);
            el.style.display = 'none';
            document.body.appendChild(el);
            el.click();
            document.body.removeChild(el);
        }

        function downloadRecoveryCodes() {
            setUpDownloadLinkAndDownload('kc-download-recovery-codes.txt', buildDownloadContent());
        }

        var downloadButton = document.getElementById("downloadRecoveryCodes");
        downloadButton && downloadButton.addEventListener("click", downloadRecoveryCodes);

        /* print recovery codes */
        function buildPrintContent() {
            var recoveryCodeListHTML = document.getElementById('kc-recovery-codes-list').innerHTML;
            var styles =
                `@page { size: auto;  margin-top: 0; }
                body { width: 480px; }
                div { list-style-type: none; font-family: monospace }
                p:first-of-type { margin-top: 48px }`

            return printFileContent =
                "<html><style>" + styles + "</style><body>" +
                "<title>kc-download-recovery-codes</title>" +
                "<p>${msg("recovery-codes-download-file-header")}</p>" +
                "<div>" + recoveryCodeListHTML + "</div>" +
                "<p>${msg("recovery-codes-download-file-description")}</p>" +
                "<p>${msg("recovery-codes-download-file-date")} " + formatCurrentDateTime() + "</p>" +
                "</body></html>";
        }

        function printRecoveryCodes() {
            var w = window.open();
            w.document.write(buildPrintContent());
            w.print();
            w.close();
        }

        var printButton = document.getElementById("printRecoveryCodes");
        printButton && printButton.addEventListener("click", printRecoveryCodes);
    </script>
</#if>
</@layout.registrationLayout>
