<#import "template.ftl" as layout>
<#import "buttons.ftl" as buttons>

<@layout.registrationLayout; section>
<!-- template: session-limit-confirmation.ftl -->

    <#if section = "header">
      ${msg("sessionLimitReachedTitle")}

   <#elseif section = "form">

    <form action="${url.loginAction}" class="${properties.kcFormClass!}" id="kc-session-limit-form" method="post">
      <p class="instruction">${msg("sessionLimitConfirmationHeader")}</p>

      <@buttons.actionGroup>
        <@buttons.button id="kc-terminate-oldest" name="terminateOldest" label="sessionLimitTerminateOldest"/>
        <@buttons.button id="kc-cancel" name="cancel" label="doCancel" type="secondary"/>
      </@buttons.actionGroup>
    </form>
   </#if>

</@layout.registrationLayout>
