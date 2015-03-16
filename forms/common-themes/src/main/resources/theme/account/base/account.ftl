<#import "template.ftl" as layout>
<@layout.mainLayout active='account' bodyClass='user'; section>

    <div class="row">
        <div class="col-md-10">
            <h2>Edit Account</h2>
        </div>
        <div class="col-md-2 subtitle">
            <span class="subtitle"><span class="required">*</span> Required fields</span>
        </div>
    </div>

    <form action="${url.accountUrl}" class="form-horizontal" method="post">

        <input type="hidden" id="stateChecker" name="stateChecker" value="${stateChecker}">

        <div class="form-group">
            <div class="col-sm-2 col-md-2">
                <label for="username" class="control-label">${msg("username")}</label>
            </div>

            <div class="col-sm-10 col-md-10">
                <input type="text" class="form-control" id="username" name="username" disabled="disabled" value="${(account.username!'')?html}"/>
            </div>
        </div>

        <div class="form-group">
            <div class="col-sm-2 col-md-2">
            <label for="email" class="control-label">${msg("email")}</label> <span class="required">*</span>
            </div>

            <div class="col-sm-10 col-md-10">
                <input type="text" class="form-control" id="email" name="email" autofocus value="${(account.email!'')?html}"/>
            </div>
        </div>


        <div class="form-group">
            <div class="col-sm-2 col-md-2">
                <label for="lastName" class="control-label">${msg("lastName")}</label> <span class="required">*</span>
            </div>

            <div class="col-sm-10 col-md-10">
                <input type="text" class="form-control" id="lastName" name="lastName" value="${(account.lastName!'')?html}"/>
            </div>
        </div>

        <div class="form-group">
            <div class="col-sm-2 col-md-2">
                <label for="firstName" class="control-label">${msg("firstName")}</label> <span class="required">*</span>
            </div>

            <div class="col-sm-10 col-md-10">
                <input type="text" class="form-control" id="firstName" name="firstName" value="${(account.firstName!'')?html}"/>
            </div>
        </div>

        <div class="form-group">
            <div class="col-sm-2 col-md-2">
                <label for="user.attributes.street" class="control-label">${msg("street")}</label>
            </div>

            <div class="col-sm-10 col-md-10">
                <input type="text" class="form-control" id="user.attributes.street" name="user.attributes.street" value="${(account.attributes.street!'')?html}"/>
            </div>
        </div>
        <div class="form-group">
            <div class="col-sm-2 col-md-2">
                <label for="user.attributes.locality" class="control-label">${msg("locality")}</label>
            </div>

            <div class="col-sm-10 col-md-10">
                <input type="text" class="form-control" id="user.attributes.locality" name="user.attributes.locality" value="${(account.attributes.locality!'')?html}"/>
            </div>
        </div>
        <div class="form-group">
            <div class="col-sm-2 col-md-2">
                <label for="user.attributes.region" class="control-label">${msg("region")}</label>
            </div>

            <div class="col-sm-10 col-md-10">
                <input type="text" class="form-control" id="user.attributes.region" name="user.attributes.region" value="${(account.attributes.region!'')?html}"/>
            </div>
        </div>
        <div class="form-group">
            <div class="col-sm-2 col-md-2">
                <label for="user.attributes.postal_code" class="control-label">${msg("postal_code")}</label>
            </div>

            <div class="col-sm-10 col-md-10">
                <input type="text" class="form-control" id="user.attributes.postal_code" name="user.attributes.postal_code" value="${(account.attributes.postal_code!'')?html}"/>
            </div>
        </div>
        <div class="form-group">
            <div class="col-sm-2 col-md-2">
                <label for="user.attributes.country" class="control-label">${msg("country")}</label>
            </div>

            <div class="col-sm-10 col-md-10">
                <input type="text" class="form-control" id="user.attributes.country" name="user.attributes.country" value="${(account.attributes.country!'')?html}"/>
            </div>
        </div>

        <div class="form-group">
            <div id="kc-form-buttons" class="col-md-offset-2 col-md-10 submit">
                <div class="">
                    <#if url.referrerURI??><a href="${url.referrerURI}">Back to application</a></#if>
                    <button type="submit" class="btn btn-primary btn-lg" name="submitAction" value="Save">Save</button>
                    <button type="submit" class="btn btn-default btn-lg" name="submitAction" value="Cancel">Cancel</button>
                </div>
            </div>
        </div>
    </form>

</@layout.mainLayout>