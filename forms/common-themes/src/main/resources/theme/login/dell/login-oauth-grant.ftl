<#-- TODO: Only a placeholder, implementation needed -->
<#import "template.ftl" as layout>
<@layout.registrationLayout bodyClass="oauth"; section>
    <#if section = "title">
        ${rb.oauthGrantTitle}
    <#elseif section = "header">
        Temporary access for <strong>${(realm.name)!''}</strong> requested by <strong>${(client.clientId)!''}</strong>.
    <#elseif section = "form">
        <div id="kc-oauth" class="content-area">
            <h3>${rb.oauthGrantRequest}</h3>
            <ul>
                <#if oauth.claimsRequested??>
                    <li>
                        <span>
                        Personal Info:&nbsp;
                            <#list oauth.claimsRequested as claim>
                                ${claim}&nbsp;
                            </#list>
                        </span>
                    </li>
                </#if>
                <#list oauth.realmRolesRequested as role>
                    <li>
                        <span><#if role.description??>${role.description}<#else>${role.name}</#if></span>
                    </li>
                </#list>

                <#list oauth.resourceRolesRequested?keys as resource>
                    <#list oauth.resourceRolesRequested[resource] as role>
                        <li>
                            <span class="kc-role"><#if role.description??>${role.description}<#else>${role.name}</#if></span>
                            <span class="kc-resource">in <strong>${resource}</strong></span>
                        </li>
                    </#list>
                </#list>
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
                            <input class="btn btn-primary btn-lg" name="accept" id="kc-login" type="submit" value="${rb.yes}"/>
                            <input class="btn btn-default btn-lg" name="cancel" id="kc-cancel" type="submit" value="${rb.no}"/>
                        </div>
                    </div>
                </div>
            </form>
        </div>
    </#if>
</@layout.registrationLayout>