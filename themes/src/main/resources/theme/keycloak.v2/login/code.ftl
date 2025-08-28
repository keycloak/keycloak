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
                <@field.clipboard name="code" label="" value=code.code />
                
                <script>
                document.addEventListener('DOMContentLoaded', function() {
                    const copyButton = document.getElementById('code-copy-button');
                    const toggleButton = document.getElementById('code-toggle');
                    const input = document.getElementById('code');
                    const expandableContent = document.getElementById('code-content');
                    
                    // Only show copy button if Clipboard API is available
                    if (!navigator.clipboard) {
                        if (copyButton) {
                            copyButton.style.display = 'none';
                        }
                        return;
                    }
                    
                    // Handle copy functionality
                    if (copyButton && input) {
                        copyButton.addEventListener('click', function() {
                            const originalIcon = this.innerHTML;
                            const value = input.value;
                            
                            navigator.clipboard.writeText(value).then(function() {
                                showCopySuccess(copyButton, originalIcon);
                            }).catch(function(err) {
                                console.error('Copy failed: ', err);
                            });
                        });
                    }
                    
                    // Handle toggle functionality
                    if (toggleButton && expandableContent) {
                        toggleButton.addEventListener('click', function() {
                            const isExpanded = this.getAttribute('aria-expanded') === 'true';
                            const newExpanded = !isExpanded;
                            
                            this.setAttribute('aria-expanded', newExpanded.toString());
                            expandableContent.hidden = !newExpanded;
                            
                            // Update icon
                            const icon = this.querySelector('i');
                            if (icon) {
                                icon.className = newExpanded ? 'fas fa-angle-down' : 'fas fa-angle-right';
                            }
                            
                            // Update clipboard container class
                            const clipboardContainer = document.getElementById('code-clipboard');
                            if (clipboardContainer) {
                                if (newExpanded) {
                                    clipboardContainer.classList.add('pf-m-expanded');
                                } else {
                                    clipboardContainer.classList.remove('pf-m-expanded');
                                }
                            }
                        });
                    }
                    
                    function showCopySuccess(button, originalIcon) {
                        // Show success feedback with tooltip-like behavior
                        button.innerHTML = '<i class="fas fa-check" aria-hidden="true"></i>';
                        button.setAttribute('aria-label', '${msg("recovery-codes-copied")}');
                        button.disabled = true;
                        
                        // Reset button after 2 seconds
                        setTimeout(function() {
                            button.innerHTML = originalIcon;
                            button.setAttribute('aria-label', '${msg("recovery-codes-copy")}');
                            button.disabled = false;
                        }, 2000);
                    }
                });
                </script>
            <#else>
                <p id="error">${kcSanitize(code.error)}</p>
            </#if>
        </div>
    </#if>
</@layout.registrationLayout>