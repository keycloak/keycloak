#!/bin/sh
#
# WildFly control script
#
# chkconfig: 2345 80 20
# description: WildFly startup/shutdown script
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

# Source function library.
. /etc/init.d/functions

NAME=$(readlink -f ${0} | xargs basename)

# Check privileges
if [ `id -u` -ne 0 ]; then
	echo "You need root privileges to run this script"
	exit 1
fi

# Load wildfly init.d configuration.
if [ -z "$JBOSS_CONF" ]; then
	JBOSS_CONF="/etc/default/${NAME}"
fi

# Set defaults.
if [ -f "$JBOSS_CONF" ]; then
	. "$JBOSS_CONF"
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
	JBOSS_HOME="/opt/${NAME}"
fi
export JBOSS_HOME

# Check if wildfly is installed
if [ ! -f "$JBOSS_HOME/jboss-modules.jar" ]; then
	echo "$NAME is not installed in \"$JBOSS_HOME\""
	exit 1
fi

# Run as wildfly user
if [ -z "$JBOSS_USER" ]; then
	JBOSS_USER=wildfly
fi

# Check wildfly user
id $JBOSS_USER > /dev/null 2>&1
if [ $? -ne 0 -o -z "$JBOSS_USER" ]; then
	echo "User \"$JBOSS_USER\" does not exist..."
	exit 1
fi

# Check owner of JBOSS_HOME
if [ ! $(stat -L -c "%U" "$JBOSS_HOME") = $JBOSS_USER ]; then
	echo "The user \"$JBOSS_USER\" is not owner of \"$(readlink -f $JBOSS_HOME)\""
	echo "Try: chown -R $JBOSS_USER:$JBOSS_USER \"$(readlink -f $JBOSS_HOME)\""
	exit 1
fi

# Location to set the pid file
if [ -z "$JBOSS_PIDFILE" ]; then
	JBOSS_PIDFILE=/var/run/wildfly/${NAME}.pid
fi
export JBOSS_PIDFILE

# Location to set the lock file
if [ -z "$JBOSS_LOCKFILE" ]; then
	JBOSS_LOCKFILE=/var/lock/subsys/${NAME}
fi

# Location to keep the console log
if [ -z "$JBOSS_CONSOLE_LOG" ]; then
	JBOSS_CONSOLE_LOG=/var/log/${NAME}/console.log
fi

# The amount of time to wait for startup
if [ -z "$STARTUP_WAIT" ]; then
	STARTUP_WAIT=30
fi

# The amount of time to wait for shutdown
if [ -z "$SHUTDOWN_WAIT" ]; then
	SHUTDOWN_WAIT=30
fi

# Startup mode of wildfly
if [ -z "$JBOSS_MODE" ]; then
	JBOSS_MODE=standalone
fi

# Startup mode script
if [ "$JBOSS_MODE" = "standalone" ]; then
	JBOSS_SCRIPT=$JBOSS_HOME/bin/standalone.sh
	if [ -z "$JBOSS_CONFIG" ]; then
		JBOSS_CONFIG=standalone.xml
	fi
	JBOSS_MARKERFILE=$JBOSS_HOME/standalone/tmp/startup-marker
else
	JBOSS_SCRIPT=$JBOSS_HOME/bin/domain.sh
	if [ -z "$JBOSS_DOMAIN_CONFIG" ]; then
		JBOSS_DOMAIN_CONFIG=domain.xml
	fi
	if [ -z "$JBOSS_HOST_CONFIG" ]; then
		JBOSS_HOST_CONFIG=host.xml
	fi
	JBOSS_MARKERFILE=$JBOSS_HOME/domain/tmp/startup-marker
fi

# Helper function to check status of wildfly service
check_status() {
	status -p "$JBOSS_PIDFILE" -l $(basename "$JBOSS_LOCKFILE") "$NAME" >/dev/null 2>&1
}

