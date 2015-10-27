package demo.scorch.hypermedia;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resources;
import org.springframework.http.MediaType;
import demo.scorch.autoconfigure.RootHypermediaLink;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

/**
 * Creates a root hypermedia resource mapping.
 *
 * @author Kenny Bastani
 */
@Controller
@RequestMapping("/")
public class BaseEndpoint implements ApplicationContextAware {

    private ApplicationContext context;

    public BaseEndpoint() {
    }

    @Override
    public void setApplicationContext(ApplicationContext context) {
        this.context = context;
    }

    @RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Resources<Object> invoke() {

        // Find controllers to map to the root
        List<Link> links = getLinks(RootHypermediaLink.class);

        // Add the v1 repositories endpoint
        links.add(new Link(linkTo(Object.class).toUri()
                .resolve("/v1/repositories").toString()).withRel("repositories"));

        return new Resources<>(Collections.emptySet(), links);
    }

    /**
     * Searches for annotations on mapped controller classes and returns
     * a list of hypermedia links.
     *
     * @param clazz is the annotation class
     * @return a list of links to annotated classes
     */
    private List<Link> getLinks(Class<? extends Annotation> clazz) {
        Map<String, Object> beans = context.getBeansWithAnnotation(clazz);
        return (List<Link>) beans.entrySet().stream()
                .map(bean -> linkTo(context.getType(bean.getKey()))
                        .withRel(bean.getKey())).collect(Collectors.toList());
    }
}
