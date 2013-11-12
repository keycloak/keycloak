<#-- TODO: Only a placeholder, implementation needed -->
<#import "template-login-action.ftl" as layout>
<@layout.registrationLayout bodyClass="reset oauth"; section>
    <#if section = "title">

    OAuth Grant

    <#elseif section = "header">

    <strong>Keycloak</strong> Central Login

    <#elseif section = "form">
    <div class="content-area">
        <p class="instruction"><strong>${oauth.client.loginName}</strong> requests access to:</p>
        <ul>
            <#list oauth.realmRolesRequested as role>
                <li>
                    <span>${role.description}</span>
                </li>
            </#list>

            <#list oauth.resourceRolesRequested?keys as resource>
                <#list oauth.resourceRolesRequested[resource] as role>
                    <li>
                        <span><#if role.description??>${role.description}<#else>${role.name}</#if></span>
                        <span class="parent">in <strong>${resource}</strong></span>
                    </li>
                </#list>
            </#list>
        </ul>

        <p class="terms">Keycloak Central Login and Google will use this information in accordance with their respective terms of service and privacy policies.</p>
        <form class="form-actions" action="${oauth.action}" method="POST">
            <input type="hidden" name="code" value="${oauth.oAuthCode}">
            <input type="submit" class="btn-primary primary" name="accept" value="Accept">
            <input type="submit" class="btn-secondary" name="cancel" value="Cancel">
        </form>
    </div>

    <#elseif section = "info" >

    <div id="info">
    </div>

    </#if>
</@layout.registrationLayout>