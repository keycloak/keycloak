<#import "template-login-action.ftl" as layout>
<@layout.registrationLayout bodyClass="reset"; section>
    <#if section = "title">

    ${rb.getString('emailForgotHeader')}

    <#elseif section = "header">

    ${rb.getString('emailForgotHeader')}

    <#elseif section = "form">

    <div id="form">
        <p class="instruction">${rb.getString('emailInstruction')}</p>
        <form action="${url.loginPasswordResetUrl}" method="post">
            <div>
                <label for="username">${rb.getString('username')}</label><input id="username" name="username" type="text" />
            </div>
        	<div>
      	    	<label for="email">${rb.getString('email')}</label><input type="text" id="email" name="email" />
			</div>
            <input class="btn-primary" type="submit" value="Submit" />
        </form>
    </div>
    <#elseif section = "info" >

    <div id="info">
    </div>

    </#if>
</@layout.registrationLayout>