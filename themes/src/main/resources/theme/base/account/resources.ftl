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
            font-weight: 600;
            color: white;
            border: 1px solid #006e9c;
            outline: 0;
            border-radius: 15px;
            background-color: #0085cf;
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
        function showHideActions(elm) {
            if (elm.style.display == 'none') {
                elm.style.display = '';
            } else {
                elm.style.display = 'none';
            }
        }
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

        function selectAllCheckBoxes(formName, elm, name) {
            var shares = document.forms[formName].getElementsByTagName('input');

            for (i = 0; i < shares.length; i++) {
                if (shares[i].name == name) {
                    shares[i].checked = elm.checked;
                }
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
            <h3>
                ${msg("needMyApproval")}
            </h3>
        </div>
    </div>
    <div class="row">
        <div class="col-md-12">
            <table class="table table-striped table-bordered">
                <thead>
                    <tr>
                        <th>${msg("resource")}</th>
                        <th>${msg("requestor")}</th>
                        <th>${msg("permissionRequestion")}</th>
                        <th>${msg("action")}</th>
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
                                            <td>
                                                <#if resourcePermission.resource.displayName??>${resourcePermission.resource.displayName}<#else>${resourcePermission.resource.name}</#if>
                                            </td>
                                            <td>${permission.requester}</td>
                                            <td>
                                                <#list permission.scopes as scope>
                                                    <#if !scope.granted>
                                                        <div class="search-box">
                                                            <#if scope.scope.displayName??>
                                                                ${scope.scope.displayName}
                                                            <#else>
                                                                ${scope.scope.name}
                                                            </#if>
                                                            <button class="close-icon" type="button" id="grant-remove-scope-${resourcePermission.resource.name}-${permission.requester}-${scope.scope.name}" name="removeScope-${resourcePermission.resource}-${permission.requester}" onclick="removeScopeElm(this.parentNode);document.forms['approveForm-${resourcePermission.resource}-${permission.requester}']['action'].value = 'deny';document.forms['approveForm-${resourcePermission.resource}-${permission.requester}'].submit();"><i class="fa fa-times" aria-hidden="true"></i></button>
                                                            <input type="hidden" name="permission_id" value="${scope.id}"/>
                                                        </div>
                                                    </#if>
                                                </#list>
                                            </td>
                                            <td width="20%" align="middle" style="vertical-align: middle">
                                                <a href="#" id="grant-${resourcePermission.resource.name}-${permission.requester}" onclick="document.forms['approveForm-${resourcePermission.resource}-${permission.requester}']['action'].value = 'grant';document.forms['approveForm-${resourcePermission.resource}-${permission.requester}'].submit();" type="submit" class="btn btn-primary">${msg("doApprove")}</a>
                                                <a href="#" id="deny-${resourcePermission.resource.name}-${permission.requester}" onclick="removeAllScopes('${resourcePermission.resource}-${permission.requester}');document.forms['approveForm-${resourcePermission.resource}-${permission.requester}']['action'].value = 'deny';document.forms['approveForm-${resourcePermission.resource}-${permission.requester}'].submit();" type="submit" class="btn btn-danger">${msg("doDeny")}</a>
                                            </td>
                                        </tr>
                                    </form>
                                </#list>
                            </form>
                        </#list>
                    <#else>
                        <tr>
                            <td colspan="4">There are no approval requests.</td>
                        </tr>
                    </#if>
                </tbody>
            </table>
        </div>
    </div>

    <div class="row">
        <div class="col-md-12">
            <h3>
                ${msg("myResourcesSub")}
            </h3>
        </div>
    </div>
    <div class="row">
        <div class="col-md-12">
            <table class="table table-striped table-bordered">
                <thead>
                <tr>
                    <th>${msg("resource")}</th>
                    <th>${msg("application")}</th>
                    <th>${msg("peopleSharingThisResource")}</th>
                </tr>
                </thead>

                <tbody>
                <#if authorization.userResources?size != 0>
                    <#list authorization.userResources as resource>
                        <tr>
                            <td>
                                <a id="detail-${resource.name}" href="${url.getResourceDetailUrl(resource.id)}">
                                    <#if resource.displayName??>${resource.displayName}<#else>${resource.name}</#if>
                                </a>
                            </td>
                            <td>
                                <a href="${resource.resourceServer.redirectUri}">${resource.resourceServer.name}</a>
                            </td>
                            <td>
                                <#if resource.permission.granted?size != 0>
                                    <a href="${url.getResourceDetailUrl(resource.id)}">${resource.permission.granted?size}</a> ${msg("shares")}
                                <#else>
                                    This resource is not being shared.
                                </#if>
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
            <h3>
                ${msg("resourcesSharedWithMe")}
            </h3>
        </div>
    </div>
    <div class="row">
        <div class="col-md-12">
            <form action="${url.resourceUrl}" name="shareForm" method="post">
                <input type="hidden" name="action" value="cancel"/>
                <table class="table table-striped table-bordered">
                    <thead>
                        <tr>
                            <th width="5%"><input type="checkbox" onclick="selectAllCheckBoxes('shareForm', this, 'resource_id');" <#if authorization.userSharedResources?size == 0>disabled="true"</#if></td>
                            <th>${msg("resource")}</th>
                            <th>${msg("owner")}</th>
                            <th>${msg("application")}</th>
                            <th>${msg("permission")}</th>
                            <th>${msg("date")}</th>
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
                                        <#if resource.displayName??>${resource.displayName}<#else>${resource.name}</#if>
                                    </td>
                                    <td>
                                        ${resource.owner.username}
                                    </td>
                                    <td>
                                        <a href="${resource.resourceServer.redirectUri}">${resource.resourceServer.name}</a>
                                    </td>
                                    <td>
                                        <#if resource.permission.granted?size != 0>
                                            <ul>
                                                <#list resource.permission.granted as permission>
                                                    <#list permission.scopes as scope>
                                                        <#if scope.granted>
                                                            <li>
                                                                <#if scope.scope.displayName??>
                                                                    ${scope.scope.displayName}
                                                                <#else>
                                                                    ${scope.scope.name}
                                                                </#if>
                                                            </li>
                                                        </#if>
                                                    </#list>
                                                </#list>
                                            </ul>
                                        <#else>
                                            Any action
                                        </#if>
                                    </td>
                                    <td>
                                        ${resource.permission.grantedDate?datetime}
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
            <a href="#" onclick="document.forms['shareForm'].submit();" type="submit" class="btn btn-danger">${msg("doRemove")}</a>
        </div>
        </#if>
    </div>

    <br/>
    <div class="row">
        <div class="col-md-12">
            <h3>
                ${msg("requestsWaitingApproval")}
            </h3>
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
                    <form action="${url.resourceUrl}" name="waitingApprovalForm" method="post">
                        <input type="hidden" name="action" value="cancelRequest"/>
                        <table class="table table-striped table-bordered">
                            <thead>
                                <tr>
                                    <th width="5%"><input type="checkbox" onclick="selectAllCheckBoxes('waitingApprovalForm', this, 'resource_id');" <#if authorization.userPermissionRequests?size == 0>disabled="true"</#if></th>
                                    <th>${msg("resource")}</th>
                                    <th>${msg("owner")}</th>
                                    <th>${msg("action")}</th>
                                    <th>${msg("date")}</th>
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
                                                <#if permission.resource.displayName??>${permission.resource.displayName}<#else>${permission.resource.name}</#if>
                                            </td>
                                            <td>
                                                ${permission.resource.owner.username}
                                            </td>
                                            <td>
                                                <ul>
                                                    <#list permission.pending as requester>
                                                        <#list requester.scopes as scope>
                                                            <li>
                                                                <#if scope.scope.displayName??>
                                                                    ${scope.scope.displayName}
                                                                <#else>
                                                                    ${scope.scope.name}
                                                                </#if>
                                                            </li>
                                                        </#list>
                                                    </#list>
                                                </ul>
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
                    <a href="#" onclick="document.forms['waitingApprovalForm'].submit();" type="submit" class="btn btn-danger">${msg("doRemove")}</a>
                </div>
            </div>
        </div>
    </div>

</@layout.mainLayout>