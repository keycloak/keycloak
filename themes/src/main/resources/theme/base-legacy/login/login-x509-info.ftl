<#import "template.ftl" as layout>
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

                <div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
                    <div class="${properties.kcFormButtonsWrapperClass!}">
                        <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!}" name="login" id="kc-login" type="submit" value="${msg("doContinue")}"/>
                        <#if x509.formData.isUserEnabled??>
                            <input class="${properties.kcButtonClass!} ${properties.kcButtonDefaultClass!} ${properties.kcButtonLargeClass!}" name="cancel" id="kc-cancel" type="submit" value="${msg("doIgnore")}"/>
                        </#if>
                    </div>
                </div>
            </div>
        </form>
    </#if>

</@layout.registrationLayout>
