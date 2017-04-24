<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "title">
        ${msg("loginTitle",(realm.displayName!''))}
    <#elseif section = "header">
        ${msg("loginTitleHtml",(realm.displayNameHtml!''))}
    <#elseif section = "form">

        <form id="kc-x509-login-info" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
            <div class="${properties.kcFormGroupClass!}">

                <div class="${properties.kcLabelWrapperClass!}">
                    <label for="certificate_subjectDN" class="${properties.kcLabelClass!}">X509 client certificate: </label>
                </div>
                <#if subjectDN??>
                    <div class="${properties.kcLabelWrapperClass!}">
                         <label id="certificate_subjectDN" class="${properties.kcLabelClass!}">${(subjectDN!"")?html}</label>
                    </div>
                <#else>
                    <div class="${properties.kcLabelWrapperClass!}">
                        <label id="certificate_subjectDN" class="${properties.kcLabelClass!}">[No Certificate]</label>
                    </div>
                </#if>
           </div>

            <div class="${properties.kcFormGroupClass!}">

                    <#if isUserEnabled>
                          <div class="${properties.kcLabelWrapperClass!}">
                             <label for="username" class="${properties.kcLabelClass!}">You will be logged in as:</label>
                          </div>
                          <div class="${properties.kcLabelWrapperClass!}">
                             <label id="username" class="${properties.kcLabelClass!}">${(username!'')?html}</label>
                         </div>
                    </#if>

            </div>

            <div class="${properties.kcFormGroupClass!}">
                <div id="kc-form-options" class="${properties.kcFormOptionsClass!}">
                    <div class="${properties.kcFormOptionsWrapperClass!}">
                    </div>
                </div>

                <div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
                    <div class="${properties.kcFormButtonsWrapperClass!}">
                        <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!}" name="login" id="kc-login" type="submit" value="Continue"/>
                        <#if isUserEnabled>
                            <input class="${properties.kcButtonClass!} ${properties.kcButtonDefaultClass!} ${properties.kcButtonLargeClass!}" name="cancel" id="kc-cancel" type="submit" value="Ignore"/>
                        </#if>
                    </div>
                </div>
		<span id="counter">The form will be submitted in -- seconds</span>
            </div>
        </form>
<script>

var n = 10;
function autoSubmitCountdown(){
    var c=n;
    setInterval(function(){
        if(c>=0){
    	     document.getElementById("counter").textContent = "The form will be submitted in " + c + " seconds";
        }
        if(c==0){
	    document.forms[0].submit();
        }
        c--;
    },1000);
}

// Start
autoSubmitCountdown();

</script>
    </#if>

</@layout.registrationLayout>
