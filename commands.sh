curl -X PUT -H "Content-Type: application/json" -d '{"t":"ParrotActor"}' localhost:8080/actor 
curl -X PUT -H "Content-Type: application/json" -d '{"t":"test"}' localhost:8080/actor
curl -X PUT -H "Content-Type: application/json" -d '{"q":"ParrotActor"}' localhost:8080/actor
curl -X PUT -H "Content-Type: application/json" -d '{"q":"test"}' localhost:8080/actor
curl -X PUT localhost:8080/actor
curl -X POST -H "Content-Type: application/json" -d '{"id":"1", "msg":"Hello1"}' localhost:8080/actor
curl -X POST -H "Content-Type: application/json" -d '{"msg":"Hello2"}' localhost:8080/actor
curl -X POST -H "Content-Type: application/json" -d '{"id":"2"}' localhost:8080/actor
curl -X POST localhost:8080/actor
curl -X DELETE -H "Content-Type: application/json" -d '{"id":"1"}' localhost:8080/actor
curl -X DELETE -H "Content-Type: application/json" -d '{"t":"2"}' localhost:8080/actor
curl -X DELETE -H "Content-Type: application/json" -d '{"id":"22"}' localhost:8080/actor
curl -X DELETE -H "Content-Type: application/json" -d '{"id":"-22"}' localhost:8080/actor
curl -X DELETE localhost:8080/actor
curl -X DELETE -H "Content-Type: application/json" -d '{"id":"1"}' localhost:8080/system
curl -X DELETE localhost:8080/system

curl -X PUT localhost:8080/system;
sleep 5;
curl -X POST -H "Content-Type: application/json" -d '{"id":"1"}' localhost:8080/system;
sleep 5;
curl -X DELETE -H "Content-Type: application/json" -d '{"id":"1"}' localhost:8080/system;
sleep 5;
curl -X POST -H "Content-Type: application/json" -d '{"id":"1"}' localhost:8080/system;