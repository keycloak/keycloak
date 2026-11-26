<#import "template.ftl" as layout>
<#import "buttons.ftl" as buttons>
<@layout.registrationLayout; section>
    <#if section = "header">
        ${msg("frontchannel-logout.title")}
    <#elseif section = "form">
        <div class="${properties.kcFormClass!}">
            <div class="${properties.kcFormGroupClass!}">
                <p>${msg("frontchannel-logout.message")}</p>
                <ul class="${properties.kcListClass!}">
                    <#list logout.clients as client>
                        <li>
                            ${client.name}
                            <iframe src="${client.frontChannelLogoutUrl}" style="display:none;"></iframe>
                        </li>
                    </#list>
                </ul>
            </div>
            <#if logout.logoutRedirectUri?has_content>
                <script>
                    <#outputformat "JavaScript">
                    function readystatechange(event) {
                        if (document.readyState=='complete') {
                            window.location.replace(${logout.logoutRedirectUri?c});
                        }
                    }
                    document.addEventListener('readystatechange', readystatechange);
                    </#outputformat>
                </script>
                <@buttons.buttonLink id="continue" href=logout.logoutRedirectUri label="doContinue" class=["kcButtonPrimaryClass", "kcButtonBlockClass"] />
            </#if>
        </div>
    </#if>
</@layout.registrationLayout>
