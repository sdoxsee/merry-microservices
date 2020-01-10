package ca.simplestep.gateway;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.oidc.web.server.logout.OidcClientInitiatedServerLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.logout.ServerLogoutSuccessHandler;
import org.springframework.security.web.server.csrf.CookieServerCsrfTokenRepository;
import org.springframework.security.web.server.csrf.CsrfToken;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;

import static org.springframework.security.config.Customizer.withDefaults;
import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

@SpringBootApplication
public class GatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(GatewayApplication.class, args);
	}
}

@EnableWebFluxSecurity
class SecurityConfiguration {

	@Value("${server.port:8080}")
	private String serverPort;

	private final ReactiveClientRegistrationRepository clientRegistrationRepository;

	public SecurityConfiguration(ReactiveClientRegistrationRepository clientRegistrationRepository) {
		this.clientRegistrationRepository = clientRegistrationRepository;
	}

	@Bean
	public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
		http
				.oauth2Login(withDefaults())
				.csrf(csrf -> csrf.csrfTokenRepository(CookieServerCsrfTokenRepository.withHttpOnlyFalse()))
				.authorizeExchange(exchanges ->
						exchanges
								.pathMatchers("/manifest.json", "/*.png", "/static/**", "/api/user", "/").permitAll()
								.anyExchange().authenticated()
				)
				.logout(logout ->
						logout
								.logoutSuccessHandler(oidcLogoutSuccessHandler()));
		return http.build();
	}

	// see https://github.com/spring-projects/spring-security/issues/5766#issuecomment-564636167
	@Bean
	WebFilter addCsrfToken() {
		return (exchange, next) -> exchange
			.<Mono<CsrfToken>>getAttribute(CsrfToken.class.getName())
			.doOnSuccess(token -> {})
			.then(next.filter(exchange));
	}

	private ServerLogoutSuccessHandler oidcLogoutSuccessHandler() {
		OidcClientInitiatedServerLogoutSuccessHandler oidcLogoutSuccessHandler =
				new OidcClientInitiatedServerLogoutSuccessHandler(this.clientRegistrationRepository) {
					@Override
					public Mono<Void> onLogoutSuccess(WebFilterExchange exchange, Authentication authentication) {
						// https://stackoverflow.com/q/15988323/1098564
						// logout was called and proxied, let's default redirection to "origin"
						List<String> origin = exchange.getExchange().getRequest().getHeaders().get(HttpHeaders.ORIGIN);
						// https://stackoverflow.com/q/22397072/1098564
						setPostLogoutRedirectUri(URI.create(origin.isEmpty() || "null".equals(origin.get(0)) ?
								"http://localhost:" + serverPort :
								origin.get(0)));
						return super.onLogoutSuccess(exchange, authentication);
					}
				};
		return oidcLogoutSuccessHandler;
	}
}

@Configuration
class WebConfig implements WebFluxConfigurer {

	@Bean
	RouterFunction<ServerResponse> routerFunction(GatewayHandler gatewayHandler) {
		return route(GET("/api/user"), gatewayHandler::getCurrentUser)
				.andRoute(GET("/private"), gatewayHandler::getPrivate);
	}
}

@Component
class GatewayHandler {

	public Mono<ServerResponse> getCurrentUser(ServerRequest request) {
		return request.principal()
				.map(p -> ((OAuth2AuthenticationToken)p).getPrincipal())
				.flatMap(n -> ok().bodyValue(n.getAttribute("preferred_username")));
	}

	public Mono<ServerResponse> getPrivate(ServerRequest serverRequest) {
		return ServerResponse.temporaryRedirect(URI.create("/")).build();
	}
}

// see https://github.com/spring-projects/spring-boot/issues/9785
@Component
class CustomWebFilter implements WebFilter {
	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		if (exchange.getRequest().getURI().getPath().equals("/")) {
			return chain.filter(exchange.mutate().request(exchange.getRequest().mutate().path("/index.html").build()).build());
		}

		return chain.filter(exchange);
	}
}