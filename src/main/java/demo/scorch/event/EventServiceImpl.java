package demo.scorch.event;

import demo.scorch.job.Job;
import demo.scorch.zookeeper.ZookeeperClient;
import org.apache.zookeeper.CreateMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * An event service ingests events and dispatches the events to the state machine
 * the {@link Job} object graph.
 *
 * @author Kenny Bastani
 */
@Service
public class EventServiceImpl implements EventService {
    private ZookeeperClient zookeeperClient;

    @Autowired
    public EventServiceImpl(ZookeeperClient zookeeperClient) {
        this.zookeeperClient = zookeeperClient;
    }

    /**
     * Retrieve the available {@link DomainType}
     *
     * @return the set of available {@link DomainType}
     */
    @Override
    public List<DomainType> getDomainTypes() {
        return null;
    }

    /**
     * Retrieve the available {@link EventType} for a {@link DomainType}.
     *
     * @param domainType
     * @return the set of available {@link EventType}
     */
    @Override
    public List<EventType> getEventTypes(DomainType domainType) {
        return null;
    }

    /**
     * Notify a state machine with a given event that will alter the current state of the machine.
     *
     * @param event is the event object
     */
    @Override
    public boolean sendEvent(Event event) {
        return zookeeperClient.save(event, CreateMode.PERSISTENT_SEQUENTIAL);
    }
}
