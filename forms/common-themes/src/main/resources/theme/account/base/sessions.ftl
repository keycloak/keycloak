<#import "template.ftl" as layout>
<@layout.mainLayout active='sessions' bodyClass='sessions'; section>

    <div class="row">
        <div class="col-md-10">
            <h2>Sessions</h2>
        </div>
    </div>

    <table class="table table-striped table-bordered">
        <thead>
        <tr>
            <td>IP</td>
            <td>Started</td>
            <td>Last Access</td>
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
                <td>${session.lastAccess?datetime}</td>
                <td>${session.expires?datetime}</td>
                <td>
                    <#list session.applications as app>
                        ${app}<br/>
                    </#list>
                </td>
                <td>
                    <#list session.clients as client>
                        ${client}<br/>
                    </#list>
                </td>
            </tr>
        </#list>
        </tbody>

    </table>

    <a id="logout-all-sessions" href="${url.sessionsLogoutUrl}">${msg("doLogOutAllSessions")}</a>

</@layout.mainLayout>
