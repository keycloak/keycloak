/*
 * The MIT License
 *
 * Copyright (c) 2009, Sun Microsystems, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.jvnet.libpam;

import junit.framework.TestCase;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class InteractiveTester extends TestCase {
    public InteractiveTester(String testName) {
        super(testName);
    }

    public void testPositiveCase() throws Exception {
        for (int i = 0; i < 1000; i++)
            testOne();
    }

    public void testOne() throws Exception {
        UnixUser u = new PAM("sshd").authenticate(System.getProperty("user.name"), System.getProperty("password"));
        if (!printOnce) {
            System.out.println(u.getUID());
            System.out.println(u.getGroups());
            printOnce = true;
        }
    }

    public void testGetGroups() throws Exception {
        System.out.println(new PAM("sshd").getGroupsOfUser(System.getProperty("user.name")));
    }

    public void testConcurrent() throws Exception {
        ExecutorService es = Executors.newFixedThreadPool(10);
        Set<Future<?>> result = new HashSet<Future<?>>();
        for (int i = 0; i < 1000; i++) {
            result.add(es.submit(new Callable<Object>() {
                public Object call() throws Exception {
                    testOne();
                    return null;
                }
            }));
        }
        // wait for completion
        for (Future<?> f : result) {
            f.get();
        }
        es.shutdown();
    }

    public void testNegative() throws Exception {
        try {
            new PAM("sshd").authenticate("bogus", "bogus");
            fail("expected a failure");
        } catch (PAMException e) {
            // yep
        }
    }

    public static void main(String[] args) throws Exception {
        UnixUser u = new PAM("sshd").authenticate(args[0], args[1]);
        System.out.println(u.getUID());
        System.out.println(u.getGroups());
        System.out.println(u.getGecos());
        System.out.println(u.getDir());
        System.out.println(u.getShell());
    }

    private boolean printOnce;
}
