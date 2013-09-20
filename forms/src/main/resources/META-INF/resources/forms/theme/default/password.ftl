<#import "template-main.ftl" as layout>
<@layout.mainLayout ; section>

    <#if section = "header">

    Change Password

    <#elseif section = "content">

    <form action="${url.passwordUrl}" method="post">
        <div>
            <label for="password">${rb.getString('password')}</label>
            <input type="password" id="password" name="password" />
        </div>
        <div>
            <label for="password-new">${rb.getString('passwordNew')}</label>
            <input type="password" id="password-new" name="password-new" />
        </div>
        <div>
            <label for="password-confirm">${rb.getString('passwordConfirm')}</label>
            <input type="password" id="password-confirm" name="password-confirm" />
        </div>

        <input type="button" value="Cancel" />
        <input type="submit" value="Save" />
    </form>

    </#if>
</@layout.mainLayout>