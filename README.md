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

## Email

The service can send emails in two different ways: Regular SMTP or the Mailgun API.

### The SMTP Option

This option can be used to have the service send emails using pretty much any mail account.

**Step 1**. Activate the profile `email-smtp` by adding this to your start command:

    --spring.profiles.active=email-smtp

**Step 2**. Add this configuration to `application-email-smtp.yaml`:

    spring:
      mail:
        host: ...
        port: ...
        username: ...
        password: ***

Other properties may also be necessary, for example properties to enable encryption.
See https://www.baeldung.com/spring-email.

### The Mailgun Option

This is the preferred option if you have an account with [Mailgun](https://www.mailgun.com/).

**Step 1**. Activate the profile `email-mailgun` by adding this to your start command:

    --spring.profiles.active=email-mailgun

**Step 2**. Add this configuration to `application-email-mailgun.yaml`:

    mailgun:
      from: ...
      endpoint: ...
      apiKey: ***

The `endpoint` will be something like `https://api.eu.mailgun.net/v3/YOUR_DOMAIN_HERE/messages`.

### Sending a Test Mail

You can test the mail sender using the `/admin` endpoint.

**Step 1**. Enable the admin account by adding this to your `application.yaml`:

    admin:
      testMail:
        to: ...
      user:
        username: admin
        password: ***

To avoid abuse, all test mails will be sent to the hardcoded address specified in `admin.testMail.to`.

**Step 2**. Run this command:

    $ curl -k -X POST -u admin:*** https://localhost:8443/admin/test-mail

Note: The command above assumes you've enabled https locally.

## Import Popular Names 

The service comes bundles with thousands of popular names from various countries.

You can enable/import them using these `curl` commands:

 * Finnish names:   `curl -k -X POST -u admin:*** https://localhost:8443/admin/import/avoindata`
 * Danish names:    `curl -k -X POST -u admin:*** https://localhost:8443/admin/import/dst`
 * Swedish names:   `curl -k -X POST -u admin:*** https://localhost:8443/admin/import/scb`
 * USA names:       `curl -k -X POST -u admin:*** https://localhost:8443/admin/import/ssa`
 * Norwegian names: `curl -k -X POST -u admin:*** https://localhost:8443/admin/import/ssb`

Note: The commands above assume you've enabled https locally.

## Ping

The service has a "ping" endpoint for verifying that the service is up-and-running with a working
database connection.

    $ curl -k -X GET -u admin:*** https://localhost:8443/admin/ping

Note: The commands above assume you've enabled https locally.