start() {
	echo -n $"Starting $NAME: "
	check_status
	status_start=$?
	if [ $status_start -eq 3 ]; then
		mkdir -p $(dirname "$JBOSS_PIDFILE")
		mkdir -p $(dirname "$JBOSS_CONSOLE_LOG")
		chown $JBOSS_USER $(dirname "$JBOSS_PIDFILE") || true
		cat /dev/null > "$JBOSS_CONSOLE_LOG"
		currenttime=$(date +%s%N | cut -b1-13)

		if [ "$JBOSS_MODE" = "standalone" ]; then
			cd $JBOSS_HOME >/dev/null 2>&1
			daemon --user=$JBOSS_USER --pidfile=$JBOSS_PIDFILE LAUNCH_JBOSS_IN_BACKGROUND=1 JBOSS_PIDFILE=$JBOSS_PIDFILE "$JBOSS_SCRIPT -c $JBOSS_CONFIG $JBOSS_OPTS &" >> $JBOSS_CONSOLE_LOG 2>&1
			cd - >/dev/null 2>&1
		else
			cd $JBOSS_HOME >/dev/null 2>&1
			daemon --user=$JBOSS_USER --pidfile=$JBOSS_PIDFILE LAUNCH_JBOSS_IN_BACKGROUND=1 JBOSS_PIDFILE=$JBOSS_PIDFILE "$JBOSS_SCRIPT --domain-config=$JBOSS_DOMAIN_CONFIG --host-config=$JBOSS_HOST_CONFIG $JBOSS_OPTS &" >> $JBOSS_CONSOLE_LOG 2>&1
			cd - >/dev/null 2>&1
		fi

		count=0
		until [ $count -gt $STARTUP_WAIT ]
		do
			sleep 1
			let count=$count+1;
			if [ -f $JBOSS_MARKERFILE ]; then
				markerfiletimestamp=$(grep -o '[0-9]\+' $JBOSS_MARKERFILE) > /dev/null
				if [ "$markerfiletimestamp" -gt "$currenttime" ] ; then
					grep -i 'success:' $JBOSS_MARKERFILE > /dev/null
					if [ $? -eq 0 ]; then
						success
						echo
						touch $JBOSS_LOCKFILE
						exit 0
					fi
					grep -i 'error:' $JBOSS_MARKERFILE > /dev/null
					if [ $? -eq 0 ]; then
						warning
						echo
						echo "$NAME started with errors, please see server log for details."
						touch $JBOSS_LOCKFILE
						exit 0
					fi
				fi
			fi
		done

		if check_status; then
			warning
			echo
			echo "$NAME hasn't started within the timeout allowed."
			touch $JBOSS_LOCKFILE
			exit 0
		else
			failure
			echo
			echo "$NAME failed to start within the timeout allowed."
			exit 1
		fi

	else
		echo
		$0 status
	fi
}

stop() {
	echo -n $"Shutting down $NAME: "
	check_status
	status_stop=$?
	if [ $status_stop -eq 0 ]; then
		count=0;
		if [ -f $JBOSS_PIDFILE ]; then
			read kpid < $JBOSS_PIDFILE
			let kwait=$SHUTDOWN_WAIT

			# Try issuing SIGTERM
			kill -15 $kpid
			until [ `ps --pid $kpid 2> /dev/null | grep -c $kpid 2> /dev/null` -eq '0' ] || [ $count -gt $kwait ]
				do
				sleep 1
				let count=$count+1;
			done

			if [ $count -gt $kwait ]; then
				kill -9 $kpid
			fi
		fi
		success
	elif [ $status_stop -eq 1 ]; then
		echo
		echo -n "$NAME dead but pid file exists, cleaning up"
	elif [ $status_stop -eq 2 ]; then
		echo
		echo -n "$NAME dead but subsys locked, cleaning up"
	elif [ $status_stop -eq 3 ]; then
		echo
		echo -n $"$NAME is already stopped"
	fi
	rm -f $JBOSS_PIDFILE
	rm -f $JBOSS_LOCKFILE
	echo
}

case "$1" in
	start)
		start
		;;
	stop)
		stop
		;;
	restart)
		$0 stop
		$0 start
		;;
	status)
		status -p "$JBOSS_PIDFILE" -l $(basename "$JBOSS_LOCKFILE") "$NAME"
		;;
	*)
		## If no parameters are given, print which are avaiable.
		echo "Usage: $0 {start|stop|restart|status}"
		exit 1
		;;
esac
