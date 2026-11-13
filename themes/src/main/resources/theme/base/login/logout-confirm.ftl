<#import "template.ftl" as layout>
<#import "buttons.ftl" as buttons>
<@layout.registrationLayout; section>
    <#if section = "header">
        ${msg("logoutConfirmTitle")}
    <#elseif section = "form">
        <div id="kc-logout-confirm" class="content-area">
            <form class="form-actions ${properties.kcFormClass!}" action="${url.logoutConfirmAction}" onsubmit="confirmLogout.disabled = true; return true;" method="POST">
                <p class="instruction">${msg("logoutConfirmHeader")}</p>
                <input type="hidden" name="session_code" value="${logoutConfirm.code}">
                <@buttons.actionGroup id="kc-form-buttons">
                    <@buttons.button name="confirmLogout" id="kc-logout" label="doLogout" class=["kcButtonLargeClass"] value=msg("doLogout") tabindex="4" />
                </@buttons.actionGroup>
            </form>

            <div id="kc-info-message">
                <#if logoutConfirm.skipLink>
                <#else>
                    <#if (client.baseUrl)?has_content>
                        <p><@buttons.buttonLink id="backToApplication" href=client.baseUrl label="backToApplication"/></p>
                    </#if>
                </#if>
            </div>

            <div class="clearfix"></div>
        </div>
    </#if>
</@layout.registrationLayout>
