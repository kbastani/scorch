package org.springframework.jobmanager.event;

import org.springframework.http.HttpEntity;
import org.springframework.jobmanager.autoconfigure.RootHypermediaLink;
import org.springframework.jobmanager.hypermedia.HypermediaController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * The {@link EventController} exposes a set of methods from the
 * {@link EventService} that manage the receiving and consequential
 * notification to a given domain object of a {@link org.springframework.statemachine.StateMachine}
 *
 * @author Kenny Bastani
 */
@Controller
@RootHypermediaLink("eventService")
@RequestMapping(value = "/v1/event")
public class EventController extends HypermediaController {

    public EventController() {
        super("/v1/event/");
    }

    @RequestMapping(path = "/events", method = RequestMethod.POST)
    public HttpEntity<?> createEvent(@RequestBody Event event) {
        throw new NotImplementedException();
    }
}
