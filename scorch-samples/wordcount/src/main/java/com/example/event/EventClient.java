package com.example.event;

import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@FeignClient("scorch")
public interface EventClient {
    @RequestMapping(method = RequestMethod.POST, value = "/v1/event/events",
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    Boolean sendEvent(Event event);
}
