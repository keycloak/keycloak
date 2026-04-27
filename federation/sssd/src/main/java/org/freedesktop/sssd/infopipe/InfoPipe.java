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

package org.freedesktop.sssd.infopipe;

import java.util.List;
import java.util.Map;

import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.annotations.DBusMemberName;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.types.Variant;

/**
 * @author <a href="mailto:bruno@abstractj.org">Bruno Oliveira</a>.
 */
@DBusInterfaceName("org.freedesktop.sssd.infopipe")
public interface InfoPipe extends DBusInterface {

    String OBJECTPATH = "/org/freedesktop/sssd/infopipe";
    String BUSNAME = "org.freedesktop.sssd.infopipe";

    @DBusMemberName("GetUserAttr")
    Map<String, Variant> getUserAttributes(String user, List<String> attr);

    @DBusMemberName("GetUserGroups")
    List<String> getUserGroups(String user);

    @DBusMemberName("Ping")
    String ping(String ping);

}