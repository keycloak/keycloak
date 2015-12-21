<#import "template.ftl" as layout>
<@layout.registrationLayout bodyClass="oauth"; section>
    <#if section = "title">
        ${msg("oauthGrantTitle")}
    <#elseif section = "header">
    ${msg("oauthGrantTitleHtml",(realm.displayNameHtml!''))} <strong><#if client.name??>${advancedMsg(client.name)}<#else>${client.clientId}</#if></strong>.
    <#elseif section = "form">
        <div id="kc-oauth" class="content-area">
            <h3>${msg("oauthGrantRequest")}</h3>
            <ul>
                <#if oauth.claimsRequested??>
                    <li>
                        <span>
                            ${msg("personalInfo")}&nbsp;
                            <#list oauth.claimsRequested as claim>
                                ${advancedMsg(claim)}<#if claim_has_next>,&nbsp;</#if>
                            </#list>
                        </span>
                    </li>
                </#if>
                <#if oauth.accessRequestMessage??>
                    <li>
                        <span>
                            ${oauth.accessRequestMessage}
                        </span>
                    </li>
                </#if>
                <#if oauth.realmRolesRequested??>
                    <#list oauth.realmRolesRequested as role>
                        <li>
                            <span><#if role.description??>${advancedMsg(role.description)}<#else>${advancedMsg(role.name)}</#if></span>
                        </li>
                    </#list>
                </#if>
                <#if oauth.resourceRolesRequested??>
                    <#list oauth.resourceRolesRequested?keys as resource>
                        <#list oauth.resourceRolesRequested[resource] as clientRole>
                            <li>
                                <span class="kc-role"><#if clientRole.roleDescription??>${advancedMsg(clientRole.roleDescription)}<#else>${advancedMsg(clientRole.roleName)}</#if></span>
                                <span class="kc-resource">${msg("inResource")} <strong><#if clientRole.clientName??>${advancedMsg(clientRole.clientName)}<#else>${clientRole.clientId}</#if></strong> </span>
                            </li>
                        </#list>
                    </#list>
                </#if>
            </ul>

            <form class="form-actions" action="${url.oauthAction}" method="POST">
                <input type="hidden" name="code" value="${oauth.code}">
                <div class="${properties.kcFormGroupClass!}">
                    <div id="kc-form-options" class="${properties.kcFormOptionsClass!}">
                        <div class="${properties.kcFormOptionsWrapperClass!}">
                        </div>
                    </div>

                    <div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
                        <div class="${properties.kcFormButtonsWrapperClass!}">
                            <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!}" name="accept" id="kc-login" type="submit" value="${msg("doYes")}"/>
                            <input class="${properties.kcButtonClass!} ${properties.kcButtonDefaultClass!} ${properties.kcButtonLargeClass!}" name="cancel" id="kc-cancel" type="submit" value="${msg("doNo")}"/>
                        </div>
                    </div>
                </div>
            </form>
        </div>
    </#if>
</@layout.registrationLayout>