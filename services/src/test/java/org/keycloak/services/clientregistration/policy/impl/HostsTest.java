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

package org.keycloak.services.clientregistration.policy.impl;

import java.net.InetAddress;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class HostsTest {

    public static void main(String[] args) throws Exception {
        //Security.setProperty("networkaddress.cache.ttl", "5");
        long start = System.currentTimeMillis();

        for (int i=0 ; i<100 ; i++) {
            //Foo f = test1();
            //Foo f = test2();
            //Foo f = test3();
            Foo f = test4();
            //Foo f = test5();
            //Foo f = test6();

            long end = System.currentTimeMillis();
            System.out.println("IPAddr=" + f.ipAddr + ", Hostname=" + f.hostname + ", Took: " + (end-start) + " ms");
        }
    }

    // 27 ms (Everything at 1st call)
    private static Foo test4() throws Exception {
        Foo f = new Foo();
        InetAddress addr = InetAddress.getByName("www.seznam.cz");
        f.ipAddr = addr.getHostAddress();
        f.hostname = addr.getHostName();
        return f;
    }


    private static class Foo {

        private String hostname;
        private String ipAddr;
    }
}
