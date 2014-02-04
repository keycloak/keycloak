<#import "template.ftl" as layout>
<@layout.mainLayout active='account' bodyClass='user'; section>

    <h2 class="pull-left">Edit Account</h2>

    <p class="subtitle"><span class="required">*</span> Required fields</p>

    <form action="${url.accountUrl}" method="post">
        <fieldset class="border-top">
            <div class="form-group">
                <label for="username">${rb.username}</label>
                <input type="text" id="username" name="username" disabled="disabled" value="${account.username!''}"/>
            </div>
            <div class="form-group">
                <label for="email">${rb.email}</label><span class="required">*</span>
                <input type="text" id="email" name="email" autofocus value="${account.email!''}"/>
            </div>
            <div class="form-group">
                <label for="lastName">${rb.lastName}</label><span class="required">*</span>
                <input type="text" id="lastName" name="lastName" value="${account.lastName!''}"/>
            </div>
            <div class="form-group">
                <label for="firstName">${rb.firstName}</label><span class="required">*</span>
                <input type="text" id="firstName" name="firstName" value="${account.firstName!''}"/>
            </div>
        </fieldset>
        <div class="form-actions">
            <#if url.referrerURI??><a href="${url.referrerURI}">Back to application</a></#if>
            <button type="submit" class="primary">Save</button>
            <button type="submit">Cancel</button>
        </div>
    </form>

</@layout.mainLayout>