<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=true; section>
    <#if section = "header" || section = "show-username">
        <script type="text/javascript">
            // Fill up the two hidden and submit the form
            function fillAndSubmit() {
                document.getElementById('authexec-hidden-input').value = document.getElementById('authenticators-choice').value;
                document.getElementById('kc-select-credential-form').submit();
            }
            <#if auth.authenticationSelections?size gt 1>
                // We bind the action to the select
                window.addEventListener('load', function() {
                    document.getElementById('authenticators-choice').addEventListener('change', fillAndSubmit);
                });
            </#if>
        </script>
    <#elseif section = "form">
        <form id="kc-select-credential-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
            <div class="${properties.kcFormGroupClass!}">
                <div class="${properties.kcLabelWrapperClass!}">
                    <label for="authenticators-choice" class="${properties.kcLabelClass!}">${msg("loginCredential")}</label>
                </div>
                <div class="${properties.kcInputWrapperClass!}">
                    <select id="authenticators-choice" class="form-control" size="1">
                        <#list auth.authenticationSelections as authenticationSelection>
                            <option value="${authenticationSelection.authExecId}" <#if authenticationSelection.authExecId == execution>selected</#if>>${msg('${authenticationSelection.authExecDisplayName}')}</option>
                        </#list>
                    </select>
                    <input type="hidden" id="authexec-hidden-input" name="authenticationExecution" />
                </div>
            </div>
        </form>
    </#if>
</@layout.registrationLayout>

