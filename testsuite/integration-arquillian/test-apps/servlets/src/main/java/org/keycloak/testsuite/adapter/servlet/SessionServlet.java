/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.testsuite.adapter.servlet;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@WebServlet("/SessionServlet")
public class SessionServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (req.getRequestURI().endsWith("/logout")) {
            req.logout();
            return;
        }

        String counter;
        String counterWrapperValue;
        if (req.getRequestURI().endsWith("/donotincrease")) {
            counter = getCounter(req);
            counterWrapperValue = getCounterWrapper(req);
        } else {
            counter = increaseAndGetCounter(req);
            counterWrapperValue = increaseAndGetCounterWrapper(req);
        }

        resp.setContentType("text/html");
        PrintWriter pw = resp.getWriter();
        pw.printf("<html><head><title>%s</title></head><body>", "Session Test");
        pw.printf("Counter=%s<br>", counter);
        pw.printf("CounterWrapper=%s<br>", counterWrapperValue);
        pw.printf("Node name=%s<br>", System.getProperty("jboss.node.name", "property not specified"));
        pw.print("</body></html>");
        pw.flush();


    }

    private String getCounter(HttpServletRequest req) {
        HttpSession session = req.getSession();
        return String.valueOf(session.getAttribute("counter"));
    }

    private String increaseAndGetCounter(HttpServletRequest req) {
        HttpSession session = req.getSession();
        Integer counter = (Integer)session.getAttribute("counter");
        counter = (counter == null) ? 1 : counter + 1;
        session.setAttribute("counter", counter);
        return String.valueOf(counter);
    }

    private String getCounterWrapper(HttpServletRequest req) {
        HttpSession session = req.getSession();
        return String.valueOf(session.getAttribute("counterWrapper"));
    }

    private String increaseAndGetCounterWrapper(HttpServletRequest req) {
        HttpSession session = req.getSession();
        CounterWrapper counter = (CounterWrapper)session.getAttribute("counterWrapper");
        counter = (counter == null) ? new CounterWrapper() : counter.increase();
        session.setAttribute("counterWrapper", counter);
        return String.valueOf(counter);
    }


    // This is just to test that custom class can be added as an attribute to the HttpSession
    public static class CounterWrapper implements Serializable {

        private int counter = 1;

        @Override
        public String toString() {
            return String.valueOf(counter);
        }

        public CounterWrapper increase() {
            counter = counter + 1;
            return this;
        }
    }
}
