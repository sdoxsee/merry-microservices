# Merry Microservices: Part 3 ‘Policy Service’–Application authorization management based on identity and permissions

This is the repository corresponding to [Part 3](https://sdoxsee.github.io/blog/2020/01/12/merry-microservices-part3-policy-service) of the blog series [Merry Microservices](https://sdoxsee.github.io/blog/2019/12/17/merry-microservices-an-introduction). It demonstrates the place for a “policy service” to manage the identity permissions (or policies) specific to each application in the architecture rather than overloading the JWT, at the Identity Provider level, with irrelevant permissions. Here, you'll find the following files and folders:
* `docker-compose.yml` spins up a Keycloak instance on port 9080 and a "policy service" on port 8080 that `note` and `gateway` will also use
* `keycloak.yml` and `realm-config` folder are from https://github.com/jhipster/jhipster-sample-app-oauth2/blob/master/src/main/docker
* `note` folder is a Spring Boot OAuth2 resource server app using Webflux and R2DBC that stores `Note` entities
* `gateway` folder is a Spring Cloud Gateway app with a React CRUD UI that handles the OAuth2/OIDC dance and relays requests to resource servers (e.g. `note`)

# Quickstart

1. Start `keycloak` (port 9080) and `policyservice` (port 8080)

```
docker-compose up
```
Of course, if you want users to be granted permissions for, say, `CanRead`, `CanReadConfidentialNotes` or `Snowing`, you'll need to configure the "policy service" as per the Part 3 blog post. See "Dealing with an identity provider in Docker" below for challenges with Dockerized identity providers.

2. Start `note` on port 8081

```
(cd note && ./mvnw spring-boot:run)
```

3. Start `gateway` on port 8082
```
(cd gateway && ./mvnw clean package spring-boot:run -DskipTests)
```

# Gateway in development

Rather than just build and start the UI gateway on port 8082, we can split the frontend and backend for a better developer experience.

In one terminal, start the backend on port 8082
```
(cd gateway && ./mvnw clean spring-boot:run)
```
In another terminal, start the frontend on port 3000
```
(cd gateway && npm start)
```
Now, whenever you save your TypeScript files, you'll get hot-reloading in the browser.

# Dealing with an identity provider in Docker

In order to sign in to the Dockerized "policy service" using Keycloak, you'll need to add the following to your machine's `hosts` file ([details](https://www.jhipster.tech/docker-compose/#-keycloak)):

```
127.0.0.1	keycloak
```

If you're on a Mac, this will automatically append the line to your `/etc/hosts` file: 
```
sudo -- sh -c "echo '127.0.0.1	keycloak' >> /etc/hosts"
```

This is needed because you will access your application with a browser on your machine (which name is `localhost`, or `127.0.0.1`), but inside Docker it will run in its own container, which name is `keycloak`. Other than the extra configuration, another downside is that when you connect other services outside of docker, they won't be able to leverage the existing identity provider session (i.e. "SSO") since it's on "`keycloak`" rather than "`localhost`".