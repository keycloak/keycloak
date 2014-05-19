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
            <td>Applications</td>
            <td>Clients</td>
        </tr>
        </thead>

        <tbody>
        <#list sessions.sessions as session>
            <tr>
                <td>${session.ipAddress}</td>
                <td>${session.started?datetime}</td>
                <td>${session.expires?datetime}</td>
                <td>
                    <ul style="list-style: none; ">
                        <#list session.applications as app>
                            <li>${app}</li>
                        </#list>
                    </ul>
                </td>
                <td>
                    <ul style="list-style: none; ">
                        <#list session.clients as client>
                            <li>${client}</li>
                        </#list>
                    </ul>
                </td>
            </tr>
        </#list>
        </tbody>

    </table>

    <a id="logout-all-sessions" href="${url.sessionsLogoutUrl}">Logout all sessions</a>

</@layout.mainLayout>