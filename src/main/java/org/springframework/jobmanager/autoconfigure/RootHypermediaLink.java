package org.springframework.jobmanager.autoconfigure;

import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;

import java.lang.annotation.*;

/**
 * The {@link RootHypermediaLink} annotation is used to mark a {@link Controller}
 * class as a hypermedia link that will be added to the root resource of a
 * hypermedia enabled application.
 *
 * @author Kenny Bastani
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface RootHypermediaLink {
    /**
     * The name of the link that will be included in the hypermedia enabled
     * root endpoint of the application.
     *
     * @return the name of the link to be added to the root resource
     */
    String value() default "";
}
