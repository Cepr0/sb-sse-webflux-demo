package io.github.cepr0.demo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.Banner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Optional;
import java.util.StringJoiner;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

@Slf4j
@RestController
@SpringBootApplication
public class Application {

	private static final long DELAY = 2;

	private final StatDataFactory dataFactory;

	public Application(StatDataFactory dataFactory) {
		this.dataFactory = dataFactory;
	}

	public static void main(String[] args) {
		new SpringApplicationBuilder(Application.class)
				.bannerMode(Banner.Mode.OFF)
				.run(args);
	}

	@Bean
	public RouterFunction<ServerResponse> indexRouter(@Value("classpath:/static/index.html") Resource index) {
		return route(GET("/"), request -> ok().contentType(MediaType.TEXT_HTML).syncBody(index));
	}

	@Bean
	public Flux<StatData> dataEmitter() {
		return Flux.interval(Duration.ofSeconds(1))
				.map(n -> dataFactory.getData())
				.publish()
				.autoConnect()
				.publishOn(Schedulers.parallel());
	}

	@GetMapping("/stats")
	public Flux<ServerSentEvent> getStats() {
		return dataEmitter()
				.doOnNext(data -> log.info("[i] Sending: {}", data))
				.doOnSubscribe(s -> log.info("[i] New subscription"))
				.doOnCancel(() -> log.info("[i] Canceling..."))
				.map(data -> ServerSentEvent.builder(data).build());
	}

	@PostMapping("/process")
	public Mono<ServerSentEvent<Object>> process(@RequestBody ProcessRequest request) {
		log.info("[i] Received: {}", request.getData());
		var response = new ProcessResponse();

		return validate(request, response)
				.map(error -> Mono.just(ServerSentEvent.builder()
						.event("error")
						.data(error)
						.build())
				)
				.orElseGet(() -> longOperation(response)
						.doOnNext(result -> log.info("[i] Operation result: {}", result))
						.map(result -> ServerSentEvent.<Object>builder(result).build())
				);
	}

	private Mono<ProcessResponse> longOperation(ProcessResponse r) {
		r.setNum(r.getNum() * 2);
		r.setText(r.getText().toUpperCase());
		return Mono.just(r).delayElement(Duration.ofSeconds(DELAY));
	}

	private Optional<String> validate(ProcessRequest request, ProcessResponse response) {
		var error = new StringJoiner("<br>");

		String num = request.get("num");
		if (!StringUtils.hasText(num)) {
			error.add("Property 'num' is absent or empty");
		} else {
			try {
				response.setNum(Long.parseLong(num));
			} catch (NumberFormatException e) {
				error.add("Property 'num' must be an integer");
			}
		}

		String text = request.get("text");
		if (!StringUtils.hasText(text)) {
			error.add("Property 'text' is absent or empty");
		} else {
			if (text.length() > 20) {
				error.add("Property 'text' is too long");
			} else {
				response.setText(text);
			}
		}

		String result = error.toString();
		if (StringUtils.hasText(result)) {
			return Optional.of(result);
		} else {
			return Optional.empty();
		}
	}
}
