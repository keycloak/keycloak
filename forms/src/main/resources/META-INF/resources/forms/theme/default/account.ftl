<#import "template-main.ftl" as layout>
<@layout.mainLayout active='account' bodyClass='user'; section>

    <#if section = "header">

    Edit Account

    <#elseif section = "content">
    <p class="subtitle"><span class="required">*</span> Required fields</p>
    <form action="${url.accountUrl}" method="post">
        <fieldset class="border-top">
            <div class="form-group">
                <label for="app-name">${rb.getString('username')}</label>
                <input type="text" id="username" name="username" disabled="disabled" value="${user.username!''}"/>
            </div>
            <div class="form-group">
                <label for="email">${rb.getString('email')}</label><span class="required">*</span>
                <input type="email" id="email" name="email" autofocus value="${user.email!''}"/>
            </div>
            <div class="form-group">
                <label for="last-name">${rb.getString('lastName')}</label><span class="required">*</span>
                <input type="text" id="lastName" name="lastName" value="${user.lastName!''}"/>
            </div>
            <div class="form-group">
                <label for="first-name">${rb.getString('firstName')}</label><span class="required">*</span>
                <input type="text" id="firstName" name="firstName" value="${user.firstName!''}"/>
            </div>
        </fieldset>
        <div class="form-actions">
            <a href="#">« Back to my application</a>
            <button type="submit" class="primary">Save</button>
            <button type="submit">Cancel</button>
        </div>
    </form>

    </#if>
</@layout.mainLayout>