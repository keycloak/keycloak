<#import "template.ftl" as layout>
<@layout.mainLayout active='social' bodyClass='social'; section>

    <div class="row">
        <div class="col-md-10">
            <h2>Social Accounts</h2>
        </div>
    </div>

    <form action="${url.passwordUrl}" class="form-horizontal" method="post">
        <#list social.links as socialLink>
            <div class="form-group">
                <div class="col-sm-2 col-md-2">
                    <label for="${socialLink.providerId!}" class="control-label">${socialLink.providerName!}</label>
                </div>
                <div class="col-sm-5 col-md-5">
                    <input disabled="true" class="form-control" value="${socialLink.socialUsername!}">
                </div>
                <div class="col-sm-5 col-md-5">
                    <#if socialLink.connected>
                        <#if social.removeLinkPossible>
                            <a href="${socialLink.actionUrl}" type="submit" class="btn btn-primary btn-lg">Remove ${socialLink.providerName!}</a>
                        </#if>
                    <#else>
                        <a href="${socialLink.actionUrl}" type="submit" class="btn btn-primary btn-lg">Add ${socialLink.providerName!}</a>
                    </#if>
                </div>
            </div>
        </#list>
    </form>

</@layout.mainLayout>