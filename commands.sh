curl -X PUT -H "Content-Type: application/json" -d '{"t":"ParrotActor"}' localhost:8080/actor 
curl -X PUT -H "Content-Type: application/json" -d '{"t":"ParrotActor"}' localhost:8080/actor
curl -X PUT -H "Content-Type: application/json" -d '{"t":"ParrotActor"}' localhost:8080/actor
curl -X PUT -H "Content-Type: application/json" -d '{"t":"ParrotActor"}' localhost:8080/actor
curl -X PUT -H "Content-Type: application/json" -d '{"t":"ParrotActor"}' localhost:8080/actor
curl -X PUT -H "Content-Type: application/json" -d '{"t":"ParrotActor"}' localhost:8080/actor
curl -X PUT -H "Content-Type: application/json" -d '{"t":"ParrotActor"}' localhost:8080/actor
curl -X PUT -H "Content-Type: application/json" -d '{"t":"ParrotActor"}' localhost:8080/actor
curl -X POST -H "Content-Type: application/json" -d '{"id":"1", "msg":"Hello1"}' localhost:8080/actor
curl -X POST -H "Content-Type: application/json" -d '{"id":"2", "msg":"Hello2"}' localhost:8080/actor
curl -X POST -H "Content-Type: application/json" -d '{"id":"3", "msg":"Hello3"}' localhost:8080/actor
curl -X POST -H "Content-Type: application/json" -d '{"id":"4", "msg":"Hello4"}' localhost:8080/actor
curl -X POST -H "Content-Type: application/json" -d '{"id":"5", "msg":"Hello5"}' localhost:8080/actor
curl -X POST -H "Content-Type: application/json" -d '{"id":"6", "msg":"Hello6"}' localhost:8080/actor
curl -X POST -H "Content-Type: application/json" -d '{"id":"7", "msg":"Hello7"}' localhost:8080/actor
curl -X POST -H "Content-Type: application/json" -d '{"id":"8", "msg":"Hello8"}' localhost:8080/actor
curl -X DELETE -H "Content-Type: application/json" -d '{"id":"1"}' localhost:8080/actor
curl -X DELETE -H "Content-Type: application/json" -d '{"id":"2"}' localhost:8080/actor
curl -X DELETE -H "Content-Type: application/json" -d '{"id":"3"}' localhost:8080/actor
curl -X DELETE -H "Content-Type: application/json" -d '{"id":"4"}' localhost:8080/actor
curl -X DELETE -H "Content-Type: application/json" -d '{"id":"5"}' localhost:8080/actor
curl -X DELETE -H "Content-Type: application/json" -d '{"id":"6"}' localhost:8080/actor
curl -X DELETE -H "Content-Type: application/json" -d '{"id":"7"}' localhost:8080/actor
curl -X DELETE -H "Content-Type: application/json" -d '{"id":"8"}' localhost:8080/actor
curl -X POST -H "Content-Type: application/json" -d '{"id":"1", "msg":"Hello"}' localhost:8080/actor
curl -X DELETE localhost:8080/system

curl -X PUT localhost:8080/system;
sleep 5;
curl -X POST -H "Content-Type: application/json" -d '{"id":"1"}' localhost:8080/system;
sleep 5;
curl -X DELETE -H "Content-Type: application/json" -d '{"id":"1"}' localhost:8080/system;
sleep 5;
curl -X POST -H "Content-Type: application/json" -d '{"id":"1"}' localhost:8080/system;