<#import "template.ftl" as layout>
<@layout.emailLayout>
${renderedHtmlBody!""?no_esc}
</@layout.emailLayout>