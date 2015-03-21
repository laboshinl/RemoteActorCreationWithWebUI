#!/bin/bash

echo "Compiling everything"
cd LocalApp/
sbt compile &
cd ../RemoteApp/
sbt compile &
cd ../MessageRouter/
sbt compile &
echo "Starting everything"
cd ../LocalApp/
sbt run &
cd ../RemoteApp/
sbt run &
cd ../MessageRouter/
sbt run &
