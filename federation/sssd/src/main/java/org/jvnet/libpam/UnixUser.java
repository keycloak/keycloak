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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.sun.jna.Memory;
import com.sun.jna.ptr.IntByReference;
import org.jvnet.libpam.impl.CLibrary.group;
import org.jvnet.libpam.impl.CLibrary.passwd;

import static org.jvnet.libpam.impl.CLibrary.libc;

/**
 * Represents an Unix user. Immutable.
 *
 * @author Kohsuke Kawaguchi
 */
public class UnixUser {
    private final String userName, gecos, dir, shell;
    private final int uid, gid;
    private final Set<String> groups;

    /*package*/ UnixUser(String userName, passwd pwd) throws PAMException {
        this.userName = userName;
        this.gecos = pwd.getPwGecos();
        this.dir = pwd.getPwDir();
        this.shell = pwd.getPwShell();
        this.uid = pwd.getPwUid();
        this.gid = pwd.getPwGid();

        int sz = 4; /*sizeof(gid_t)*/

        int ngroups = 64;
        Memory m = new Memory(ngroups * sz);
        IntByReference pngroups = new IntByReference(ngroups);
        try {
            if (libc.getgrouplist(userName, pwd.getPwGid(), m, pngroups) < 0) {
                // allocate a bigger memory
                m = new Memory(pngroups.getValue() * sz);
                if (libc.getgrouplist(userName, pwd.getPwGid(), m, pngroups) < 0)
                    // shouldn't happen, but just in case.
                    throw new PAMException("getgrouplist failed");
            }
            ngroups = pngroups.getValue();
        } catch (LinkageError e) {
            // some platform, notably Solaris, doesn't have the getgrouplist function
            ngroups = libc._getgroupsbymember(userName, m, ngroups, 0);
            if (ngroups < 0)
                throw new PAMException("_getgroupsbymember failed");
        }

        groups = new HashSet<String>();
        for (int i = 0; i < ngroups; i++) {
            int gid = m.getInt(i * sz);
            group grp = libc.getgrgid(gid);
            if (grp == null) {
                continue;
            }
            groups.add(grp.gr_name);
        }
    }

    public UnixUser(String userName) throws PAMException {
        this(userName, passwd.loadPasswd(userName));
    }

    /**
     * Copy constructor for mocking. Not intended for regular use. Only for testing.
     * This signature may change in the future.
     */
    protected UnixUser(String userName, String gecos, String dir, String shell, int uid, int gid, Set<String> groups) {
        this.userName = userName;
        this.gecos = gecos;
        this.dir = dir;
        this.shell = shell;
        this.uid = uid;
        this.gid = gid;
        this.groups = groups;
    }

    /**
     * Gets the unix account name. Never null.
     */
    public String getUserName() {
        return userName;
    }

    /**
     * Gets the UID of this user.
     */
    public int getUID() {
        return uid;
    }

    /**
     * Gets the GID of this user.
     */
    public int getGID() {
        return gid;
    }

    /**
     * Gets the gecos (the real name) of this user.
     */
    public String getGecos() {
        return gecos;
    }

    /**
     * Gets the home directory of this user.
     */
    public String getDir() {
        return dir;
    }

    /**
     * Gets the shell of this user.
     */
    public String getShell() {
        return shell;
    }

    /**
     * Gets the groups that this user belongs to.
     *
     * @return never null.
     */
    public Set<String> getGroups() {
        return Collections.unmodifiableSet(groups);
    }

    public static boolean exists(String name) {
        return libc.getpwnam(name) != null;
    }
}
