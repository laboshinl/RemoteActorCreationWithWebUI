#!/bin/bash

if [ "$1" == "localhost" ]; then 
	cd scripts/configurations
	echo "Changing settings to localhost"
	cp application.conf.localapp.local ../../LocalApp/src/main/resources/application.conf
	cp application.conf.remoteapp.local ../../RemoteApp/src/main/resources/application.conf
	cp application.conf.messagerouter.local ../../MessageRouter/src/main/resources/application.conf
else
	if [ "$1" == "openstack" ]; then 
		cd scripts/configurations
		echo "Changing settings to openstack"
		cp application.conf.localapp.openstack ../../LocalApp/src/main/resources/application.conf
		cp application.conf.remoteapp.openstack ../../RemoteApp/src/main/resources/application.conf
		cp application.conf.messagerouter.openstack ../../MessageRouter/src/main/resources/application.conf
	else
		echo "Enter 'localhost' or 'openstack' to change application.conf's accordingly"
	fi
fi