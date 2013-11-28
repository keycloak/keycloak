<#import "template-main.ftl" as layout>
<@layout.mainLayout active='password' bodyClass='password'; section>

    <#if section = "header">
    Change Password

    <#elseif section="content">
    <p class="subtitle">All fields required</p>
    <form action="${url.passwordUrl}" method="post">
        <fieldset class="border-top">
            <p class="info">Password updated 2 days ago by Admin.</p>
            <div class="form-group">
                <label for="password">${rb.getString('password')}</label>
                <input type="password" id="password" name="password" autofocus>
            </div>
            <div class="form-group">
                <label for="password-new">${rb.getString('passwordNew')}</label>
                <input type="password" id="password-new" name="password-new">
            </div>
            <div class="form-group">
                <label for="password-confirm" class="two-lines">${rb.getString('passwordConfirm')}</label>
                <input type="password" id="password-confirm" name="password-confirm">
            </div>
        </fieldset>
        <div class="form-actions">
            <#if url.referrerURI??><a href="${url.referrerURI}">Back to application</a></#if>
            <button type="submit" class="primary">Save</button>
            <button type="submit">Cancel</button>
        </div>
    </form>
    </#if>

</@layout.mainLayout>