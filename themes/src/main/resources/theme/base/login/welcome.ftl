<#import "template.ftl" as layout>
<#import "field.ftl" as field>
<#import "buttons.ftl" as buttons>
<#assign headerDescription="">
<#if localUser && !successMessage?has_content>
    <#assign headerDescription=msg("welcomeAdminDescription", productName)>
<#elseif !localUser>
    <#assign headerDescription=msg("welcomeLocalAccessDescription")>
</#if>
<@layout.pageLayout bodyPageId="welcome" brandName=productNameHtml pageTitle=msg("welcomeTitle", productName) headerDescription=headerDescription
    displayMain=adminConsoleEnabled && (bootstrap || successMessage?has_content); section>
    <#if section = "header">
        <#if localUser>
            ${msg("welcomeAdminTitle")}
        <#else>
            ${msg("welcomeLocalAccessTitle")}
        </#if>
    <#elseif section = "form">
        <#if successMessage?has_content>
            <@buttons.buttonLink id="kc-open-admin-console" href=adminUrl label="welcomeOpenAdminConsole" class=["kcButtonPrimaryClass", "kcButtonBlockClass"] />
        </#if>
        <#if bootstrap>
            <#if localUser>
                <form id="kc-welcome-form" class="${properties.kcFormClass!}" method="post" novalidate>
                    <@field.input name="username" label=msg("username") required=true autocomplete="username" autofocus=true />
                    <@field.password name="password" label=msg("password") required=true autocomplete="new-password" />
                    <@field.password name="passwordConfirmation" label=msg("passwordConfirm") required=true autocomplete="new-password" />
                    <@field.input name="email" label=msg("email") type="email" autocomplete="email" />
                    <@field.input name="firstName" label=msg("firstName") autocomplete="given-name" />
                    <@field.input name="lastName" label=msg("lastName") autocomplete="family-name" />
                    <input name="stateChecker" type="hidden" value="${stateChecker}">
                    <@buttons.actionGroup>
                        <@buttons.button id="kc-create-user" label="welcomeCreateUser" />
                    </@buttons.actionGroup>
                </form>
            <#else>
                <p>${msg("welcomeRemoteInstructions")}</p>
            </#if>
        </#if>
    </#if>
</@layout.pageLayout>
