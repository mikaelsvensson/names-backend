$ curl http://localhost:8080/names?filter=mika

$ mvn package
$ java -jar \
    names-backend-service/target/names-backend-service-0.0.1-SNAPSHOT.jar \
    --server.port=8082