<#macro emailLayout>
<html>
<body>
    <p>Dear ${user.getFirstName()},</p>
    <#nested>
    <div style="line-height: 0.4rem;margin-top: 1.5rem;">
        <p>Best regards,</p>
        <p>Emeritus Insights</p>
    </div>
</body>
</html>
</#macro>
