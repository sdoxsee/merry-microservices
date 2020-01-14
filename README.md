# Merry Microservices: Part 2 'UI Gateway'--A React UI served by a Spring Cloud Gateway OAuth2 Client

This is the repository corresponding to [Part 2](https://sdoxsee.github.io/blog/2019/12/17/merry-microservices-part2-gateway) of the blog series [Merry Microservices](https://sdoxsee.github.io/blog/2019/12/17/merry-microservices-an-introduction). It builds a CRUD front-end gateway application with Create React App, TypeScript, and Hooks. The JavaScript is served up by Spring Cloud Gateway--acting as a light server-side proxy for the React UI that safely manages OpenID Connect and OAuth2 flows and that relays request back to our note resources server. Here, you'll find the following files and folders:
* `docker-compose.yml` spins up a Keycloak instance on port 9080 that `note` and `gateway` will also use
* `keycloak.yml` and `realm-config` folder are from https://github.com/jhipster/jhipster-sample-app-oauth2/blob/master/src/main/docker
* `note` folder is a Spring Boot OAuth2 resource server app using Webflux and R2DBC that stores `Note` entities
* `gateway` folder is a Spring Cloud Gateway app with a React CRUD UI that handles the OAuth2/OIDC dance and relays requests to resource servers (e.g. `note`)

# Quickstart

1. Start `keycloak` (port 9080)

```
docker-compose up
```

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