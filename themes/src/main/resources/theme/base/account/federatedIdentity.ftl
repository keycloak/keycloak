<#import "template.ftl" as layout>
<@layout.mainLayout active='social' bodyClass='social'; section>

    <div class="row">
        <div class="col-md-10">
            <h2>${msg("federatedIdentitiesHtmlTitle")}</h2>
        </div>
    </div>

    <div id="federated-identities">
    <#list federatedIdentity.identities as identity>
        <div class="row margin-bottom">
            <div class="col-sm-2 col-md-2">
                <label for="${identity.providerId!}" class="control-label">${identity.displayName!}</label>
            </div>
            <div class="col-sm-5 col-md-5">
                <input disabled="true" class="form-control" value="${identity.userName!}">
            </div>
            <div class="col-sm-5 col-md-5">
                <#if identity.connected>
                    <#if federatedIdentity.removeLinkPossible>
                        <form action="${url.socialUrl}" method="post" class="form-inline">
                            <input type="hidden" id="stateChecker" name="stateChecker" value="${stateChecker}">
                            <input type="hidden" id="action" name="action" value="remove">
                            <input type="hidden" id="providerId" name="providerId" value="${identity.providerId!}">
                            <button id="remove-link-${identity.providerId!}" class="btn btn-default">${msg("doRemove")}</button>
                        </form>
                    </#if>
                <#else>
                    <form action="${url.socialUrl}" method="post" class="form-inline">
                        <input type="hidden" id="stateChecker" name="stateChecker" value="${stateChecker}">
                        <input type="hidden" id="action" name="action" value="add">
                        <input type="hidden" id="providerId" name="providerId" value="${identity.providerId!}">
                        <button id="add-link-${identity.providerId!}" class="btn btn-default">${msg("doAdd")}</button>
                    </form>
                </#if>
            </div>
        </div>
    </#list>
    </div>

</@layout.mainLayout>
