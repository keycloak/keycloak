<#import "template.ftl" as layout>
<@layout.mainLayout active='social' bodyClass='social'; section>

    <div class="row">
        <div class="col-md-10">
            <h2>Social Accounts</h2>
        </div>
    </div>

    <table>
        <th>
            <td>${event.date}</td>
            <td>${event.event}</td>
            <td>${event.ipAddress}</td>
            <td>${event.clientId}</td>
        </th>

        <#list log.events as event>
            <tr>
                <td>${event.date}</td>
                <td>${event.event}</td>
                <td>${event.ipAddress}</td>
                <td>${event.clientId}</td
            </tr>
        </#list>
    </table>

</@layout.mainLayout>