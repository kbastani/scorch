package org.springframework.scorch.zookeeper;

import org.apache.commons.lang.SerializationUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.zookeeper.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * A Spring bean wrapper for a {@link ZooKeeper} client.
 *
 * @author Kenny Bastani
 */
@Component
public class ZookeeperClient implements AutoCloseable {

    @Value("${spring.cloud.zookeeper.connect-string}")
    private String zookeeperHost;

    @Value("${spring.application.name:scorch}")
    private String applicationName;

    private final static Log log = LogFactory.getLog(ZookeeperClient.class);

    private boolean initialized;
    private ZooKeeper zooKeeper;
    private ZookeeperWatcher zookeeperWatcher;
    private String root;

    /**
     * The bean's initialization method.
     */
    private void init() {
        zookeeperWatcher = new ZookeeperWatcher();
        if (reconnect(zookeeperHost)) {
            // Get or create the root barrier
            getOrCreateRoot();
        }
    }

    /**
     * Gets or creates the root node, which is the name of the Spring application.
     */
    private void getOrCreateRoot() {
        if(root == null)
            root = "/".concat(applicationName);

        // Get or create root node for each required path
        List<String> paths = Arrays.asList(root,
                root.concat("/job"),
                root.concat("/stage"),
                root.concat("/task"));

        paths.forEach(this::createNode);
    }

    private void createNode(String path) {
        // Open zookeeper transaction
        Transaction transaction = zooKeeper.transaction();
        try {
            if (zooKeeper.exists(path, false) == null) {
                transaction.create(path, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                transaction.commit().forEach(log::info);
            }
        } catch (KeeperException | InterruptedException e) {
            log.error(e);
        }
    }

    /**
     * Reconnects to the enclosed {@link ZooKeeper} client.
     *
     * @param zookeeperHost is the host address to the zookeeper cluster
     * @return a flag indicating whether or not the reconnect succeeded
     */
    private boolean reconnect(String zookeeperHost) {
        try {
            zooKeeper = new ZooKeeper(zookeeperHost, 2000, zookeeperWatcher);
            initialized = true;
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            initialized = false;
        }

        return initialized;
    }

    /**
     * Connects the enclosed {@link ZooKeeper} client.
     *
     * @return a flag indicating whether or not the reconnect succeeded
     */
    public boolean connect() {
        return connect(this.zookeeperHost);
    }

    /**
     * Connects the enclosed {@link ZooKeeper} client.
     *
     * @param zookeeperHost is the host address to the zookeeper cluster
     * @return a flag indicating whether or not the reconnect succeeded
     */
    public boolean connect(String zookeeperHost) {
        return reconnect(zookeeperHost);
    }

    /**
     * Get the enclosed {@link ZooKeeper} client.
     *
     * @return the managed {@link ZooKeeper} client
     */
    public ZooKeeper getZooKeeper() {
        return zooKeeper;
    }

    /**
     * Get the enclosed instance of {@link ZookeeperWatcher}, which
     * is responsible for monitoring the state of the enclosed
     * {@link ZooKeeper} client.
     *
     * @return the enclosed {@link ZookeeperWatcher}
     */
    public ZookeeperWatcher getZookeeperWatcher() {
        return zookeeperWatcher;
    }

    /**
     * Get the configured {@link ZooKeeper} cluster host details.
     *
     * @return the configured connection string for the {@link ZooKeeper} client
     */
    public String getZookeeperHost() {
        return zookeeperHost;
    }

    public void setZookeeperHost(String zookeeperHost) {
        this.zookeeperHost = zookeeperHost;
    }

    /**
     * Save the state of an object on the {@link ZooKeeper} cluster.
     *
     * @param object is the object to synchronize on the {@link ZooKeeper} cluster
     */
    public <T extends Distributed> void save(T object) {
        if(initialized) {
            byte[] data = SerializationUtils.serialize(object);

            // Get the distributed object's zookeeper path
            String elementPath = String.format("%s/%s/%s", root,
                    object.getClass().getSimpleName().toLowerCase(), object.getId());

            try {
                // Create a new version of the zookeeper distributed object
                zooKeeper.create(elementPath, data,
                        ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT_SEQUENTIAL);
            } catch (KeeperException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Closes this resource, relinquishing any underlying resources.
     * This method is invoked automatically on objects managed by the
     * {@code try}-with-resources statement.
     * <p>
     * <p>While this interface method is declared to throw {@code
     * Exception}, implementers are <em>strongly</em> encouraged to
     * declare concrete implementations of the {@code close} method to
     * throw more specific exceptions, or to throw no exception at all
     * if the close operation cannot fail.
     * <p>
     * <p> Cases where the close operation may fail require careful
     * attention by implementers. It is strongly advised to relinquish
     * the underlying resources and to internally <em>mark</em> the
     * resource as closed, prior to throwing the exception. The {@code
     * close} method is unlikely to be invoked more than once and so
     * this ensures that the resources are released in a timely manner.
     * Furthermore it reduces problems that could arise when the resource
     * wraps, or is wrapped, by another resource.
     * <p>
     * <p><em>Implementers of this interface are also strongly advised
     * to not have the {@code close} method throw {@link
     * InterruptedException}.</em>
     * <p>
     * This exception interacts with a thread's interrupted status,
     * and runtime misbehavior is likely to occur if an {@code
     * InterruptedException} is {@linkplain Throwable#addSuppressed
     * suppressed}.
     * <p>
     * More generally, if it would cause problems for an
     * exception to be suppressed, the {@code AutoCloseable.close}
     * method should not throw it.
     * <p>
     * <p>Note that unlike the {@link Closeable#close close}
     * method of {@link Closeable}, this {@code close} method
     * is <em>not</em> required to be idempotent.  In other words,
     * calling this {@code close} method more than once may have some
     * visible side effect, unlike {@code Closeable.close} which is
     * required to have no effect if called more than once.
     * <p>
     * However, implementers of this interface are strongly encouraged
     * to make their {@code close} methods idempotent.
     *
     * @throws Exception if this resource cannot be closed
     */
    @Override
    public void close() throws Exception {
        if (initialized) {
            if (zooKeeper.getState().isAlive()) {
                zooKeeper.close();
            }
        }
    }
}
