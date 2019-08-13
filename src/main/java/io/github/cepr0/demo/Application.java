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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

@Slf4j
@RestController
@SpringBootApplication
public class Application {

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
	public Flux<ServerSentEvent<StatData>> getStats() {
		return dataEmitter()
				.doOnNext(data -> log.info("[i] Sending: {}", data))
				.doOnSubscribe(s -> log.info("[i] New subscription"))
				.doOnCancel(() -> log.info("[i] Canceling..."))
				.map(data -> ServerSentEvent.builder(data).build());
	}
}
