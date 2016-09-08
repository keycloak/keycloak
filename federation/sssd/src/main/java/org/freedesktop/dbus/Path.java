/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus;

public class Path implements Comparable<Path> {
    protected String path;

    public Path(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public String toString() {
        return path;
    }

    public boolean equals(Object other) {
        return (other instanceof Path) && path.equals(((Path) other).path);
    }

    public int hashCode() {
        return path.hashCode();
    }

    public int compareTo(Path that) {
        return path.compareTo(that.path);
    }
}
