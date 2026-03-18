<#import "template.ftl" as layout>
<#import "buttons.ftl" as buttons>

<@layout.registrationLayout; section>
<!-- template: delete-account-confirm.ftl -->

    <#if section = "header">
      ${msg("deleteAccountConfirm")}

   <#elseif section = "form">

    <form action="${url.loginAction}" class="${properties.kcFormClass!}" id="kc-deleteaccount-form" method="post">

      <div class="${properties.kcAlertClass!} pf-m-warning">
        <div class="${properties.kcAlertIconClass!}">
          <i class="${properties.kcFeedbackWarningIcon!}" aria-hidden="true"></i>
        </div>
        <span class="${properties.kcAlertTitleClass!}">
          ${msg("irreversibleAction")}
        </span>
      </div>

      <p>${msg("deletingImplies")}</p>
      <ul class="pf-v5-c-list" role="list">
        <li>${msg("loggingOutImmediately")}</li>
        <li>${msg("errasingData")}</li>
      </ul>

      <p class="delete-account-text">${msg("finalDeletionConfirmation")}</p>

      <@buttons.actionGroup>
        <@buttons.button id="kc-submit" label="doConfirmDelete" class=["kcButtonPrimaryClass"]/>
        <#if triggered_from_aia>
          <@buttons.button id="kc-cancel" name="cancel-aia" label="doCancel" class=["kcButtonSecondaryClass"]/>
        </#if>
      </@buttons.actionGroup>
    </form>
   </#if>

</@layout.registrationLayout>