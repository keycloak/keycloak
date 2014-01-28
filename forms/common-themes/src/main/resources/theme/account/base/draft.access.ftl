<#-- TODO: Only a placeholder, implementation needed -->
<#import "template.ftl" as layout>
<@layout.mainLayout active='access' bodyClass='access'; section>

    <#if section = "header">

    <h2>Manage Authorised Access</h2>

    <#elseif section = "content">

    <p class="info">Services requested access to your following accounts:</p>
    <table class="list">
        <caption>Table of services</caption>
        <tbody>
        <tr class="collapsed">
            <td class="provider"><a href="#eventjuggler">EventJuggler</a></td>
            <td class="soft">3 services accessing</td>
        </tr>
        <tr class="expanded hidden" id="#eventjuggler">
            <td colspan="2">
                <span class="provider">EventJuggler</span>
                <p>You have granted the following services access to your EventJuggler account:</p>
                <form>
                    <ul>
                        <li>
                            <span class="item">Event Announcer - Events info</span>
                            <button class="link"><span class="icon-cancel-circle">Icon: remove</span>Revoke Access</button>
                        </li>
                        <li>
                            <span class="item">Facebook - Events info</span>
                            <button class="link"><span class="icon-cancel-circle">Icon: remove</span>Revoke Access</button>
                        </li>
                        <li>
                            <span class="item">Event Announcer - Profile info</span>
                            <span class="status red">Access revoked</span>
                            <button class="link">Undo</button>
                        </li>
                    </ul>
                    <div class="form-actions">
                        <button type="submit" class="primary">Save</button>
                        <button type="submit">Cancel</button>
                    </div>
                </form>
            </td>
        </tr>
        <tr class="collapsed">
            <td class="provider"><a href="#another-service">Another Service</a></td>
            <td class="soft">5 services accessing</td>
        </tr>
        <tr class="expanded hidden" id="another-service">
            <td colspan="2">
                <span class="provider">EventJuggler</span>
                <p>You have granted the following services access to your EventJuggler account:</p>
                <form>
                    <ul>
                        <li>
                            <span class="item">Event Announcer - Events info</span>
                            <button class="link"><span class="icon-cancel-circle">Icon: remove</span>Revoke Access</button>
                        </li>
                        <li>
                            <span class="item">Facebook - Events info</span>
                            <button class="link"><span class="icon-cancel-circle">Icon: remove</span>Revoke Access</button>
                        </li>
                        <li>
                            <span class="item">Event Announcer - Profile info</span>
                            <span class="status red">Access revoked</span>
                            <button class="link">Undo</button>
                        </li>
                    </ul>
                    <div class="form-actions">
                        <button type="submit" class="primary">Save</button>
                        <button type="submit">Cancel</button>
                    </div>
                </form>

            </td>
        </tr>
        </tbody>
    </table>

    </#if>
</@layout.mainLayout>