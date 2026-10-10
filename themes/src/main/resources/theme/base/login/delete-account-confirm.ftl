<#import "template.ftl" as layout>
<#import "buttons.ftl" as buttons>
<@layout.registrationLayout; section>

    <#if section = "header">
            ${msg("deleteAccountConfirm")}

   <#elseif section = "form">

    <form action="${url.loginAction}" class="form-vertical" method="post">

       <div class="alert alert-warning kc-alert-delete-account">
           <span class="pficon pficon-warning-triangle-o"></span>
           ${msg("irreversibleAction")}
       </div>

       <p>${msg("deletingImplies")}</p>
       <ul class="kc-delete-account-list">
         <li>${msg("loggingOutImmediately")}</li>
         <li>${msg("errasingData")}</li>
       </ul>

        <p class="delete-account-text">${msg("finalDeletionConfirmation")}</p>

      <div id="kc-form-buttons">
            <@buttons.button label="doConfirmDelete" fullWidth=false class=["kcButtonLargeClass"] value=msg("doConfirmDelete") />
            <#if triggered_from_aia>
            <@buttons.button name="cancel-aia" label="doCancel" type="secondary" fullWidth=false class=["kcButtonLargeClass"] className="kc-delete-account-cancel" value="true" />
            </#if>
       </div>
    </form>
   </#if>

</@layout.registrationLayout>
