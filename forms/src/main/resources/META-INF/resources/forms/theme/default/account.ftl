<#import "template-main.ftl" as layout>
<@layout.mainLayout ; section>

    <#if section = "header">

    Edit Account

    <#elseif section = "content">

    <form action="${url.accountUrl}" method="post">
        <div>
            <label for="firstName">${rb.getString('firstName')}</label>
            <input type="text" id="firstName" name="firstName" value="${user.firstName?default('')}" />
        </div>
        <div>
            <label for="lastName">${rb.getString('lastName')}</label>
            <input type="text" id="lastName" name="lastName" value="${user.lastName?default('')}" />
        </div>
        <div>
            <label for="email">${rb.getString('email')}</label>
            <input type="text" id="email" name="email" value="${user.email?default('')}" />
        </div>
        <div>
            <label for="username">${rb.getString('username')}</label>
            <input type="text" id="username" name="username" value="${user.username?default('')}" disabled="true" />
        </div>

        <input type="button" value="Cancel" />
        <input type="submit" value="Save" />
    </form>

    </#if>
</@layout.mainLayout>