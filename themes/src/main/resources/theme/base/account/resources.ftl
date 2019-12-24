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
            padding: 2px 5px;
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

    <#if authorization.resourcesWaitingApproval?size != 0>
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
                        <#list authorization.resourcesWaitingApproval as resource>
                            <#list resource.permissions as permission>
                                <form action="${url.getResourceGrant(resource.id)}" name="approveForm-${resource.id}-${permission.requester.username}" method="post">
                                    <input type="hidden" name="action" value="grant">
                                    <input type="hidden" name="requester" value="${permission.requester.username}">
                                    <input type="hidden" id="stateChecker" name="stateChecker" value="${stateChecker}">
                                    <tr>
                                        <td>
                                            <#if resource.displayName??>${resource.displayName}<#else>${resource.name}</#if>
                                        </td>
                                        <td>
                                            <#if permission.requester.email??>${permission.requester.email}<#else>${permission.requester.username}</#if>
                                        </td>
                                        <td>
                                            <#list permission.scopes as scope>
                                                <#if scope.scope??>
                                                    <div class="search-box">
                                                        <#if scope.scope.displayName??>
                                                            ${scope.scope.displayName}
                                                        <#else>
                                                            ${scope.scope.name}
                                                        </#if>
                                                        <button class="close-icon" type="button" id="grant-remove-scope-${resource.name}-${permission.requester.username}-${scope.scope.name}" name="removeScope-${resource.id}-${permission.requester.username}" onclick="removeScopeElm(this.parentNode);document.forms['approveForm-${resource.id}-${permission.requester.username}']['action'].value = 'deny';document.forms['approveForm-${resource.id}-${permission.requester.username}'].submit();"><i class="fa fa-times" aria-hidden="true"></i></button>
                                                        <input type="hidden" name="permission_id" value="${scope.id}"/>
                                                    </div>
                                                <#else>
                                                    ${msg("anyPermission")}
                                                </#if>
                                            </#list>
                                        </td>
                                        <td width="20%" align="middle" style="vertical-align: middle">
                                            <a href="#" id="grant-${resource.name}-${permission.requester.username}" onclick="document.forms['approveForm-${resource.id}-${permission.requester.username}']['action'].value = 'grant';document.forms['approveForm-${resource.id}-${permission.requester.username}'].submit();" type="submit" class="btn btn-primary">${msg("doApprove")}</a>
                                            <a href="#" id="deny-${resource.name}-${permission.requester.username}" onclick="removeAllScopes('${resource.id}-${permission.requester.username}');document.forms['approveForm-${resource.id}-${permission.requester.username}']['action'].value = 'deny';document.forms['approveForm-${resource.id}-${permission.requester.username}'].submit();" type="submit" class="btn btn-danger">${msg("doDeny")}</a>
                                        </td>
                                    </tr>
                                </form>
                            </#list>
                        </#list>
                    </tbody>
                </table>
            </div>
        </div>
    </#if>

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
                <#if authorization.resources?size != 0>
                    <#list authorization.resources as resource>
                        <tr>
                            <td>
                                <a id="detail-${resource.name}" href="${url.getResourceDetailUrl(resource.id)}">
                                    <#if resource.displayName??>${resource.displayName}<#else>${resource.name}</#if>
                                </a>
                            </td>
                            <td>
                                <#if resource.resourceServer.baseUri??>
                                    <a href="${resource.resourceServer.baseUri}">${resource.resourceServer.name}</a>
                                <#else>
                                    ${resource.resourceServer.name}
                                </#if>
                            </td>
                            <td>
                                <#if resource.shares?size != 0>
                                    <a href="${url.getResourceDetailUrl(resource.id)}">${resource.shares?size} <i class="fa fa-users"></i></a>
                                <#else>
                                    ${msg("notBeingShared")}
                                </#if>
                            </td>
                        </tr>
                    </#list>
                <#else>
                    <tr>
                        <td colspan="4">${msg("notHaveAnyResource")}</td>
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
                <input type="hidden" id="stateChecker" name="stateChecker" value="${stateChecker}">
                <table class="table table-striped table-bordered">
                    <thead>
                        <tr>
                            <th width="5%"><input type="checkbox" onclick="selectAllCheckBoxes('shareForm', this, 'resource_id');" <#if authorization.sharedResources?size == 0>disabled="true"</#if></td>
                            <th>${msg("resource")}</th>
                            <th>${msg("owner")}</th>
                            <th>${msg("application")}</th>
                            <th>${msg("permission")}</th>
                            <th>${msg("date")}</th>
                        </tr>
                    </thead>
                    <tbody>
                        <#if authorization.sharedResources?size != 0>
                            <#list authorization.sharedResources as resource>
                                <tr>
                                    <td>
                                        <input type="checkbox" name="resource_id" value="${resource.id}"/>
                                    </td>
                                    <td>
                                        <#if resource.displayName??>${resource.displayName}<#else>${resource.name}</#if>
                                    </td>
                                    <td>
                                        <#if resource.owner.email??>${resource.owner.email}<#else>${resource.owner.username}</#if>
                                    </td>
                                    <td>
                                        <a href="${resource.resourceServer.baseUri}">${resource.resourceServer.name}</a>
                                    </td>
                                    <td>
                                        <#if resource.permissions?size != 0>
                                            <ul>
                                                <#list resource.permissions as permission>
                                                    <#list permission.scopes as scope>
                                                        <#if scope.granted && scope.scope??>
                                                            <li>
                                                                <#if scope.scope.displayName??>
                                                                    ${scope.scope.displayName}
                                                                <#else>
                                                                    ${scope.scope.name}
                                                                </#if>
                                                            </li>
                                                        <#else>
                                                            ${msg("anyPermission")}
                                                        </#if>
                                                    </#list>
                                                </#list>
                                            </ul>
                                        <#else>
                                            Any action
                                        </#if>
                                    </td>
                                    <td>
                                        ${resource.permissions[0].grantedDate?datetime}
                                    </td>
                                </tr>
                            </#list>
                        <#else>
                            <tr>
                                <td colspan="6">${msg("noResourcesSharedWithYou")}</td>
                            </tr>
                        </#if>
                    </tbody>
                </table>
            </form>
        </div>
        <#if authorization.sharedResources?size != 0>
        <div class="col-md-12">
            <a href="#" onclick="document.forms['shareForm'].submit();" type="submit" class="btn btn-danger">${msg("doRemoveSharing")}</a>
        </div>
        </#if>
    </div>

    <#if authorization.resourcesWaitingOthersApproval?size != 0>
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
                <i class="pficon pficon-info"></i> ${msg("havePermissionRequestsWaitingForApproval",authorization.resourcesWaitingOthersApproval?size)}
                <a href="#" onclick="document.getElementById('waitingApproval').style.display=''">${msg("clickHereForDetails")}</a>
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
                            <input type="hidden" id="stateChecker" name="stateChecker" value="${stateChecker}">
                            <table class="table table-striped table-bordered">
                                <thead>
                                    <tr>
                                        <th width="5%"><input type="checkbox" onclick="selectAllCheckBoxes('waitingApprovalForm', this, 'resource_id');" <#if authorization.resourcesWaitingOthersApproval?size == 0>disabled="true"</#if></th>
                                        <th>${msg("resource")}</th>
                                        <th>${msg("owner")}</th>
                                        <th>${msg("action")}</th>
                                        <th>${msg("date")}</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <#list authorization.resourcesWaitingOthersApproval as resource>
                                        <tr>
                                            <td>
                                                <input type="checkbox" name="resource_id" value="${resource.id}"/>
                                            </td>
                                            <td>
                                                <#if resource.displayName??>${resource.displayName}<#else>${resource.name}</#if>
                                            </td>
                                            <td>
                                                <#if resource.owner.email??>${resource.owner.email}<#else>${resource.owner.username}</#if>
                                            </td>
                                            <td>
                                                <ul>
                                                    <#list resource.permissions as permission>
                                                        <#list permission.scopes as scope>
                                                            <li>
                                                                <#if scope.scope??>
                                                                    <#if scope.scope.displayName??>
                                                                        ${scope.scope.displayName}
                                                                    <#else>
                                                                        ${scope.scope.name}
                                                                    </#if>
                                                                <#else>
                                                                    ${msg("anyPermission")}
                                                                </#if>
                                                            </li>
                                                        </#list>
                                                    </#list>
                                                </ul>
                                            </td>
                                            <td>
                                                ${resource.permissions[0].createdDate?datetime}
                                            </td>
                                        </tr>
                                    </#list>
                                </tbody>
                            </table>
                        </form>
                    </div>
                    <div class="col-md-12">
                        <a href="#" onclick="document.forms['waitingApprovalForm'].submit();" type="submit" class="btn btn-danger">${msg("doRemoveRequest")}</a>
                    </div>
                </div>
            </div>
        </div>
    </#if>

</@layout.mainLayout>