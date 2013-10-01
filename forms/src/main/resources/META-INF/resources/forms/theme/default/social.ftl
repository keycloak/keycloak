<#-- TODO: Only a placeholder, implementation needed -->
<#import "template-main.ftl" as layout>
<@layout.mainLayout active='social' bodyClass='social'; section>

    <#if section = "header">

    <h2>Social Accounts</h2>

    <#elseif section = "content">

    <form>
        <fieldset>
            <p class="info">You have the following social accounts associated to your Keycloak account:</p>
            <table class="list">
                <caption>Table of social accounts</caption>
                <tbody>
                <tr>
                    <td class="provider"><span class="social google">Google</span></td>
                    <td class="soft">Connected as john@google.com</td>
                    <td class="action"><a href="#" class="button">Remove Google</a></td>
                </tr>
                <tr>
                    <td class="provider"><span class="social twitter">Twitter</span></td>
                    <td class="soft"></td>
                    <td class="action"><a href="#" class="button">Add Twitter</a></td>
                </tr>
                <tr>
                    <td class="provider"><span class="social facebook">Facebook</span></td>
                    <td class="soft"></td>
                    <td class="action"><a href="#" class="button">Add Facebook</a></td>
                </tr>
                </tbody>
            </table>
        </fieldset>
    </form>

    </#if>
</@layout.mainLayout>