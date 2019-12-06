<#macro registrationLayout bodyClass="" displayInfo=false displayMessage=true displayWide=false>
<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "header">
        <script type="text/javascript">
            // Fill up the two hidden and submit the form
            function fillAndSubmit() {
                var selectValue = document.getElementById('authenticators-choice').value;
                if (selectValue != '') {
                    var split = selectValue.split("|");
                    document.getElementById('authexec-hidden-input').value = split[0];
                    document.getElementById('credentialId-hidden-input').value = split[1];
                    document.getElementById('kc-select-credential-form').submit();
                }
            }
            <#if auth.authenticationSelections?size gt 1>
                // We bind the action to the select
                window.addEventListener('load', function() {
                    document.getElementById('authenticators-choice').addEventListener('change', fillAndSubmit);
                });
            </#if>
        </script>
        <#nested "header">
    <#elseif section = "form">
        <#if auth.authenticationSelections?size gt 1>
            <form id="kc-select-credential-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
                <div class="${properties.kcFormGroupClass!}">
                    <div class="${properties.kcLabelWrapperClass!}">
                        <label for="authenticators-choice" class="${properties.kcLabelClass!}">${msg("loginCredential")}</label>
                    </div>
                    <div class="${properties.kcInputWrapperClass!}">
                        <select id="authenticators-choice" class="form-control" size="1">
                            <#list auth.authenticationSelections as authenticationSelection>
                                <#if authenticationSelection.credentialId?has_content>
                                    <option value="${authenticationSelection.id}" <#if auth.selectedCredential?has_content && authenticationSelection.credentialId == auth.selectedCredential>selected</#if>><#if authenticationSelection.showCredentialType()>${msg('${authenticationSelection.authExecName}')}</#if>${authenticationSelection.credentialName}</option>
                                <#else >
                                    <option value="${authenticationSelection.id}" <#if authenticationSelection.authExecId == execution>selected</#if>>${msg('${authenticationSelection.authExecName}')}</option>
                                </#if>
                            </#list>
                        </select>
                        <input type="hidden" id="authexec-hidden-input" name="authenticationExecution" />
                        <input type="hidden" id="credentialId-hidden-input" name="credentialId" <#if auth.selectedCredential?has_content>value="${auth.selectedCredential}"</#if>/>
                    </div>
                </div>
            </form>
        </#if>
        <#nested "form">
    </#if>
</@layout.registrationLayout>
</#macro>
