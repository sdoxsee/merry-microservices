package ca.simplestep.note;

import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.annotation.Id;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;

import static org.springframework.web.reactive.function.server.RequestPredicates.DELETE;
import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RequestPredicates.PUT;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@SpringBootApplication
public class NoteApplication {

	public static void main(String[] args) {
		SpringApplication.run(NoteApplication.class, args);
	}
}

@Configuration
class WebConfig implements WebFluxConfigurer {

	@Bean
	RouterFunction<ServerResponse> routerFunction(NoteHandler noteHandler) {
		return route(GET("/api/notes"), noteHandler::all)
				.andRoute(GET("/api/notes/{id}"), noteHandler::getById)
				.andRoute(PUT("/api/notes/{id}"), noteHandler::updateById)
				.andRoute(DELETE("/api/notes/{id}"), noteHandler::deleteById)
				.andRoute(POST("/api/notes"), noteHandler::create);
	}

	@Bean
	WebClient policyServiceWebClient() {
		return WebClient.create("http://localhost:8080/");
	}
}

@Component
class NoteHandler {

	private final NoteRepository noteRepository;
	private final PolicyService policyService;

	NoteHandler(NoteRepository noteRepository, PolicyService policyService) {
		this.noteRepository = noteRepository;
		this.policyService = policyService;
	}

	Mono<ServerResponse> getById(ServerRequest r) {
		return defaultReadResponse(this.noteRepository.findById(id(r)));
	}

	Mono<ServerResponse> all(ServerRequest r) {
		return r.principal().flatMap((principal) -> {
			Jwt jwt = ((JwtAuthenticationToken) principal).getToken();
			return policyService.hasPermission(jwt, "CanRead")
				.flatMap(canRead -> canRead ?
						policyService.hasPermission(jwt, "CanReadConfidentialNotes")
							.flatMap(canReadConfidentialNotes -> defaultReadResponse(
									canReadConfidentialNotes ?
											noteRepository.findAll() :
											noteRepository.findByConfidentialFalse())) :
						ServerResponse.status(HttpStatus.FORBIDDEN).build());
		});
	}

	Mono<ServerResponse> deleteById(ServerRequest r) {
		Mono<Note> noteMono = this.noteRepository.findById(id(r))
				.flatMap(n -> this.noteRepository.deleteById(id(r)).thenReturn(n));
		return defaultReadResponse(noteMono);
	}

	Mono<ServerResponse> updateById(ServerRequest r) {
		Flux<Note> noteFlux = r
				.bodyToFlux(Note.class)
				.flatMap(toWrite -> this.noteRepository.save(toWrite));
		return defaultWriteResponse(noteFlux);
	}

	Mono<ServerResponse> create(ServerRequest r) {
		Flux<Note> flux = r
				.bodyToFlux(Note.class)
				.flatMap(toWrite -> this.noteRepository.save(toWrite));
		return defaultWriteResponse(flux);
	}

	private static Mono<ServerResponse> defaultWriteResponse(Publisher<Note> notes) {
		return Mono
			.from(notes)
			.flatMap(n -> ServerResponse
					.created(URI.create("/notes/" + n.getId()))
					.contentType(MediaType.APPLICATION_JSON)
					.build()
			);
	}


	private static Mono<ServerResponse> defaultReadResponse(Publisher<Note> notes) {
		return ServerResponse
				.ok()
				.contentType(MediaType.APPLICATION_JSON)
				.body(notes, Note.class);
	}

	private static Long id(ServerRequest r) {
		return Long.valueOf(r.pathVariable("id"));
	}
}

interface NoteRepository extends ReactiveCrudRepository<Note, Long> {

	@Query("SELECT * FROM note WHERE text = :text")
	Flux<Note> findByText(String text);

	@Query("SELECT * FROM note WHERE confidential = false")
	Flux<Note> findByConfidentialFalse();
}

class Note {

	@Id
	private Long id;
	private String text;
	private boolean confidential;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public boolean isConfidential() {
		return confidential;
	}

	public void setConfidential(boolean confidential) {
		this.confidential = confidential;
	}

	static Note create(String text) {
		return create(text, false);
	}

	static Note create(String text, boolean confidential) {
		Note note = new Note();
		note.text = text;
		note.confidential = confidential;
		return note;
	}
}

@Service
class PolicyService {

	@Value("${app.policy-name}")
	private String appPolicyName;

	private final WebClient webClient;

	public PolicyService(WebClient webClient) {
		this.webClient = webClient;
	}

	@Transactional(readOnly = true)
	public Mono<Boolean> hasPermission(Jwt jwt, String permission) {
		return this.webClient
				.get()
				.uri(uriBuilder -> uriBuilder
						.path("/api/policy-evaluation")
						.queryParam("policy", appPolicyName)
						.queryParam("permission", permission)
						.build())
				.headers(headers -> headers.setBearerAuth(jwt.getTokenValue()))
				.retrieve()
				.bodyToMono(Boolean.class);
	}
}