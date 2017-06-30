User contributed init scripts
=============================

```
 ____      ____  _       _______     ____  _____  _____  ____  _____   ______    _  
|_  _|    |_  _|/ \     |_   __ \   |_   \|_   _||_   _||_   \|_   _|.' ___  |  | | 
  \ \  /\  / / / _ \      | |__) |    |   \ | |    | |    |   \ | | / .'   \_|  | | 
   \ \/  \/ / / ___ \     |  __ /     | |\ \| |    | |    | |\ \| | | |   ____  | | 
    \  /\  /_/ /   \ \_  _| |  \ \_  _| |_\   |_  _| |_  _| |_\   |_\ `.___]  | |_| 
     \/  \/|____| |____||____| |___||_____|\____||_____||_____|\____|`._____.'  (_) 

```



In this folder you can find user contributed scripts & services for running WildFly as a service on various operating systems.

This scripts are user contributions and are here as example and/or reference.  

init.d
-------------------

System V init.d scripts.

wildfly-init-redhat.sh for usage with RHEL / Centos distribution
wildfly-init-debian.sh for usage with Debian based distributions

Both scripts use wildfly.conf as reference configuration file and expect that file is present in proper location.

Selected script should be copied and renamed to proper directory before usage.


systemd
------------------------------------------
Init script for usage linux distribution that use systemd.

see systemd/README on how to use it


service
-------

Windows service files, to enable installing wildfly as service on Windows.
see service.bat on how to use it