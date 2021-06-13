<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "header">
        <script>
            document.title =  "${msg("frontchannel-logout.title")}";
        </script>
        ${msg("frontchannel-logout.title")}
    <#elseif section = "form">
        <p>${msg("frontchannel-logout.message")}</p>
        <#if frontchannelLogout.urls?has_content>
            <ul>
            <#list frontchannelLogout.urls[0]?split(' ') as url>
                <li>
                    <iframe src="${url}" style="width:1px; height:1px;"></iframe>
                    <img src="${url}" style="width:1px; height:1px;">
                </li>
            </#list>
            </ul>
        </#if>
        <#if frontchannelLogout.redirectUri?has_content>
            <script>
                function readystatechange(event){
                    if(document.readyState=='complete'){
                        window.location.replace('${frontchannelLogout.redirectUri[0]}');
                    }
                }
                document.addEventListener('readystatechange', readystatechange);
            </script>
            <a id="continue" class="btn btn-primary" href="${frontchannelLogout.redirectUri[0]}">${msg("doContinue")}</a>
        </#if>
    </#if>
</@layout.registrationLayout>
