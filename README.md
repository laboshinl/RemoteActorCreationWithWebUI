# RemoteActorCreationWithWebUI

There are two sbt projects in this repo. LocalApp starts web-server on localhost:8080. RemoteApp creates actor system and listens on localhost:15150.<br>
Web page on localhost:8080 contents two links: one will start another actor on remote system, another will shut both systems down.
