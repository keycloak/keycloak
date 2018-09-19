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
        function removeScopeElm(elm) {
            elm.parentNode.removeChild(elm);
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

        function getChildren(parent, childId) {
            var childNodes = [];

            for (i = 0; i < parent.childNodes.length; i++) {
                if (parent.childNodes[i].id == childId) {
                    childNodes.push(parent.childNodes[i]);
                }
            }

            return childNodes;
        }
    </script>

    <div class="row">
        <div class="col-md-10">
            <h2>
                <a href="${url.resourceUrl}">My Resources</a> <i class="fa fa-angle-right"></i> <#if authorization.resource.displayName??>${authorization.resource.displayName}<#else>${authorization.resource.name}</#if>
            </h2>
        </div>
    </div>

    <#if authorization.resource.iconUri??>
        <img src="${authorization.resource.iconUri}">
        <br/>
    </#if>

    <div class="row">
        <div class="col-md-10">
            <h3>
                ${msg("peopleAccessResource")}
            </h3>
        </div>
    </div>
    <div class="row">
        <div class="col-md-12">
                <table class="table table-striped table-bordered">
                    <thead>
                        <tr>
                            <th>${msg("user")}</th>
                            <th>${msg("permission")}</th>
                            <th>${msg("date")}</th>
                            <th>${msg("action")}</th>
                        </tr>
                    </thead>
                    <tbody>
                        <#if authorization.resource.shares?size != 0>
                            <#list authorization.resource.shares as permission>
                                <form action="${url.getResourceGrant(authorization.resource.id)}" name="revokeForm-${authorization.resource.id}-${permission.requester.username}" method="post">
                                    <input type="hidden" name="action" value="revoke">
                                    <input type="hidden" name="requester" value="${permission.requester.username}">
                                    <tr>
                                        <td>
                                            <#if permission.requester.email??>${permission.requester.email}<#else>${permission.requester.username}</#if>
                                        </td>
                                        <td>
                                            <#if permission.scopes?size != 0>
                                                <#list permission.scopes as scope>
                                                    <#if scope.granted>
                                                        <div class="search-box">
                                                            <#if scope.scope.displayName??>
                                                                ${scope.scope.displayName}
                                                            <#else>
                                                                ${scope.scope.name}
                                                            </#if>
                                                            <button class="close-icon" type="button" name="removeScope-${authorization.resource.id}-${permission.requester.username}" onclick="removeScopeElm(this.parentNode);document.forms['revokeForm-${authorization.resource.id}-${permission.requester.username}'].submit();"><i class="fa fa-times" aria-hidden="true"></i></button>
                                                            <input type="hidden" name="permission_id" value="${scope.id}"/>
                                                        </div>
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
                                            <a href="#" id="revoke-${authorization.resource.name}-${permission.requester.username}" onclick="removeAllScopes('${authorization.resource.id}-${permission.requester.username}');document.forms['revokeForm-${authorization.resource.id}-${permission.requester.username}'].submit();" type="submit" class="btn btn-primary">${msg("doRevoke")}</a>
                                        </td>
                                    </tr>
                                </form>
                            </#list>
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
            <h3>
                ${msg("resourceManagedPolicies")}
            </h3>
        </div>
    </div>
    <div class="row">
        <div class="col-md-12">
                <table class="table table-striped table-bordered">
                    <thead>
                        <tr>
                            <th>${msg("description")}</th>
                            <th>${msg("permission")}</th>
                            <th>${msg("action")}</th>
                        </tr>
                    </thead>
                    <tbody>
                        <#if authorization.resource.policies?size != 0>
                            <#list authorization.resource.policies as permission>
                                <form action="${url.getResourceGrant(authorization.resource.id)}" name="revokePolicyForm-${authorization.resource.id}-${permission.id}" method="post">
                                    <input type="hidden" name="action" value="revokePolicy">
                                    <input type="hidden" name="permission_id" value="${permission.id}"/>
                                    <tr>
                                        <td>
                                            <#if permission.description??>
                                                ${permission.description}
                                            </#if>
                                        </td>
                                        <td>
                                            <#if permission.scopes?size != 0>
                                                <#list permission.scopes as scope>
                                                    <div class="search-box">
                                                        <#if scope.displayName??>
                                                            ${scope.displayName}
                                                        <#else>
                                                            ${scope.name}
                                                        </#if>
                                                        <button class="close-icon" type="button" name="removePolicyScope-${authorization.resource.id}-${permission.id}-${scope.id}" onclick="removeScopeElm(this.parentNode);document.forms['revokePolicyForm-${authorization.resource.id}-${permission.id}'].submit();"><i class="fa fa-times" aria-hidden="true"></i></button>
                                                        <input type="hidden" name="permission_id" value="${permission.id}:${scope.id}"/>
                                                    </div>
                                                </#list>
                                            <#else>
                                                ${msg("anyAction")}
                                            </#if>
                                        </td>
                                        <td width="20%" align="middle" style="vertical-align: middle">
                                            <a href="#" id="revokePolicy-${authorization.resource.name}-${permission.id}" onclick="document.forms['revokePolicyForm-${authorization.resource.id}-${permission.id}']['action'].value = 'revokePolicyAll';document.forms['revokePolicyForm-${authorization.resource.id}-${permission.id}'].submit();" type="submit" class="btn btn-primary">${msg("doRevoke")}</a>
                                        </td>
                                    </tr>
                                </form>
                            </#list>
                        <#else>
                            <tr>
                                <td colspan="3">
                                    ${msg("resourceNoPermissionsGrantingAccess")}
                                </td>
                            </tr>
                        </#if>
                    </tbody>
                </table>
            </form>
        </div>
    </div>
    <div class="row">
        <div class="col-md-10">
            <h3>
                ${msg("shareWithOthers")}
            </h3>
        </div>
    </div>
    <div class="row">
        <div class="col-md-10">
            <form action="${url.getResourceShare(authorization.resource.id)}" name="shareForm" method="post">
                <div class="col-sm-3 col-md-3">
                    <label for="password" class="control-label">${msg("username")} or ${msg("email")} </label> <span class="required">*</span>
                </div>
                <div class="col-sm-8 col-md-8">
                    <div class="row">
                        <div class="col-md-12">
                            <input type="text" class="form-control" id="user_id" name="user_id" autofocus autocomplete="off">
                        </div>
                        <div class="col-md-12">
                            <br/>
                            <#list authorization.resource.scopes as scope>
                                <div id="scope" class="search-box">
                                    <#if scope.displayName??>
                                        ${scope.displayName}
                                    <#else>
                                        ${scope.name}
                                    </#if>
                                    <button class="close-icon" id="share-remove-scope-${authorization.resource.name}-${scope.name}" type="button" onclick="if (getChildren(this.parentNode.parentNode, 'scope').length > 1) {removeScopeElm(this.parentNode)}"><i class="fa fa-times" aria-hidden="true"></i></button>
                                    <input type="hidden" name="scope_id" value="${scope.id}"/>
                                </div>
                            </#list>
                        </div>
                        <div class="col-md-12">
                            <br/>
                            <a href="#" onclick="document.forms['shareForm'].submit()" type="submit" id="share-button" class="btn btn-primary">${msg("Share")}</a>
                        </div>
                    </div>
                </div>
            </form>
        </div>
    </div>
    <br/>
</@layout.mainLayout>