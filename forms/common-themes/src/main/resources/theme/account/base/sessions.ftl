<#import "template.ftl" as layout>
<@layout.mainLayout active='sessions' bodyClass='sessions'; section>

    <div class="row">
        <div class="col-md-10">
            <h2>Sessions</h2>
        </div>
    </div>

    <table class="table">
        <thead>
        <tr>
            <td>IP</td>
            <td>Started</td>
            <td>Expires</td>
        </tr>
        </thead>

        <tbody>
        <#list sessions.sessions as session>
            <tr>
                <td>${session.ipAddress}</td>
                <td>${session.started?datetime}</td>
                <td>${session.expires?datetime}</td>
            </tr>
        </#list>
        </tbody>

    </table>

    <a id="logout-all-sessions" href="${url.sessionsLogoutUrl}">Logout all sessions</a>

</@layout.mainLayout>