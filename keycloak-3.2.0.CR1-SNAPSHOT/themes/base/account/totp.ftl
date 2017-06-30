<#import "template.ftl" as layout>
<@layout.mainLayout active='totp' bodyClass='totp'; section>

    <#if totp.enabled>
<h2>${msg("authenticatorTitle")}</h2>

<table class="table table-bordered table-striped">
    <thead
        <tr>
            <th colspan="2">${msg("configureAuthenticators")}</th>
            </tr>
        </thead>
    <tbody>
        <tr>
            <td class="provider">${msg("mobile")}</td>
            <td class="action">
                <a id="remove-mobile" href="${url.totpRemoveUrl}"><i class="pficon pficon-delete"></i></a>
                </td>
            </tr>
        </tbody>
    </table>
    <#else>
<h2>${msg("authenticatorTitle")}</h2>

<hr/>

<ol>
    <li>
        <p>${msg("totpStep1")}</p>
    </li>
    <li>
        <p>${msg("totpStep2")}</p>
        <p><img src="data:image/png;base64, ${totp.totpSecretQrCode}" alt="Figure: Barcode"></p>
        <p><span class="code">${totp.totpSecretEncoded}</span></p>
    </li>
    <li>
        <p>${msg("totpStep3")}</p>
    </li>
</ol>

<hr/>

<form action="${url.totpUrl}" class="form-horizontal" method="post">
    <input type="hidden" id="stateChecker" name="stateChecker" value="${stateChecker?html}">
    <div class="form-group">
        <div class="col-sm-2 col-md-2">
            <label for="totp" class="control-label">${msg("authenticatorCode")}</label>
            </div>

        <div class="col-sm-10 col-md-10">
            <input type="text" class="form-control" id="totp" name="totp" autocomplete="off" autofocus autocomplete="off">
            <input type="hidden" id="totpSecret" name="totpSecret" value="${totp.totpSecret}" />
            </div>
        </div>

    <div class="form-group">
        <div id="kc-form-buttons" class="col-md-offset-2 col-md-10 submit">
            <div class="">
                <button type="submit" class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!}" name="submitAction" value="Save">${msg("doSave")}</button>
                <button type="submit" class="${properties.kcButtonClass!} ${properties.kcButtonDefaultClass!} ${properties.kcButtonLargeClass!}" name="submitAction" value="Cancel">${msg("doCancel")}</button>
                </div>
            </div>
        </div>
    </form>
    </#if>

</@layout.mainLayout>
