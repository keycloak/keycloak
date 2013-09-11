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
package org.keycloak.social;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class SocialRequestManager {

    public static final long TIMEOUT = 10 * 60 * 1000;

    private Map<String, RequestDetails> map = new HashMap<String, RequestDetails>();
    
    private LinkedHashMap<String, Long> expires = new LinkedHashMap<String, Long>();

    public synchronized void addRequest(String requestId, RequestDetails request) {
        pruneExpired();

        map.put(requestId, request);
        expires.put(requestId, System.currentTimeMillis() + TIMEOUT);
    }

    public synchronized boolean isRequestId(String requestId) {
        return map.containsKey(requestId);
    }

    public synchronized RequestDetails retrieveData(String requestId) {
        expires.remove(requestId);
        RequestDetails details = map.remove(requestId);

        pruneExpired();

        return details;
    }

    // Just obtain data without expiring it
    public synchronized RequestDetails getData(String requestId) {
        return map.get(requestId);
    }
    
    private void pruneExpired() {
        long currentTime = System.currentTimeMillis();
        Iterator<Entry<String, Long>> itr = expires.entrySet().iterator();
        while (itr.hasNext()) {
            Entry<String, Long> e = itr.next();
            if (e.getValue() < currentTime) {
                itr.remove();
                map.remove(e.getKey());
            } else {
                return;
            }
        }
    }

}
