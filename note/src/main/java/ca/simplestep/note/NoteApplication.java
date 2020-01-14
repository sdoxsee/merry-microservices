package ca.simplestep.note;

import org.reactivestreams.Publisher;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.annotation.Id;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.config.WebFluxConfigurer;
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
		return route(GET("/api/notes"), noteHandler::getAll)
				.andRoute(GET("/api/notes/{id}"), noteHandler::getById)
				.andRoute(PUT("/api/notes/{id}"), noteHandler::updateById)
				.andRoute(DELETE("/api/notes/{id}"), noteHandler::deleteById)
				.andRoute(POST("/api/notes"), noteHandler::create);
	}
}

@Component
class NoteHandler {

	private final NoteRepository noteRepository;

	NoteHandler(NoteRepository noteRepository) {
		this.noteRepository = noteRepository;
	}

	Mono<ServerResponse> getById(ServerRequest r) {
		return defaultReadResponse(this.noteRepository.findById(id(r)));
	}

	Mono<ServerResponse> getAll(ServerRequest r) {
		return defaultReadResponse(noteRepository.findAll());
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