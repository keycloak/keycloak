<#import "template.ftl" as layout>
<@layout.mainLayout active='authorization' bodyClass='authorization'; section>
    <style>
        .search-box,.close-icon,.search-wrapper {
        	position: relative;
        }
        .search-wrapper {
        	width: 500px;
        	margin: auto;
        	margin-top: 50px;
        }
        .search-box {
        	border: 1px solid #ccc;
            outline: 0;
            border-radius: 15px;
            background-color: #c7e5f0;
            padding: 5px;
        }
        .search-box:focus {
        	box-shadow: 0 0 15px 5px #b0e0ee;
        	border: 2px solid #bebede;
        }
        .close-icon {
        	border:1px solid transparent;
        	background-color: transparent;
        	display: inline-block;
        	float: right;
          outline: 0;
          cursor: pointer;
        }
        .close-icon:after {
        	display: block;
        	width: 15px;
        	height: 15px;
        	background-color: #FA9595;
        	z-index:1;
        	right: 35px;
        	top: 0;
        	bottom: 0;
        	margin: auto;
        	padding: 2px;
        	border-radius: 50%;
        	text-align: center;
        	color: white;
        	font-weight: normal;
        	font-size: 12px;
        	box-shadow: 0 0 2px #E50F0F;
        	cursor: pointer;
        }
        .search-box:not(:valid) ~ .close-icon {
        	display: none;
        }
    </style>
    <script>
        function removeScopeElm(elm) {
            var td = elm.parentNode;
            var tr = td.parentNode;
            var tbody = tr.parentNode;

            td.removeChild(elm);

            var childCount = td.childNodes.length - 1;

            for (i = 0; i < td.childNodes.length; i++) {
                if (!td.childNodes[i].tagName || td.childNodes[i].tagName.toUpperCase() != 'DIV') {
                    td.removeChild(td.childNodes[i]);
                    childCount--;
                }
            }

            if (childCount <= 0) {
                tbody.removeChild(tr);
            }
        }

        function removeAllScopes(id) {
            var scopesElm = document.getElementsByName('removeScope-' + id);

            for (i = 0; i < scopesElm.length; i++) {
                var td = scopesElm[i].parentNode.parentNode;
                var tr = td.parentNode;
                var tbody = tr.parentNode;
                tbody.removeChild(tr);
            }
        }
    </script>
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
                    <td>${msg("shares")}</td>
                    <td>${msg("approvalRequests")}</td>
                    <td>${msg("application")}</td>
                </tr>
                </thead>

                <tbody>
                <#if authorization.userResources?size != 0>
                    <#list authorization.userResources as resource>
                        <tr>
                            <td>
                                <a id="${resource.name}-detail" href="${url.getResourceDetailUrl(resource.id)}">${resource.name}</a>
                            </td>
                            <td>
                                <#if resource.permission.granted?size != 0>
                                    ${resource.permission.granted?size}
                                <#else>
                                    This resource is not being shared.
                                </#if>
                            </td>
                            <td>
                                <#if resource.approvalRequests != 0>
                                    ${resource.approvalRequests} permission request(s) <a href="${url.getResourceDetailUrl(resource.id)}">waiting</a> for approval.
                                <#else>
                                    No permission requests
                                </#if>
                            </td>
                            <td>
                                <a href="${resource.resourceServer.redirectUri}">${resource.resourceServer.name}</a>
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
                            <td width="5%"><input type="checkbox" disabled="authorization.userSharedResources?size == 0"/></td>
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
                                        <a href="${resource.resourceServer.redirectUri}">${resource.resourceServer.name}</a>
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
            <a href="#" onclick="document.forms['cancelForm'].submit();" type="submit" class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!}">${msg("doRemove")}</a>
        </div>
        </#if>
    </div>

    <div class="row">
        <div class="col-md-12">
            <h2>
                ${msg("approvalRequests")}
            </h2>
        </div>
    </div>

    <div class="row">
        <div class="col-md-12">
            <table class="table table-striped table-bordered">
                <thead>
                    <tr>
                        <td>${msg("resource")}</td>
                        <td>${msg("requester")}</td>
                        <td>${msg("actions")}</td>
                        <td>${msg("doGrant")}</td>
                    </tr>
                </thead>
                <tbody>
                    <#if authorization.userPendingRequests?size != 0>
                        <#list authorization.userPendingRequests as resourcePermission>
                                <#list resourcePermission.pending as permission>
                                    <form action="${url.getResourceGrant(resourcePermission.resource.id)}" name="approveForm-${resourcePermission.resource}-${permission.requester}" method="post">
                                        <input type="hidden" name="action" value="grant">
                                        <input type="hidden" name="requester" value="${permission.requester}">
                                        <tr>
                                            <td>${resourcePermission.resource.name}</td>
                                            <td>${permission.requester}</td>
                                            <td>
                                                <#list permission.scopes as scope>
                                                    <#if !scope.granted>
                                                        <div class="search-box">
                                                            ${scope.scope.name}
                                                            <button class="close-icon" type="button" name="removeScope-${resourcePermission.resource}-${permission.requester}" onclick="removeScopeElm(this.parentNode);document.forms['approveForm-${resourcePermission.resource}-${permission.requester}']['action'].value = 'deny';document.forms['approveForm-${resourcePermission.resource}-${permission.requester}'].submit();"><i class="fa fa-times" aria-hidden="true"></i></button>
                                                            <input type="hidden" name="permission_id" value="${scope.id}"/>
                                                        </div>
                                                    </#if>
                                                </#list>
                                            </td>
                                            <td width="20%" align="middle" style="vertical-align: middle">
                                                <a href="#" id="grant-${resourcePermission.resource.name}-${permission.requester}" onclick="document.forms['approveForm-${resourcePermission.resource}-${permission.requester}']['action'].value = 'grant';document.forms['approveForm-${resourcePermission.resource}-${permission.requester}'].submit();" type="submit" id="grant-${permission.id}" class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!}">${msg("doGrant")}</a>
                                                <a href="#" id="deny-${resourcePermission.resource.name}-${permission.requester}" onclick="removeAllScopes('${resourcePermission.resource}-${permission.requester}');document.forms['approveForm-${resourcePermission.resource}-${permission.requester}']['action'].value = 'deny';document.forms['approveForm-${resourcePermission.resource}-${permission.requester}'].submit();" type="submit" id="grant-${permission.id}" class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!}">${msg("doDeny")}</a>
                                            </td>
                                        </tr>
                                    </form>
                                </#list>
                            </form>
                        </#list>
                    </#if>
                </tbody>
            </table>
        </div>
    </div>

    <div class="row">
            <div class="col-md-12">
                <h2>
                    ${msg("waitingApproval")}
                </h2>
            </div>
        </div>

    <div class="row">
        <div class="col-md-12">
            <#if authorization.userPermissionRequests?size != 0>
                <i class="pficon pficon-info"></i> You have ${authorization.userPermissionRequests?size} permission request(s) <a href="#" onclick="document.getElementById('waitingApproval').style.display=''">waiting</a> for approval.
            <#else>
                You have no permission requests waiting for approval.
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
                    <a href="#" onclick="document.forms['cancelRequestForm'].submit();" type="submit" class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!}">${msg("doRemove")}</a>
                </div>
            </div>
        </div>
    </div>

</@layout.mainLayout>