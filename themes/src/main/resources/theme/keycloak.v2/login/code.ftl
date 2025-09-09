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
                <@field.clipboard name="code" label="" ariaLabel=msg("codeSuccess") value=code.code />
                <script type="module">
                    (() => {
                        const copyButton = document.getElementById('kc-code-copy-button');
                        const toggleButton = document.getElementById('kc-code-toggle');
                        const input = document.getElementById('kc-code');
                        const expandableContent = document.getElementById('kc-code-content');
                        const clipboardContainer = document.getElementById('kc-code-clipboard');

                        // Validate elements
                        if (!copyButton || !toggleButton || !input || !expandableContent || !clipboardContainer) {
                            console.error("Missing required DOM elements for code interactions.");
                            return;
                        }

                        // Validate Clipboard API
                        if (!navigator.clipboard) {
                            console.error("Clipboard API not supported in this browser.");
                            copyButton.style.display = 'none';
                            return;
                        }
                        
                        // Handle copy functionality
                        copyButton.addEventListener('click', async() => {
                            const originalIcon = copyButton.innerHTML;

                            // Get value from expandable content
                            let value = expandableContent.querySelector('code')?.textContent;

                            try {
                                await navigator.clipboard.writeText(value);
                                updateCopyButton(true, copyButton, originalIcon);
                            } catch (err) {
                                console.error('Copy failed: ', err);
                                updateCopyButton(false, copyButton, originalIcon);
                            }
                        });
                        
                        // Handle toggle functionality
                        toggleButton.addEventListener('click', () => {
                            const newExpanded = !(toggleButton.getAttribute('aria-expanded') === 'true');
                            
                            toggleButton.setAttribute('aria-expanded', newExpanded.toString());
                            expandableContent.hidden = !newExpanded;
                            
                            // Update icon
                            const icon = toggleButton.querySelector('#kc-code-toggle-icon');
                            if (icon) {
                                icon.className = newExpanded ? toggleButton.dataset.iconExpandedClass : toggleButton.dataset.iconCollapsedClass;
                            }
                            
                            // Update clipboard container class
                            if (newExpanded) {
                                clipboardContainer.classList.add(toggleButton.dataset.classExpanded);
                            } else {
                                clipboardContainer.classList.remove(toggleButton.dataset.classExpanded);
                            }
                        });
                    })();
                    
                    // Update copy button to show success or failure state
                    function updateCopyButton(success, button, originalIcon) {
                        button.setAttribute('aria-label', success ? button.dataset.successLabel : button.dataset.failureLabel);
                        const icon = button.querySelector('#kc-code-copy-icon');
                        if (icon) {
                            icon.className = success ? button.dataset.iconSuccess : button.dataset.iconFailure;
                        }
                        button.disabled = true;
                        
                        // Reset button after 2 seconds
                        setTimeout(() => {
                            button.innerHTML = originalIcon;
                            button.setAttribute('aria-label', '${msg("code-copy-label")}');
                            button.disabled = false;
                        }, 2000);
                    }
                </script>
            <#else>
                <p id="error">${kcSanitize(code.error)}</p>
            </#if>
        </div>
    </#if>
</@layout.registrationLayout>