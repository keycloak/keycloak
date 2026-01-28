<#macro assertions org="">
    <#if org?has_content>
        Sign-in to ${org.name} organization
        <#list org.attributes?keys as key>
            The ${key} is ${org.attributes[key]?join(", ")}
        </#list>
        <#if org.member>
            User is member of ${org.name}
        </#if>
    <#else>
        Sign-in to the realm
    </#if>
</#macro>