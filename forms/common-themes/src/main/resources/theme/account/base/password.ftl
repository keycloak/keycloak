<#import "template.ftl" as layout>
<@layout.mainLayout active='password' bodyClass='password'; section>

    <div class="row">
        <div class="col-md-10">
            <h2>Change Password</h2>
        </div>
        <div class="col-md-2 subtitle">
            <span class="subtitle">All fields required</span>
        </div>
    </div>

    <form action="${url.passwordUrl}" class="form-horizontal" method="post">
        <#if password.passwordSet>
            <div class="form-group">
                <div class="col-sm-2 col-md-2">
                    <label for="password" class="control-label">${msg("password")}</label>
                </div>

                <div class="col-sm-10 col-md-10">
                    <input type="password" class="form-control" id="password" name="password" autofocus>
                </div>
            </div>
        </#if>

        <input type="hidden" id="stateChecker" name="stateChecker" value="${stateChecker}">

        <div class="form-group">
            <div class="col-sm-2 col-md-2">
                <label for="password-new" class="control-label">${msg("passwordNew")}</label>
            </div>

            <div class="col-sm-10 col-md-10">
                <input type="password" class="form-control" id="password-new" name="password-new">
            </div>
        </div>

        <div class="form-group">
            <div class="col-sm-2 col-md-2">
                <label for="password-confirm" class="control-label" class="two-lines">${msg("passwordConfirm")}</label>
            </div>

            <div class="col-sm-10 col-md-10">
                <input type="password" class="form-control" id="password-confirm" name="password-confirm">
            </div>
        </div>

        <div class="form-group">
            <div id="kc-form-buttons" class="col-md-offset-2 col-md-10 submit">
                <div class="">
                    <button type="submit" class="btn btn-primary btn-lg" name="submitAction" value="Save">Save</button>
                    <button type="submit" class="btn btn-default btn-lg" name="submitAction" value="Cancel">Cancel</button>
                </div>
            </div>
        </div>
    </form>

</@layout.mainLayout>