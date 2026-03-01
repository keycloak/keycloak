<#import "template.ftl" as layout>
<@layout.registrationLayout; section>

    <#if section = "header">
            ${msg("deleteAccountConfirm")}

   <#elseif section = "form">

    <form action="${url.loginAction}" class="form-vertical" method="post">

       <div class="alert alert-warning" style="margin-top:0 !important;margin-bottom:30px !important">
           <span class="pficon pficon-warning-triangle-o"></span>
           ${msg("irreversibleAction")}
       </div>

       <p>${msg("deletingImplies")}</p>
       <ul style="color: #72767b;list-style: disc;list-style-position: inside;">
         <li>${msg("loggingOutImmediately")}</li>
         <li>${msg("errasingData")}</li>
       </ul>

        <p class="delete-account-text">${msg("finalDeletionConfirmation")}</p>

      <div id="kc-form-buttons">
            <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!}" type="submit" value="${msg("doConfirmDelete")}" />
            <#if triggered_from_aia>
            <button class="${properties.kcButtonClass!} ${properties.kcButtonDefaultClass!} ${properties.kcButtonLargeClass!}" style="margin-left: calc(100% - 220px)" type="submit" name="cancel-aia" value="true">${msg("doCancel")}</button>
            </#if>
       </div>
    </form>
   </#if>

</@layout.registrationLayout>