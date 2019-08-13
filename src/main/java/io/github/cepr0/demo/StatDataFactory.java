package io.github.cepr0.demo;

import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class StatDataFactory {

	private static final AtomicInteger COUNTER1 = new AtomicInteger();
	private static final AtomicInteger COUNTER2 = new AtomicInteger();
	private static final AtomicInteger COUNTER3 = new AtomicInteger();

	public StatData getData() {
		return new StatData(
				randomNext(COUNTER1),
				randomNext(COUNTER2),
				randomNext(COUNTER3)
		);
	}

	private int randomNext(AtomicInteger counter) {
		ThreadLocalRandom r = ThreadLocalRandom.current();
		return counter.addAndGet(r.nextInt(1, 11));
	}
}
