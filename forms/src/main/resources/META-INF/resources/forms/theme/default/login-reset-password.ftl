<#import "template-login-action.ftl" as layout>
<@layout.registrationLayout bodyClass=""; section>
    <#if section = "title">

    Reset password

    <#elseif section = "header">

    Reset password

    <#elseif section = "form">

    <div name="form">
        <form action="${url.passwordResetUrl}" method="post">
            <div>
                <label for="username">${rb.getString('username')}</label>
                <input id="username" name="username" type="text" />
            </div>
        	<div>
      	    	<label for="email">${rb.getString('email')}</label>
            	<input type="text" id="email" name="email" />
			</div>

            <input class="btn-primary" type="submit" value="Submit" />
        </form>
    </div>

    <#elseif section = "info" >

    <div name="info">
    </div>

    </#if>
</@layout.registrationLayout>