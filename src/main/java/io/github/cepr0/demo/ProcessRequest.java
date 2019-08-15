package io.github.cepr0.demo;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

import java.util.HashMap;
import java.util.Map;

public class ProcessRequest {

	private Map<String, String> data = new HashMap<>();

	@JsonAnySetter
	public void setData(String key, String value) {
		this.data.put(key, value);
	}

	@JsonAnyGetter
	public Map<String, String> getData() {
		return data;
	}

	public String get(String key) {
		return data.get(key);
	}
}
