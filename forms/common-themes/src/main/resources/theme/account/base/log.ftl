<#import "template.ftl" as layout>
<@layout.mainLayout active='log' bodyClass='log'; section>

    <div class="row">
        <div class="col-md-10">
            <h2>Account Log</h2>
        </div>
    </div>

    <table class="table table-striped table-bordered">
        <thead>
        <tr>
            <td>Date</td>
            <td>Event</td>
            <td>IP</td>
            <td>Client</td>
            <td>Details</td>
        </tr>
        </thead>

        <tbody>
        <#list log.events as event>
            <tr>
                <td>${event.date?datetime}</td>
                <td>${event.event}</td>
                <td>${event.ipAddress}</td>
                <td>${event.client!}</td>
                <td><#list event.details as detail>${detail.key} = ${detail.value} <#if detail_has_next>, </#if></#list></td>
            </tr>
        </#list>
        </tbody>

    </table>

</@layout.mainLayout>