<#import "template.ftl" as layout>
<@layout.mainLayout active='deleteAccount' bodyClass='deleteAccount'; section>

    <div class="row">
        <div class="col-md-10">
            <h2>${msg("deleteAccount")}</h2>
        </div>
    </div>


    <form action="${url.deleteAccountUrl}" class="form-horizontal" method="post">

       <div class="alert alert-warning" style="margin-top:0 !important;margin-bottom:30px !important">
           <span class="pficon pficon-warning-triangle-o"></span>
           ${msg("irreversibleAction")}
       </div>

        <div class="row">
          <div class="col-md-10">
                <p>${msg("deletingImplies")}<p>
                <ul>
                  <li>${msg("loggingOutImmediately")}</li>
                  <li>${msg("errasingData")}</li>
                  <li>${msg("accountUnusable")}</li>
                </ul>
          </div>
        </div>

        <input type="text" id="username" name="username" value="${(account.username!'')}" autocomplete="username" readonly="readonly" style="display:none;">

        <input type="hidden" id="stateChecker" name="stateChecker" value="${stateChecker}">

        <div class="form-group">
            <div id="kc-form-buttons" class="col-md-offset-2 col-md-10 submit">
                <div class="">
                    <button id="delete" type="submit" class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!}" name="submitAction" value="delete">${msg("doDelete")}</button>
                </div>
            </div>
        </div>
    </form>

</@layout.mainLayout>