<#import "template.ftl" as layout>
<#import "buttons.ftl" as buttons>
<@layout.registrationLayout; section>
    <#if section = "header">
        ${msg("doLogIn")}
    <#elseif section = "form">

        <form id="kc-x509-login-info" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
            <div class="${properties.kcFormGroupClass!}">

                <div class="${properties.kcLabelWrapperClass!}">
                    <label for="certificate_subjectDN" class="${properties.kcLabelClass!}">${msg("clientCertificate")}</label>
                </div>
                <#if x509.formData.subjectDN??>
                    <div class="${properties.kcLabelWrapperClass!}">
                         <label id="certificate_subjectDN" class="${properties.kcLabelClass!}">${(x509.formData.subjectDN!"")}</label>
                    </div>
                <#else>
                    <div class="${properties.kcLabelWrapperClass!}">
                        <label id="certificate_subjectDN" class="${properties.kcLabelClass!}">${msg("noCertificate")}</label>
                    </div>
                </#if>
           </div>

            <div class="${properties.kcFormGroupClass!}">

                    <#if x509.formData.isUserEnabled??>
                          <div class="${properties.kcLabelWrapperClass!}">
                             <label for="username" class="${properties.kcLabelClass!}">${msg("doX509Login")}</label>
                          </div>
                          <div class="${properties.kcLabelWrapperClass!}">
                             <label id="username" class="${properties.kcLabelClass!}">${(x509.formData.username!'')}</label>
                         </div>
                    </#if>

            </div>

            <div class="${properties.kcFormGroupClass!}">
                <div id="kc-form-options" class="${properties.kcFormOptionsClass!}">
                    <div class="${properties.kcFormOptionsWrapperClass!}">
                    </div>
                </div>

                <@buttons.actionGroup horizontal=true id="kc-form-buttons" group=false>
                    <@buttons.button name="login" id="kc-login" label="doContinue" fullWidth=false class=["kcButtonLargeClass"] value=msg("doContinue") />
                    <#if x509.formData.isUserEnabled??>
                        <@buttons.button name="cancel" id="kc-cancel" label="doIgnore" type="secondary" fullWidth=false class=["kcButtonLargeClass"] value=msg("doIgnore") />
                    </#if>
                </@buttons.actionGroup>
            </div>
        </form>
    </#if>

</@layout.registrationLayout>
