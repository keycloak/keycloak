<#import "template-login-action.ftl" as layout>
<@layout.registrationLayout bodyClass=""; section>
    <#if section = "title">

    Update profile

    <#elseif section = "header">

    Update profile

    <#elseif section = "form">

    <div name="form">
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

            <div class="aside-btn">
            </div>

            <input class="btn-primary" type="submit" value="Submit" />
        </form>
    </div>

    <#elseif section = "info" >

    <div name="info">
    </div>

    </#if>
</@layout.registrationLayout>