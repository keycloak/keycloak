/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.keycloak.testsuite.pages;

import org.keycloak.services.resources.flows.Urls;
import org.keycloak.testsuite.Constants;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import javax.ws.rs.core.UriBuilder;
import java.util.LinkedList;
import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class AccountLogPage extends AbstractAccountPage {

    private static String PATH = Urls.accountLogPage(UriBuilder.fromUri(Constants.AUTH_SERVER_ROOT).build(), "test").toString();

    public boolean isCurrent() {
        return driver.getTitle().contains("Account Management") && driver.getCurrentUrl().endsWith("/account/log");
    }

    public void open() {
        driver.navigate().to(PATH);
    }

    public List<List<String>> getEvents() {
        List<List<String>> table = new LinkedList<List<String>>();
        for (WebElement r : driver.findElements(By.tagName("tr"))) {
            List<String> row = new LinkedList<String>();
            for (WebElement col : r.findElements(By.tagName("td"))) {
                row.add(col.getText());
            }
            table.add(row);
        }
        table.remove(0);
        return table;
    }

}
