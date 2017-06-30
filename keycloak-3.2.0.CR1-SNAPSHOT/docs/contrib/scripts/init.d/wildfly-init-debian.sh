#!/bin/sh
#
# /etc/init.d/wildfly -- startup script for WildFly
#
# Written by Jorge Solorzano
#
### BEGIN INIT INFO
# Provides:             wildfly
# Required-Start:       $remote_fs $network
# Required-Stop:        $remote_fs $network
# Should-Start:         $named
# Should-Stop:          $named
# Default-Start:        2 3 4 5
# Default-Stop:         0 1 6
# Short-Description:    WildFly Application Server
# Description:          WildFly startup/shutdown script
### END INIT INFO

NAME=$(readlink -f ${0} | xargs basename)
DESC="WildFly Application Server"
DEFAULT="/etc/default/$NAME"

# Check privileges
if [ `id -u` -ne 0 ]; then
	echo "You need root privileges to run this script"
	exit 1
fi

# Make sure wildfly is started with system locale
if [ -r /etc/default/locale ]; then
	. /etc/default/locale
	export LANG
fi

# Source function library.
. /lib/lsb/init-functions

if [ -r /etc/default/rcS ]; then
	. /etc/default/rcS
fi

# Overwrite settings from default file
if [ -f "$DEFAULT" ]; then
	. "$DEFAULT"
fi

# Location of JDK
if [ -n "$JAVA_HOME" ]; then
	export JAVA_HOME
fi

# Setup the JVM
if [ -z "$JAVA" ]; then
	if [ -n "$JAVA_HOME" ]; then
		JAVA="$JAVA_HOME/bin/java"
	else
		JAVA="java"
	fi
fi

# Location of wildfly
if [ -z "$JBOSS_HOME" ]; then
	JBOSS_HOME="/opt/$NAME"
fi
export JBOSS_HOME

# Check if wildfly is installed
if [ ! -f "$JBOSS_HOME/jboss-modules.jar" ]; then
	log_failure_msg "$NAME is not installed in \"$JBOSS_HOME\""
	exit 1
fi

# Run as wildfly user
# Example of user creation for Debian based:
# adduser --system --group --no-create-home --home $JBOSS_HOME --disabled-login wildfly
if [ -z "$JBOSS_USER" ]; then
	JBOSS_USER=wildfly
fi

# Check wildfly user
id $JBOSS_USER > /dev/null 2>&1
if [ $? -ne 0 -o -z "$JBOSS_USER" ]; then
	log_failure_msg "User \"$JBOSS_USER\" does not exist..."
	exit 1
fi

# Check owner of JBOSS_HOME
if [ ! $(stat -L -c "%U" "$JBOSS_HOME") = $JBOSS_USER ]; then
	log_failure_msg "The user \"$JBOSS_USER\" is not owner of \"$JBOSS_HOME\""
	exit 1
fi

# Startup mode of wildfly
if [ -z "$JBOSS_MODE" ]; then
	JBOSS_MODE=standalone
fi

# Startup mode script
if [ "$JBOSS_MODE" = "standalone" ]; then
	JBOSS_SCRIPT="$JBOSS_HOME/bin/standalone.sh"
	if [ -z "$JBOSS_CONFIG" ]; then
		JBOSS_CONFIG=standalone.xml
	fi
	JBOSS_MARKERFILE="$JBOSS_HOME/standalone/tmp/startup-marker"
else
	JBOSS_SCRIPT="$JBOSS_HOME/bin/domain.sh"
	if [ -z "$JBOSS_DOMAIN_CONFIG" ]; then
		JBOSS_DOMAIN_CONFIG=domain.xml
	fi
	if [ -z "$JBOSS_HOST_CONFIG" ]; then
		JBOSS_HOST_CONFIG=host.xml
	fi
	JBOSS_MARKERFILE="$JBOSS_HOME/domain/tmp/startup-marker"
fi

# Check startup file
if [ ! -x "$JBOSS_SCRIPT" ]; then
	log_failure_msg "$JBOSS_SCRIPT is not an executable!"
	exit 1
fi

# Check cli file
JBOSS_CLI="$JBOSS_HOME/bin/jboss-cli.sh"
if [ ! -x "$JBOSS_CLI" ]; then
	log_failure_msg "$JBOSS_CLI is not an executable!"
	exit 1
fi

# The amount of time to wait for startup
if [ -z "$STARTUP_WAIT" ]; then
	STARTUP_WAIT=30
fi

# The amount of time to wait for shutdown
if [ -z "$SHUTDOWN_WAIT" ]; then
	SHUTDOWN_WAIT=30
fi

# Location to keep the console log
if [ -z "$JBOSS_CONSOLE_LOG" ]; then
	JBOSS_CONSOLE_LOG="/var/log/$NAME/console.log"
fi
export JBOSS_CONSOLE_LOG

# Location to set the pid file
JBOSS_PIDFILE="/var/run/wildfly/$NAME.pid"
export JBOSS_PIDFILE

# Launch wildfly in background
LAUNCH_JBOSS_IN_BACKGROUND=1
export LAUNCH_JBOSS_IN_BACKGROUND

# Helper function to check status of wildfly service
check_status() {
	pidofproc -p "$JBOSS_PIDFILE" "$JAVA" >/dev/null 2>&1
}

