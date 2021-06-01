<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('username','email','firstName','lastName'); section>
    <#if section = "header">
        ${msg("loginProfileTitle")}
    <#elseif section = "form">
        <form id="kc-update-profile-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">

            <#list profile.attributes as attribute>
                <div class="${properties.kcFormGroupClass!}">
                    <div class="${properties.kcLabelWrapperClass!}">
                        <label for="${attribute.name}" class="${properties.kcLabelClass!}">${msg("${attribute.name}")}</label>
                        <#if attribute.required>*</#if>
                    </div>
                    <div class="${properties.kcInputWrapperClass!}">
                        <input type="text" id="${attribute.name}" name="${attribute.name}" value="${(attribute.value!'')}"
                               class="${properties.kcInputClass!}"
                               aria-invalid="<#if messagesPerField.existsError('${attribute.name}')>true</#if>"
                               <#if attribute.readOnly>disabled</#if>
                        />

                        <#if messagesPerField.existsError('${attribute.name}')>
                            <span id="input-error-${attribute.name}" class="${properties.kcInputErrorMessageClass!}" aria-live="polite">
                                ${kcSanitize(messagesPerField.get('${attribute.name}'))?no_esc}
                            </span>
                        </#if>
                    </div>
                </div>
            </#list>

            <div class="${properties.kcFormGroupClass!}">
                <div id="kc-form-options" class="${properties.kcFormOptionsClass!}">
                    <div class="${properties.kcFormOptionsWrapperClass!}">
                    </div>
                </div>

                <div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
                    <#if isAppInitiatedAction??>
                        <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!}" type="submit" value="${msg("doSubmit")}" />
                        <button class="${properties.kcButtonClass!} ${properties.kcButtonDefaultClass!} ${properties.kcButtonLargeClass!}" type="submit" name="cancel-aia" value="true" />${msg("doCancel")}</button>
                    <#else>
                        <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}" type="submit" value="${msg("doSubmit")}" />
                    </#if>
                </div>
            </div>
        </form>
    </#if>
</@layout.registrationLayout>