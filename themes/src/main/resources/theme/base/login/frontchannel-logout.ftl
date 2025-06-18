<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "header">
        <script>
            document.title =  "${msg("frontchannel-logout.title")}";
        </script>
        ${msg("frontchannel-logout.title")}
    <#elseif section = "form">
        <p>${msg("frontchannel-logout.message")}</p>
        <ul>
        <#list logout.clients as client>
            <li>
                ${client.name}
                <iframe src="${client.frontChannelLogoutUrl}" style="display:none;"></iframe>
            </li>
        </#list>
        </ul>
        <#if logout.logoutRedirectUri?has_content>
            <script>
                function readystatechange(event) {
                    if (document.readyState=='complete') {
                        window.location.replace('${logout.logoutRedirectUri}');
                    }
                }
                document.addEventListener('readystatechange', readystatechange);
            </script>
            <a id="continue" class="btn btn-primary" href="${logout.logoutRedirectUri}">${msg("doContinue")}</a>
        </#if>
    </#if>
</@layout.registrationLayout>
