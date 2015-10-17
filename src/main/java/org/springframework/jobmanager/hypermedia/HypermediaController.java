package org.springframework.jobmanager.hypermedia;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resources;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

public class HypermediaController {

    private String filter;

    public HypermediaController(String filter) {

        this.filter = filter;
    }

    @Autowired
    private RequestMappingHandlerMapping handlerMapping;

    @RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Resources<Object> invoke() {

        // Find controllers to map to the root
        List<Link> links = handlerMapping.getHandlerMethods().entrySet().stream()
                .filter(a -> a.getKey().getPatternsCondition().getPatterns().stream()
                        .anyMatch(b -> b.contains(filter)))
                .map(a -> {
                    Map.Entry<String, String> tuple;

                    String path = a.getKey().getPatternsCondition()
                            .getPatterns().toArray()[0].toString();

                    tuple = new AbstractMap.SimpleEntry<>(a.getValue()
                            .getMethod().getName(), path);

                    return tuple;
                })
                .map(a -> new Link(linkTo(Object.class).toUri().toString().concat(a.getValue())).withRel(a.getKey()))
                .collect(Collectors.toList());

        return new Resources<>(Collections.emptySet(), links);
    }
}
