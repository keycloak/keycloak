<#import "template.ftl" as layout>
<@layout.mainLayout active='totp' bodyClass='totp'; section>

    <div class="row">
        <div class="col-md-10">
            <h2>${msg("authenticatorTitle")}</h2>
        </div>
        <#if totp.otpCredentials?size == 0>
            <div class="col-md-2 subtitle">
                <span class="subtitle"><span class="required">*</span> ${msg("requiredFields")}</span>
            </div>
        </#if>
    </div>

    <#if totp.enabled>
        <table class="table table-bordered table-striped">
            <thead>
            <#if totp.otpCredentials?size gt 1>
                <tr>
                    <th colspan="4">${msg("configureAuthenticators")}</th>
                </tr>
            <#else>
                <tr>
                    <th colspan="3">${msg("configureAuthenticators")}</th>
                </tr>
            </#if>
            </thead>
            <tbody>
            <#list totp.otpCredentials as credential>
                <tr>
                    <td class="provider">${msg("mobile")}</td>
                    <#if totp.otpCredentials?size gt 1>
                        <td class="provider">${credential.id}</td>
                    </#if>
                    <td class="provider">${credential.userLabel!}</td>
                    <td class="action">
                        <form action="${url.totpUrl}" method="post" class="form-inline">
                            <input type="hidden" id="stateChecker" name="stateChecker" value="${stateChecker}">
                            <input type="hidden" id="submitAction" name="submitAction" value="Delete">
                            <input type="hidden" id="credentialId" name="credentialId" value="${credential.id}">
                            <button id="remove-mobile" class="btn btn-default">
                                <i class="pficon pficon-delete"></i>
                            </button>
                        </form>
                    </td>
                </tr>
            </#list>
            </tbody>
        </table>
        <#else>

    <hr/>

    <ol>
        <li>
            <p>${msg("totpStep1")}</p>

            <ul>
                <#list totp.policy.supportedApplications as app>
                    <li>${app}</li>
                </#list>
            </ul>
        </li>

        <#if mode?? && mode = "manual">
            <li>
                <p>${msg("totpManualStep2")}</p>
                <p><span id="kc-totp-secret-key">${totp.totpSecretEncoded}</span></p>
                <p><a href="${totp.qrUrl}" id="mode-barcode">${msg("totpScanBarcode")}</a></p>
            </li>
            <li>
                <p>${msg("totpManualStep3")}</p>
                <ul>
                    <li id="kc-totp-type">${msg("totpType")}: ${msg("totp." + totp.policy.type)}</li>
                    <li id="kc-totp-algorithm">${msg("totpAlgorithm")}: ${totp.policy.getAlgorithmKey()}</li>
                    <li id="kc-totp-digits">${msg("totpDigits")}: ${totp.policy.digits}</li>
                    <#if totp.policy.type = "totp">
                        <li id="kc-totp-period">${msg("totpInterval")}: ${totp.policy.period}</li>
                    <#elseif totp.policy.type = "hotp">
                        <li id="kc-totp-counter">${msg("totpCounter")}: ${totp.policy.initialCounter}</li>
                    </#if>
                </ul>
            </li>
        <#else>
            <li>
                <p>${msg("totpStep2")}</p>
                <p><img src="data:image/png;base64, ${totp.totpSecretQrCode}" alt="Figure: Barcode"></p>
                <p><a href="${totp.manualUrl}" id="mode-manual">${msg("totpUnableToScan")}</a></p>
            </li>
        </#if>
        <li>
            <p>${msg("totpStep3")}</p>
            <p>${msg("totpStep3DeviceName")}</p>
        </li>
    </ol>

    <hr/>

    <form action="${url.totpUrl}" class="form-horizontal" method="post">
        <input type="hidden" id="stateChecker" name="stateChecker" value="${stateChecker}">
        <div class="form-group">
            <div class="col-sm-2 col-md-2">
                <label for="totp" class="control-label">${msg("authenticatorCode")}</label> <span class="required">*</span>
            </div>

            <div class="col-sm-10 col-md-10">
                <input type="text" class="form-control" id="totp" name="totp" autocomplete="off" autofocus>
                <input type="hidden" id="totpSecret" name="totpSecret" value="${totp.totpSecret}"/>
            </div>


        </div>

        <div class="form-group" ${messagesPerField.printIfExists('userLabel',properties.kcFormGroupErrorClass!)}">
            <div class="col-sm-2 col-md-2">
                <label for="userLabel" class="control-label">${msg("totpDeviceName")}</label> <#if totp.otpCredentials?size gte 1><span class="required">*</span></#if>
            </div>

            <div class="col-sm-10 col-md-10">
                <input type="text" class="form-control" id="userLabel" name="userLabel" autocomplete="off">
            </div>
        </div>

        <div class="form-group">
            <div id="kc-form-buttons" class="col-md-offset-2 col-md-10 submit">
                <div class="">
                    <button type="submit"
                            class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!}"
                            id="saveTOTPBtn" name="submitAction" value="Save">${msg("doSave")}
                    </button>
                    <button type="submit"
                            class="${properties.kcButtonClass!} ${properties.kcButtonDefaultClass!} ${properties.kcButtonLargeClass!}"
                            id="cancelTOTPBtn" name="submitAction" value="Cancel">${msg("doCancel")}
                    </button>
                </div>
            </div>
        </div>
    </form>
    </#if>

</@layout.mainLayout>