case "$1" in
 start)
	log_daemon_msg "Starting $DESC" "$NAME"
	check_status
	status_start=$?
	if [ $status_start -eq 3 ]; then
		mkdir -p $(dirname "$JBOSS_PIDFILE")
		mkdir -p $(dirname "$JBOSS_CONSOLE_LOG")
		chown $JBOSS_USER $(dirname "$JBOSS_PIDFILE") || true
		cat /dev/null > "$JBOSS_CONSOLE_LOG"
		currenttime=$(date +%s%N | cut -b1-13)

		if [ "$JBOSS_MODE" = "standalone" ]; then
			start-stop-daemon --start --user "$JBOSS_USER" \
			--chuid "$JBOSS_USER" --chdir "$JBOSS_HOME" --pidfile "$JBOSS_PIDFILE" \
			--exec "$JBOSS_SCRIPT" -- -c $JBOSS_CONFIG $JBOSS_OPTS >> "$JBOSS_CONSOLE_LOG" 2>&1 &
		else
			start-stop-daemon --start --user "$JBOSS_USER" \
			--chuid "$JBOSS_USER" --chdir "$JBOSS_HOME" --pidfile "$JBOSS_PIDFILE" \
			--exec "$JBOSS_SCRIPT" -- --domain-config=$JBOSS_DOMAIN_CONFIG \
			--host-config=$JBOSS_HOST_CONFIG $JBOSS_OPTS >> "$JBOSS_CONSOLE_LOG" 2>&1 &
		fi

		count=0
		until [ $count -gt $STARTUP_WAIT ]
		do
			sleep 1
			count=$((count + 1));
			if [ -f "$JBOSS_MARKERFILE" ]; then
				markerfiletimestamp=$(grep -o '[0-9]*' "$JBOSS_MARKERFILE") > /dev/null
				if [ "$markerfiletimestamp" -gt "$currenttime" ] ; then
					grep -i 'success:' "$JBOSS_MARKERFILE" > /dev/null
					if [ $? -eq 0 ]; then
						log_end_msg 0
						exit 0
					fi
					grep -i 'error:' "$JBOSS_MARKERFILE" > /dev/null
					if [ $? -eq 0 ]; then
						log_end_msg 255
						log_warning_msg "$DESC started with errors, please see server log for details."
						exit 0
					fi
				fi
			fi
		done

		if check_status; then
			log_end_msg 255
			log_warning_msg "$DESC hasn't started within the timeout allowed."
			exit 0
		else
			log_end_msg 1
			log_failure_msg "$DESC failed to start within the timeout allowed."
			exit 1
		fi

	elif [ $status_start -eq 1 ]; then
		log_failure_msg "$DESC is not running but the pid file exists"
		exit 1
	elif [ $status_start -eq 0 ]; then
		log_success_msg "$DESC (already running)"
	fi
 ;;
 stop)
	check_status
	status_stop=$?
	if [ $status_stop -eq 0 ]; then
		read kpid < "$JBOSS_PIDFILE"
		log_daemon_msg "Stopping $DESC" "$NAME"

		children_pids=$(pgrep -P $kpid)

		start-stop-daemon --stop --quiet --pidfile "$JBOSS_PIDFILE" \
		--user "$JBOSS_USER" --retry=TERM/$SHUTDOWN_WAIT/KILL/5 \
		>/dev/null 2>&1

		if [ $? -eq 2 ]; then
			log_failure_msg "$DESC can't be stopped"
			exit 1
		fi

		for child in $children_pids; do
			/bin/kill -9 $child >/dev/null 2>&1
		done

		log_end_msg 0
	elif [ $status_stop -eq 1 ]; then
		log_action_msg "$DESC is not running but the pid file exists, cleaning up"
		rm -f $JBOSS_PIDFILE
	elif [ $status_stop -eq 3 ]; then
		log_action_msg "$DESC is not running"
	fi
 ;;
 restart)
	check_status
	status_restart=$?
	if [ $status_restart -eq 0 ]; then
		$0 stop
	fi
	$0 start
 ;;
 reload|force-reload)
	check_status
	status_reload=$?
	if [ $status_reload -eq 0 ]; then
		log_daemon_msg "Reloading $DESC config" "$NAME"

		if [ "$JBOSS_MODE" = "standalone" ]; then
			RELOAD_CMD=":reload"; else
			RELOAD_CMD=":reload-servers"; fi

		start-stop-daemon --start --chuid "$JBOSS_USER" \
		--exec "$JBOSS_CLI" -- --connect --command=$RELOAD_CMD >/dev/null 2>&1

		if [ $? -eq 0 ]; then
			log_end_msg 0
		else
			log_end_msg 1
		fi
	else
		log_failure_msg "$DESC is not running"
	fi
 ;;
 status)
	check_status
	status=$?
	if [ $status -eq 0 ]; then
		read pid < $JBOSS_PIDFILE
		log_action_msg "$DESC is running with pid $pid"
		exit 0
	elif [ $status -eq 1 ]; then
		log_action_msg "$DESC is not running and the pid file exists"
		exit 1
	elif [ $status -eq 3 ]; then
		log_action_msg "$DESC is not running"
		exit 3
	else
		log_action_msg "Unable to determine $NAME status"
		exit 4
	fi
 ;;
 *)
 log_action_msg "Usage: $0 {start|stop|restart|reload|force-reload|status}"
 exit 2
 ;;
esac

exit 0
