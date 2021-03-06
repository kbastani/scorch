package demo.scorch.event;

import demo.scorch.autoconfigure.RootHypermediaLink;
import demo.scorch.hypermedia.HypermediaController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Optional;

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

    private EventService eventService;

    @Autowired
    public EventController(EventService eventService) {
        super("/v1/event/");
        this.eventService = eventService;
    }

    @RequestMapping(path = "/events", method = RequestMethod.POST)
    public HttpEntity<?> sendEvent(@RequestBody Event event) {
        return Optional.of(eventService.sendEvent(event)).map(u -> new ResponseEntity<>(u, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.BAD_REQUEST));
    }
}
