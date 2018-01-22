<#import "template.ftl" as layout>
<@layout.mainLayout active='authorization' bodyClass='authorization'; section>

    <div class="row">
        <div class="col-md-10">
            <h2>
                ${msg("myResources")}
            </h2>
        </div>
    </div>

    <div class="row">
        <div class="col-md-12">
            <table class="table table-striped table-bordered">
                <thead>
                <tr>
                    <td>${msg("resource")}</td>
                    <td>${msg("approvalRequests")}</td>
                    <td>${msg("application")}</td>
                </tr>
                </thead>

                <tbody>
                <#if authorization.userResources?size != 0>
                    <#list authorization.userResources as resource>
                        <tr>
                            <td>
                                ${resource.name}
                                <a href="${url.getResourceDetailUrl(resource.id)}"><i class="fa fa-share" aria-hidden="true"></i></a>
                            </td>
                            <td>
                                <#if resource.approvalRequests != 0>
                                    ${resource.approvalRequests} permission request(s) <a href="${url.getResourceDetailUrl(resource.id)}">waiting</a> for approval.
                                <#else>
                                    No permission requests
                                </#if>
                            </td>
                            <td>
                                ${resource.resourceServerName}
                            </td>
                        </tr>
                    </#list>
                <#else>
                    <tr>
                        <td colspan="4">You don't have any resource</td>
                    </tr>
                </#if>
                </tbody>
            </table>
        </div>
    </div>

    <div class="row">
        <div class="col-md-12">
            <h2>
                ${msg("resourcesSharedWithMe")}
            </h2>
        </div>
    </div>
    <div class="row">
        <div class="col-md-12">
            <form action="${url.resourceUrl}" name="cancelForm" method="post">
                <input type="hidden" name="action" value="cancel"/>
                <table class="table table-striped table-bordered">
                    <thead>
                        <tr>
                            <td></td>
                            <td>${msg("resource")}</td>
                            <td>${msg("owner")}</td>
                            <td>${msg("actions")}</td>
                            <td>${msg("date")}</td>
                            <td>${msg("application")}</td>
                        </tr>
                    </thead>
                    <tbody>
                        <#if authorization.userSharedResources?size != 0>
                            <#list authorization.userSharedResources as resource>
                                <tr>
                                    <td>
                                        <input type="checkbox" name="resource_id" value="${resource.id}"/>
                                    </td>
                                    <td>
                                        ${resource.name}
                                    </td>
                                    <td>
                                        ${resource.owner.username}
                                    </td>
                                    <td>
                                        <#if resource.permission.granted?size != 0>
                                            <#list resource.permission.granted as permission>
                                                <#list permission.scopes as scope>
                                                    <#if scope.granted>
                                                        ${scope.scope.name}
                                                    </#if>
                                                </#list>
                                            </#list>
                                        <#else>
                                            Any action
                                        </#if>
                                    </td>
                                    <td>
                                        ${resource.permission.grantedDate?datetime}
                                    </td>
                                    <td>
                                        ${resource.resourceServerName}
                                    </td>
                                </tr>
                            </#list>
                        <#else>
                            <tr>
                                <td colspan="5">There are no resources shared with you</td>
                            </tr>
                        </#if>
                    </tbody>
                </table>
            </form>
        </div>
        <#if authorization.userSharedResources?size != 0>
        <div class="col-md-12">
            <a href="#" onclick="document.forms['cancelForm'].submit();" type="submit" class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!}">${msg("doCancel")}</a>
        </div>
        </#if>
    </div>

    <div class="row">
        <div class="col-md-10">
            <br/>
        </div>
    </div>

    <div class="row">
        <div class="col-md-12">
            <#if authorization.userPermissionRequests?size != 0>
                <i class="pficon pficon-info"></i> You have ${authorization.userPermissionRequests?size} permission request(s) <a href="#" onclick="document.getElementById('waitingApproval').style.display=''">waiting</a> for approval.
            </#if>
            <div class="row">
                <div class="col-md-12"></div>
            </div>
            <div class="row">
                <div class="col-md-12"></div>
            </div>
            <div class="row">
                <div class="col-md-12"></div>
            </div>
            <div class="row" id="waitingApproval" style="display:none">
                <div class="col-md-12">
                    <form action="${url.resourceUrl}" name="cancelRequestForm" method="post">
                        <input type="hidden" name="action" value="cancelRequest"/>
                        <table class="table table-striped table-bordered">
                            <thead>
                                <tr>
                                    <td></td>
                                    <td>${msg("name")}</td>
                                    <td>${msg("owner")}</td>
                                    <td>${msg("actions")}</td>
                                    <td>${msg("date")}</td>
                                </tr>
                            </thead>
                            <tbody>
                                <#if authorization.userPermissionRequests?size != 0>
                                    <#list authorization.userPermissionRequests as permission>
                                        <tr>
                                            <td>
                                                <input type="checkbox" name="resource_id" value="${permission.resource.id}"/>
                                            </td>
                                            <td>
                                                ${permission.resource.name}
                                            </td>
                                            <td>
                                                ${permission.resource.owner.username}
                                            </td>
                                            <td>
                                                <#list permission.pending as requester>
                                                    <#list requester.scopes as scope>
                                                        ${scope.scope.name}
                                                    </#list>
                                                </#list>
                                            </td>
                                            <td>
                                                ${permission.createdDate?datetime}
                                            </td>
                                        </tr>
                                    </#list>
                                <#else>
                                    <tr>
                                        <td colspan="5">There are no resources shared with you</td>
                                    </tr>
                                </#if>
                            </tbody>
                        </table>
                    </form>
                </div>
                <div class="col-md-12">
                    <a href="#" onclick="document.forms['cancelRequestForm'].submit();" type="submit" class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!}">${msg("doCancel")}</a>
                </div>
            </div>
        </div>
    </div>

</@layout.mainLayout>