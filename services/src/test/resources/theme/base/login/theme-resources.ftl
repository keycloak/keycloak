<#-- Keep synchronized with the other theme-resources.ftl copies under themes/. -->
<#macro renderStyles resources pathPrefix>
    <#list resources as resource>
        <link href="${pathPrefix}/${resource.path}" rel="stylesheet"<#if resource.media?has_content> media="${resource.media}"</#if><#if resource.integrity?has_content> integrity="${resource.integrity}"</#if><#if resource.crossorigin?has_content> crossorigin="${resource.crossorigin}"</#if> />
    </#list>
</#macro>

<#macro renderScripts resources pathPrefix defaultScriptType="">
    <#list resources as resource>
        <script src="${pathPrefix}/${resource.path}"<#if resource.type?has_content> type="${resource.type}"<#elseif defaultScriptType?has_content> type="${defaultScriptType}"</#if><#if resource.integrity?has_content> integrity="${resource.integrity}"</#if><#if resource.crossorigin?has_content> crossorigin="${resource.crossorigin}"</#if><#if resource.hasDefer()> defer</#if><#if resource.hasAsync()> async</#if><#if resource.blocking?has_content> blocking="${resource.blocking}"</#if>></script>
    </#list>
</#macro>

<#macro renderFavicons resources pathPrefix>
    <#list resources as resource>
        <link rel="${resource.rel!"icon"}"<#if resource.type?has_content> type="${resource.type}"</#if> href="${pathPrefix}/${resource.path}"<#if resource.media?has_content> media="${resource.media}"</#if> />
    </#list>
</#macro>
