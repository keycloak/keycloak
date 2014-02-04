<#import "template.ftl" as layout>
<@layout.mainLayout active='password' bodyClass='password'; section>

    <h2 class="pull-left">Change Password</h2>

    <p class="subtitle">All fields required</p>

    <form action="${url.passwordUrl}" method="post">
        <fieldset class="border-top">
            <div class="form-group">
                <label for="password">${rb.password}</label>
                <input type="password" id="password" name="password" autofocus>
            </div>
            <div class="form-group">
                <label for="password-new">${rb.passwordNew}</label>
                <input type="password" id="password-new" name="password-new">
            </div>
            <div class="form-group">
                <label for="password-confirm" class="two-lines">${rb.passwordConfirm}</label>
                <input type="password" id="password-confirm" name="password-confirm">
            </div>
        </fieldset>
        <div class="form-actions">
            <#if url.referrerURI??><a href="${url.referrerURI}">Back to application</a></#if>
            <button type="submit" class="primary">Save</button>
            <button type="submit">Cancel</button>
        </div>
    </form>

</@layout.mainLayout>