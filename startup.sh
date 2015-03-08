echo "Switch directory to RemoteActorCreationWithWebUI"
cd /home/debian/RemoteActorCreationWithWebUI
echo "Updating files from repository"
git pull
echo "Switch directory to RemoteApp"
cd RemoteApp
echo "Starting programm"
sbt run
