<#import "template-login-action.ftl" as layout>
<@layout.registrationLayout bodyClass=""; section>
    <#if section = "title">

    Update Account Information

    <#elseif section = "header">

    <h2>Update Account Information</h2>

    <#elseif section = "feedback">
    <div class="feedback warning show">
        <p><strong>Your account is not enabled because you need to update your account information.</strong><br>Please follow the steps below.</p>
    </div>
    <#elseif section = "form">

    <div id="form">
        <form action="${url.loginUpdateProfileUrl}" method="post">
            <div class="feedback error bottom-left">
                <p><strong>Some required fields are empty or incorrect.</strong><br>Please correct the fields in red.</p>
            </div>
            <p class="subtitle">All fields required</p>
	        <div>
    	        <label for="email">${rb.getString('email')}</label>
            	<input type="text" id="email" name="email" value="${user.email!''}" />
	        </div>
            <div>
                <label for="firstName">${rb.getString('firstName')}</label>
                <input type="text" id="firstName" name="firstName" value="${user.firstName!''}" />
            </div>
            <div>
                <label for="lastName">${rb.getString('lastName')}</label>
                <input type="text" id="lastName" name="lastName" value="${user.lastName!''}" />
            </div>

            <div class="aside-btn">
            </div>

            <input class="btn-primary" type="submit" value="Submit" />
        </form>
    </div>
    </#if>
</@layout.registrationLayout>