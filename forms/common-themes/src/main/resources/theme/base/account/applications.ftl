<#import "template.ftl" as layout>
<@layout.mainLayout active='access' bodyClass='access'; section>

    <div class="row">
        <div class="col-md-10">
            <h2>${msg("accessHtmlTitle")}</h2>
        </div>
    </div>

    <form action="${url.revokeClientUrl}" method="post">
        <input type="hidden" id="stateChecker" name="stateChecker" value="${stateChecker}">

        <table class="table table-striped table-bordered">
            <thead>
              <tr>
                <td>${msg("client")}</td>
                <td>${msg("grantedPersonalInfo")}</td>
                <td>${msg("grantedPermissions")}</td>
                <td>${msg("action")}</td>
              </tr>
            </thead>

            <tbody>
              <#list consent.clientGrants as clientGrant>
                <tr>
                    <td>
                        <#if clientGrant.client.baseUrl??><a href="${clientGrant.client.baseUrl}"></#if>
                            <#if clientGrant.client.name??>${advancedMsg(clientGrant.client.name)}<#else>${clientGrant.client.clientId}</#if>
                        <#if clientGrant.client.baseUrl??></a></#if>
                    </td>
                    <td>
                        <#list clientGrant.claimsGranted as claim>
                            ${advancedMsg(claim)}<#if claim_has_next>, </#if>
                        </#list>
                    </td>
                    <td>
                        <#list clientGrant.realmRolesGranted as role>
                            <#if role.description??>${advancedMsg(role.description)}<#else>${advancedMsg(role.name)}</#if>
                            <#if role_has_next>, </#if>
                        </#list>
                        <#list clientGrant.resourceRolesGranted?keys as resource>
                            <#if clientGrant.realmRolesGranted?has_content>, </#if>
                            <#list clientGrant.resourceRolesGranted[resource] as clientRole>
                                <#if clientRole.roleDescription??>${advancedMsg(clientRole.roleDescription)}<#else>${advancedMsg(clientRole.roleName)}</#if>
                                ${msg("inResource")} <strong><#if clientRole.clientName??>${advancedMsg(clientRole.clientName)}<#else>${clientRole.clientId}</#if></strong>
                                <#if clientRole_has_next>, </#if>
                            </#list>
                        </#list>
                    </td>
                    <td>
                        <button type='submit' class='btn btn-primary' id='revoke-${clientGrant.client.clientId}' name='clientId' value="${clientGrant.client.id}">${msg("revoke")}</button>
                    </td>
                </tr>
              </#list>
            </tbody>
        </table>
    </form>

</@layout.mainLayout>