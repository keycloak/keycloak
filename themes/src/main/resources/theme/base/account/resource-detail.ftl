<#import "template.ftl" as layout>
<@layout.mainLayout active='authorization' bodyClass='authorization'; section>

    <div class="row">
        <div class="col-md-10">
            <h2>
                <a href="${url.resourceUrl}">My Resources</a> <i class="fa fa-angle-double-right"></i> ${authorization.resource.name}
            </h2>
        </div>
    </div>

    <#if authorization.resource.iconUri??>
        <img src="${authorization.resource.iconUri}">
        <br/>
    </#if>

    <div class="row">
        <div class="col-md-10">
            <h2>
                ${msg("shares")}
            </h2>
        </div>
    </div>
    <div class="row">
        <div class="col-md-12">
                <table class="table table-striped table-bordered">
                    <thead>
                        <tr>
                            <td>${msg("user")}</td>
                            <td>${msg("actions")}</td>
                            <td>${msg("date")}</td>
                            <td>${msg("doRevoke")}</td>
                        </tr>
                    </thead>
                    <tbody>
                        <#if authorization.resource.permission?? && authorization.resource.permission.granted?size != 0>
                            <form action="${url.getResourceGrant(authorization.resource.id)}" name="revokeForm" method="post">
                                <#list authorization.resource.permission.granted as permission>
                                    <input type="hidden" name="action" value="revoke">
                                    <input type="hidden" name="requester" value="${permission.requester}">
                                    <tr>
                                        <td>${permission.requester}</td>
                                        <td>
                                            <#if permission.scopes?size != 0>
                                                <#list permission.scopes as scope>
                                                    <#if scope.granted>
                                                        ${scope.scope.name}
                                                    </#if>
                                                </#list>
                                            <#else>
                                                Any action
                                            </#if>
                                        </td>
                                        <td>
                                            ${permission.createdDate?datetime}
                                        </td>
                                        <td width="20%" align="middle" style="vertical-align: middle">
                                            <a href="#" onclick="document.forms['revokeForm'].submit();" type="submit" id="grant-${permission.id}" class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!}">${msg("doRevoke")}</a>
                                        </td>
                                    </tr>
                                </#list>
                            </form>
                        <#else>
                            <tr>
                                <td colspan="4">The resource is not being shared</td>
                            </tr>
                        </#if>
                    </tbody>
                </table>
            </form>
        </div>
    </div>
    <div class="row">
        <div class="col-md-10">
            <h2>
                ${msg("shareWithOthers")}
            </h2>
        </div>
    </div>
    <div class="row">
        <div class="col-md-12">
            <form action="${url.getResourceShare(authorization.resource.id)}" name="shareForm" method="post">
                <div class="col-sm-2 col-md-2">
                    <label for="password" class="control-label">${msg("username")}</label>
                </div>
                <div class="col-sm-10 col-md-10">
                    <div class="row">
                        <div class="col-md-12">
                            <input type="text" class="form-control" id="user_id" name="user_id" autofocus autocomplete="off">
                        </div>
                        <div class="col-md-12">
                            <br/>
                            <select id="share-scopes" class="form-control" name="scope_id" multiple>
                                <#list authorization.resource.scopes as scope>
                                    <option value="${scope.id}">${scope.name}</option>
                                </#list>
                            </select>
                        </div>
                        <div class="col-md-12">
                            <br/>
                            <a href="#" onclick="document.forms['shareForm'].submit()" type="submit" id="share" class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!}">${msg("Share")}</a>
                        </div>
                    </div>
                </div>
            </form>
        </div>
    </div>
    <br/>
</@layout.mainLayout>