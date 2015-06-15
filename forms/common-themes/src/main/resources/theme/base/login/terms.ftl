<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=false; section>
    <#if section = "title">
    ${msg("termsTitle")}
    <#elseif section = "header">
    ${msg("termsTitleHtml")}
    <#elseif section = "form">
    <div id="kc-info-message">
        <textarea class="${properties.kcTextareaClass!}" rows="20" cols="120">
        Apache License
        Version 2.0, January 2004
        http://www.apache.org/licenses/

        TERMS AND CONDITIONS FOR USE, REPRODUCTION, AND DISTRIBUTION

        </textarea>
        <form class="form-actions" action="${requiredActionUrl("terms_and_conditions", "")}" method="POST">
            <div class="${properties.kcFormGroupClass!}">
                 <div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
                    <div class="${properties.kcFormButtonsWrapperClass!}">
                        <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!}" name="accept" id="kc-login" type="submit" value="${msg("doAccept")}"/>
                        <input class="${properties.kcButtonClass!} ${properties.kcButtonDefaultClass!} ${properties.kcButtonLargeClass!}" name="cancel" id="kc-cancel" type="submit" value="${msg("doDecline")}"/>
                    </div>
                </div>
            </div>
        </form>
    </div>
    </#if>
</@layout.registrationLayout>