<#-- TODO: Only a placeholder, implementation needed -->
<#import "template.ftl" as layout>
<@layout.registrationLayout bodyClass="oauth"; section>
    <#if section = "title">
    ${rb.oauthGrantTitle}

    <#elseif section = "header">
    ${rb.oauthGrantTitleHtml}

    <#elseif section = "form">
    <div id="kc-oauth" class="content-area">
        <p class="instruction"><strong>${oauth.client}</strong> ${rb.oauthGrantRequest}</p>
        <ul id="kc-oauth-list">
            <#list oauth.realmRolesRequested as role>
                <li>
                    <span><#if role.description??>${role.description}<#else>${role.name}</#if></span>
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

        <p class="terms">${rb.oauthGrantTerms}</p>
        <form class="form-actions" action="${url.oauthAction}" method="POST">
            <input type="hidden" name="code" value="${oauth.code}">
            <input type="submit" class="btn-primary primary" name="accept" value="${rb.accept}">
            <input type="submit" class="btn-secondary" name="cancel" value="${rb.cancel}">
        </form>
    </div>
    </#if>
</@layout.registrationLayout>