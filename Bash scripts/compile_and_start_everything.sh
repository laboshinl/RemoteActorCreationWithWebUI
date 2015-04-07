#!/bin/bash

#1280*720

let "addit_monitors_amount="$(xrandr -q | grep '\*' | wc -l)" - 1"
echo "Additional monitors amount: $addit_monitors_amount"
let "hor0 = addit_monitors_amount * 1366 + 360"

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
xfce4-terminal -H -e "sbt run" --geometry=75x18+0+hor0
sleep 3
cd ../RemoteApp/
xfce4-terminal -H -e "sbt run" --geometry=75x18+0+hor0
sleep 3
cd ../MessageRouter/
xfce4-terminal -H -e "sbt run" --geometry=75x18+0+hor0
