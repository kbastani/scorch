package org.springframework.jobmanager.hypermedia;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.hateoas.ResourceSupport;

import java.io.Serializable;

public class HypermediaResource<T extends Serializable> extends ResourceSupport {
    private final T content;

    @JsonCreator
    public HypermediaResource(@JsonProperty("content") T content) {
        this.content = content;
    }

    public T getContent() {
        return content;
    }
}
