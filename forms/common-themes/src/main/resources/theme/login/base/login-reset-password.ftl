<#import "template.ftl" as layout>
<@layout.registrationLayout bodyClass=""; section>
    <#if section = "title">

    ${rb.emailForgotHeader}

    <#elseif section = "header">

    ${rb.emailForgotHeader}

    <#elseif section = "form">

    <div id="form">
        <p class="instruction">${rb.emailInstruction}</p>
        <form action="${url.loginPasswordResetUrl}" method="post">
        	<div>
      	    	<label for="email">${rb.email}</label><input type="text" id="email" name="email" />
			</div>
            <input class="btn-primary" type="submit" value="Submit" />
        </form>
    </div>
    <#elseif section = "info" >
        <p><a href="${url.loginUrl}">&laquo; Back to Login</a></p>
    </#if>
</@layout.registrationLayout>