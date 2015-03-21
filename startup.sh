#!/bin/bash

if [ "$#" -eq 0 ]; then 
	echo "Switch directory to RemoteActorCreationWithWebUI"
	cd /home/debian/RemoteActorCreationWithWebUI
	echo "Updating files from repository"
	git pull
	echo "Switch directory to RemoteApp"
	cd RemoteApp
	echo "Starting programm"
	sbt run &
elif [ "$1" == "all" ]; then 
		./scripts/compile_and_start_everything.sh
elif [ "$1" == "localhost" ]; then
		./scripts/change_settings_to.sh localhost
elif [ "$1" == "openstack" ]; then
		./scripts/change_settings_to.sh openstack
fi
