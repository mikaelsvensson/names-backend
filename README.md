$ curl http://localhost:8080/names?filter=mika

## Run service locally

    $ mvn package
    $ java -jar \
        names-backend-service/target/names-backend-service-0.0.1-SNAPSHOT.jar \
        --server.port=8080

### Use https locally

Add this to your `application.yaml`:

    server:
      port: 8443
      ssl:
        key-alias: selfsigned_localhost_sslserver
        key-store-password: changeit
        key-store: classpath:ssl-server.jks
        key-store-provider: SUN
        key-store-type: JKS

The key store file `ssl-server.jks` already exists in `names-backend-service/src/main/resources/ssl-server.jks`, so no need to create it.

For reference, this is how `ssl-server.jks` was once created:

    keytool -genkey -alias selfsigned_localhost_sslserver -keyalg RSA -keysize 2048 -validity 700 -keypass changeit -storepass changeit -keystore ssl-server.jks

If using Chrome, you may also want to "allow insecure localhost" when testing the front-end locally. Open Chrome and go to this "address": `chrome://flags/#allow-insecure-localhost`.