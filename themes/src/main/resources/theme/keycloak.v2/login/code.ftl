<#import "template.ftl" as layout>
<#import "field.ftl" as field>
<@layout.registrationLayout; section>
    <#if section = "header">
        <#if code.success>
            ${msg("codeSuccessTitle")}
        <#else>
            ${kcSanitize(msg("codeErrorTitle", code.error))}
        </#if>
    <#elseif section = "form">
        <div id="kc-code">
            <#if code.success>
                <p>${msg("copyCodeInstruction")}</p>
                <div class="${properties.kcFormGroupClass}">
                    <div class="${properties.kcInputGroup}">
                        <div class="${properties.kcInputGroupItemClass} ${properties.kcFill}">
                            <span class="${properties.kcInputClass}">
                                <input id="code" name="code" value="${code.code}" type="text" readonly aria-label="${msg("copyCodeInstruction")}"/>
                            </span>
                        </div>
                        <div class="${properties.kcInputGroupItemClass}">
                            <button class="${properties.kcButtonClass} ${properties.kcButtonSecondaryClass} pf-v5-u-ml-sm" type="button" 
                                    aria-label="${msg("recovery-codes-copy")}" 
                                    onclick="copyCodeToClipboard()" id="copy-code-button">
                                <i class="fas fa-copy" aria-hidden="true"></i>
                                <span class="pf-v5-u-ml-sm">${msg("recovery-codes-copy")}</span>
                            </button>
                        </div>
                    </div>
                </div>
            <#else>
                <p id="error">${kcSanitize(code.error)}</p>
            </#if>
        </div>
        
        <#if code.success>
        <script>
            function copyCodeToClipboard() {
                const codeInput = document.getElementById("code");
                const copyButton = document.getElementById("copy-code-button");
                const originalButtonContent = copyButton.innerHTML;
                
                // Select and copy the code
                codeInput.select();
                codeInput.setSelectionRange(0, 99999); // For mobile devices
                
                try {
                    // Try modern clipboard API first
                    if (navigator.clipboard && window.isSecureContext) {
                        navigator.clipboard.writeText(codeInput.value).then(() => {
                            showCopySuccess(copyButton, originalButtonContent);
                        }).catch(() => {
                            // Fallback to execCommand
                            fallbackCopy(codeInput, copyButton, originalButtonContent);
                        });
                    } else {
                        // Fallback to execCommand
                        fallbackCopy(codeInput, copyButton, originalButtonContent);
                    }
                } catch (err) {
                    console.error('Failed to copy code: ', err);
                }
            }
            
            function fallbackCopy(codeInput, copyButton, originalButtonContent) {
                try {
                    document.execCommand('copy');
                    showCopySuccess(copyButton, originalButtonContent);
                } catch (err) {
                    console.error('Fallback copy failed: ', err);
                }
            }
            
            function showCopySuccess(copyButton, originalButtonContent) {
                // Show success feedback
                copyButton.innerHTML = '<i class="fas fa-check" aria-hidden="true"></i><span class="pf-v5-u-ml-sm">Copied!</span>';
                copyButton.disabled = true;
                
                // Reset button after 2 seconds
                setTimeout(() => {
                    copyButton.innerHTML = originalButtonContent;
                    copyButton.disabled = false;
                }, 2000);
            }
        </script>
        </#if>
    </#if>
</@layout.registrationLayout>
