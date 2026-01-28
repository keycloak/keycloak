package org.freedesktop.dbus.interfaces;

import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.types.UInt32;

@DBusInterfaceName("org.freedesktop.DBus.Monitoring.BecomeMonitor")
@SuppressWarnings({"checkstyle:methodname"})
public interface Monitoring {
    /**
     * Converts the connection into a <span class="emphasis"><em>monitor connection</em></span> which can be used as a
     * debugging/monitoring tool. Only a user who is privileged on this bus (by some implementation-specific definition)
     * may create monitor
     * connections<a href="https://dbus.freedesktop.org/doc/dbus-specification.html#ftn.idm3162" id="idm3162"><sup
     * class="footnote">[5]</sup></a>.
     * <p>
     * Monitor connections lose all their bus names, including the unique connection name, and all their match rules.
     * Sending messages on a monitor connection is not allowed: applications should use a private connection for
     * monitoring.
     * </p>
     * <p>
     * Monitor connections may receive all messages, even messages that should only have gone to some other connection
     * ("eavesdropping"). The first argument is a list of match rules, which replace any match rules that were
     * previously active for this connection. These match rules are always treated as if they contained the special
     * <code>eavesdrop='true'</code> member.
     * </p>
     * <p>
     * As a special case, an empty list of match rules (which would otherwise match nothing, making the monitor useless)
     * is treated as a shorthand for matching all messages.
     * </p>
     * <p>
     * The second argument might be used for flags to influence the behaviour of the monitor connection in future D-Bus
     * versions.
     * </p>
     * <p>
     * Message bus implementations should attempt to minimize the side-effects of monitoring — in particular, unlike
     * ordinary eavesdropping, monitoring the system bus does not require the access control rules to be relaxed, which
     * would change the set of messages that can be delivered to their (non-monitor) destinations. However, it is
     * unavoidable that monitoring will increase the message bus's resource consumption. In edge cases where there was
     * barely enough time or memory without monitoring, this might result in message deliveries failing when they would
     * otherwise have succeeded.
     * </p>
     *
     * @param _rule Match rules to add to the connection
     * @param _flags Not used, must be 0
     */
    void BecomeMonitor(String[] _rule, UInt32 _flags);
}
