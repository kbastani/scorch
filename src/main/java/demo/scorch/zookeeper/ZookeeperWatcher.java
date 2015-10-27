package demo.scorch.zookeeper;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;

import java.util.Arrays;

/**
 * The default Zookeeper watcher for event processing from the
 * Zookeeper cluster. This watcher is responsible for monitoring
 * the event stream that is received from the Scorch cluster.
 *
 * @author Kenny Bastani
 */
public class ZookeeperWatcher implements Watcher {

    private final static Log log = LogFactory.getLog(ZookeeperWatcher.class);



    @Override
    public void process(WatchedEvent event) {
        log.info(Arrays.asList(event, event.getPath(), event.getState(), event.getType()));
    }
}
