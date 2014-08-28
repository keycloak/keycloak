<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "title">
        ${rb.loginTitle} ${realm.name}
    <#elseif section = "header">
        ${rb.loginTitle} <strong>${realm.name}</strong>
    <#elseif section = "form">
        <form id="kc-totp-login-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
            <input id="username" name="username" value="${login.username!''}" type="hidden" />
            <input id="password-token" name="password-token" value="${login.passwordToken!''}" type="hidden" />

            <div class="${properties.kcFormGroupClass!}">
                <div class="${properties.kcLabelWrapperClass!}">
                    <label for="totp" class="${properties.kcLabelClass!}">${rb.authenticatorCode}</label>
                </div>

                <div class="${properties.kcInputWrapperClass!}">
                    <input id="totp" name="totp" type="text" class="${properties.kcInputClass!}" />
                </div>
            </div>

            <div class="${properties.kcFormGroupClass!}">
                <div id="kc-form-options" class="${properties.kcFormOptionsClass!}">
                    <div class="${properties.kcFormOptionsWrapperClass!}">
                    </div>
                </div>

                <div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
                    <div class="${properties.kcFormButtonsWrapperClass!}">
                        <input class="btn btn-primary btn-lg" name="login" id="kc-login" type="submit" value="${rb.logIn}"/>
                        <input class="btn btn-default btn-lg" name="cancel" id="kc-cancel" type="submit" value="${rb.cancel}"/>
                    </div>
                </div>
            </div>
        </form>
    </#if>
</@layout.registrationLayout>