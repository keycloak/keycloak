<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "header">
        ${msg("sessionLimitReachedTitle")}
    <#elseif section = "form">
        <div id="kc-session-limit-confirmation" class="content-area">
            <p class="instruction">${msg("sessionLimitConfirmationHeader")}</p>

            <form class="form-actions" action="${url.loginAction}" method="POST">
                <div class="${properties.kcFormGroupClass!}">
                    <div id="kc-form-buttons" class="${properties.kcFormGroupClass!}">
                        <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}"
                               name="terminateOldest" id="kc-terminate-oldest" type="submit" value="${msg("sessionLimitTerminateOldest")}"/>
                        <input class="${properties.kcButtonClass!} ${properties.kcButtonDefaultClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}"
                               name="cancel" id="kc-cancel" type="submit" value="${msg("doCancel")}"/>
                    </div>
                </div>
            </form>
        </div>
    </#if>
</@layout.registrationLayout>
