<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=false; section>
    <#if section = "title">
    ${msg("termsTitle")}
    <#elseif section = "header">
    ${msg("termsTitleHtml")}
    <#elseif section = "form">
    <div id="kc-terms-text">
        ${msg("termsText")}
    </div>
    <form class="form-actions" action="${requiredActionUrl("terms_and_conditions", "")}" method="POST">
        <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!}" name="accept" id="kc-login" type="submit" value="${msg("doAccept")}"/>
        <input class="${properties.kcButtonClass!} ${properties.kcButtonDefaultClass!} ${properties.kcButtonLargeClass!}" name="cancel" id="kc-cancel" type="submit" value="${msg("doDecline")}"/>
    </form>
    </#if>
</@layout.registrationLayout>