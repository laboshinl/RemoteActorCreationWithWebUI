#!/bin/bash

echo "Compiling everything"
cd LocalApp/
sbt compile &
cd ../RemoteApp/
sbt compile &
cd ../MessageRouter/
sbt compile &
while [[ $(ps -ax | grep "sbt compile" | wc -l) != 1 ]]; do
	sleep 2
done
echo "Starting everything"
cd ../LocalApp/
xfce4-terminal -H -e "sbt run"
cd ../RemoteApp/
xfce4-terminal -H -e "sbt run"
cd ../MessageRouter/
xfce4-terminal -H -e "sbt run"
