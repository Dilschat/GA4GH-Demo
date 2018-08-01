package uk.ac.ebi.biosamples.mongo.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import uk.ac.ebi.biosamples.service.CustomInstantDeserializer;
import uk.ac.ebi.biosamples.service.CustomInstantSerializer;

@Document
public class MongoSubmission {

	@Id
	@JsonIgnore
	private String id;

	@JsonSerialize(using = CustomInstantSerializer.class)
	@JsonDeserialize(using = CustomInstantDeserializer.class)
	@LastModifiedDate
	@Indexed(background=true)
	private final Instant datetime;
	
	private final Map<String, List<String>> headers;

	@Indexed(background=true)
	private final String url;
	private final String content;

	@Indexed(background=true)
	private final String method;
	
	private MongoSubmission(Instant datetime, String method, String url, Map<String, List<String>> headers, String content){
		this.headers = headers;
		this.url = url;
		this.datetime = datetime;
		this.method = method;
		this.content = content;
	}

	public Instant getDatetime() {
		return datetime;
	}

	public Map<String, List<String>> getHeaders() {
		return headers;
	}

	public String getUrl() {
		return url;
	}

	public String getContent() {
		return content;
	}

	public String getMethod() {
		return method;
	}

    @JsonCreator
	public static MongoSubmission build(
			@JsonProperty("datetime") Instant datetime, @JsonProperty("method") String method, @JsonProperty("url") String url, @JsonProperty("headers") Map<String, List<String>> headers, 
			 @JsonProperty("content") String content) {
		return new MongoSubmission(datetime, method, url, headers, content);
	}
}
