#!/bin/bash

if [ "$1" == "localhost" ]; then 
	cd scripts/configurations
	echo "Changing settings to localhost"
	cp application.conf.localapp.local ../../App/LocalApp/src/main/resources/application.conf
	cp application.conf.remoteapp.local ../../App/RemoteApp/src/main/resources/application.conf
	cp application.conf.messagerouter.local ../../App/MessageRouter/src/main/resources/application.conf
else
	if [ "$1" == "openstack" ]; then 
		cd scripts/configurations
		echo "Changing settings to openstack"
		cp application.conf.localapp.openstack ../../App/LocalApp/src/main/resources/application.conf
		cp application.conf.remoteapp.openstack ../../App/RemoteApp/src/main/resources/application.conf
		cp application.conf.messagerouter.openstack ../../App/MessageRouter/src/main/resources/application.conf
	else
		echo "Enter 'localhost' or 'openstack' to change application.conf's accordingly"
	fi
fi