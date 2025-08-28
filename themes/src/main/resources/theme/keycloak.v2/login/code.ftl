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
                    <div id="kc-code-input" class="${properties.kcInputClass}"
                            aria-label="${msg("copyCodeInstruction")}"
                            tabindex="0">${code.code}
                    </div>
                </div>
                <div class="${properties.kcFormGroupClass}">
                    <button class="${properties.kcButtonSecondaryClass} ${properties.kcButtonBlockClass}" type="button" 
                            aria-label="${msg("recovery-codes-copy")}" 
                            onclick="copyCodeToClipboard()" id="copy-code-button">
                        <i class="fas fa-copy" aria-hidden="true"></i>
                        <span class="pf-v5-u-ml-sm">${msg("recovery-codes-copy")}</span>
                    </button>
                </div>
            <#else>
                <p id="error">${kcSanitize(code.error)}</p>
            </#if>
        </div>
        
        <#if code.success>
        <script>
            function copyCodeToClipboard() {
                const codeDiv = document.getElementById("code");
                const copyButton = document.getElementById("copy-code-button");
                const originalButtonContent = copyButton.innerHTML;
                const codeText = codeDiv.textContent || codeDiv.innerText;
                
                try {
                    // Try modern clipboard API first
                    if (navigator.clipboard && window.isSecureContext) {
                        navigator.clipboard.writeText(codeText).then(() => {
                            showCopySuccess(copyButton, originalButtonContent);
                        }).catch(() => {
                            // Fallback to text selection and execCommand
                            fallbackCopy(codeDiv, copyButton, originalButtonContent);
                        });
                    } else {
                        // Fallback to text selection and execCommand
                        fallbackCopy(codeDiv, copyButton, originalButtonContent);
                    }
                } catch (err) {
                    console.error('Failed to copy code: ', err);
                }
            }
            
            function fallbackCopy(codeDiv, copyButton, originalButtonContent) {
                try {
                    // Create a temporary textarea to select text
                    const textArea = document.createElement("textarea");
                    textArea.value = codeDiv.textContent || codeDiv.innerText;
                    document.body.appendChild(textArea);
                    textArea.select();
                    textArea.setSelectionRange(0, 99999);
                    document.execCommand('copy');
                    document.body.removeChild(textArea);
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
