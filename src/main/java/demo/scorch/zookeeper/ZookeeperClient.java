package demo.scorch.zookeeper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.fatboyindustrial.gsonjodatime.*;
import org.apache.zookeeper.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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
    final Gson gson = Converters.registerDateTime(new GsonBuilder()).create();

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
        if (root == null)
            root = "/".concat(applicationName);

        // Get or create root node for each required path
        List<String> paths = Arrays.asList(root.concat("/job"),
                root.concat("/stage"),
                root.concat("/task"),
                root.concat("/event"));

        createNode(root);
        paths.forEach(this::createNode);
    }

    /**
     * Create a node at the path in {@link ZooKeeper}.
     *
     * @param path is the location of the node
     */
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
            if (zooKeeper != null && zooKeeper.getState().isAlive()) {
                return true;
            } else {
                zooKeeper = new ZooKeeper(zookeeperHost, 2000, zookeeperWatcher);
            }
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
        if(connect()) {
            return zooKeeper;
        } else {
            return null;
        }
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
    public <T extends Distributed> boolean save(T object) {
        return save(object, CreateMode.PERSISTENT);
    }

    /**
     * Save the state of an object on the {@link ZooKeeper} cluster.
     *
     * @param object     is the object to synchronize on the {@link ZooKeeper} cluster
     * @param createMode is the mode that the {@link ZooKeeper} node should save the object
     */
    public <T extends Distributed> boolean save(T object, CreateMode createMode) {
        boolean success = false;
        if (connect()) {
            byte[] data = gson.toJson(object).getBytes();

            // Get the distributed object's zookeeper path
            String elementPath = String.format("%s/%s/%s", root,
                    object.getClass().getSimpleName().toLowerCase(), object.getId());

            try {
                // Create a new version of the zookeeper distributed object
                if (zooKeeper.exists(elementPath, false) == null) {
                    zooKeeper.create(elementPath, data, ZooDefs.Ids.OPEN_ACL_UNSAFE, createMode);
                } else {
                    zooKeeper.setData(elementPath, data, -1);
                }

                success = true;
            } catch (KeeperException | InterruptedException e) {
                log.error(e.getMessage(), e);
            }
        }

        return success;
    }

    /**
     * Get an object from the {@link ZooKeeper} cluster.
     *
     * @param clazz is the class that the object should be deserialized to
     * @param key   is the key of the object to get from the {@link ZooKeeper} cluster
     * @param <T>   is the {@link Distributed} object type to get
     * @return a {@link Distributed} object from {@link ZooKeeper}
     */
    public <T extends Distributed> T get(Class<T> clazz, String key) {
        T obj = null;

        if (connect()) {
            // Get the distributed object's zookeeper path
            String elementPath = String.format("%s/%s/%s", root, clazz.getSimpleName().toLowerCase(), key);

            try {
                // Create a new version of the zookeeper distributed object
                obj = gson.fromJson(new String(zooKeeper.getData(elementPath, zookeeperWatcher, null)), clazz);
            } catch (KeeperException | InterruptedException e) {
                log.error(e.getMessage(), e);
            }
        }

        return obj;
    }

    /**
     * Get an object from the {@link ZooKeeper} cluster.
     *
     * @param clazz is the class that the object should be deserialized to
     * @param key   is the key of the object to get from the {@link ZooKeeper} cluster
     * @param <T>   is the {@link Distributed} object type to get
     * @return a {@link Distributed} object from {@link ZooKeeper}
     */
    public <T extends Distributed> T get(Class<T> clazz, String key, boolean watch) {
        T obj = null;

        if (connect()) {
            // Get the distributed object's zookeeper path
            String elementPath = String.format("%s/%s/%s", root, clazz.getSimpleName().toLowerCase(), key);

            try {
                // Create a new version of the zookeeper distributed object
                obj = gson.fromJson(new String(zooKeeper.getData(elementPath, watch ? zookeeperWatcher : null, null)), clazz);
            } catch (KeeperException | InterruptedException e) {
                log.error(e.getMessage(), e);
            }
        }

        return obj;
    }

    /**
     * Closes this resource, relinquishing any underlying resources.
     * This method is invoked automatically on objects managed by the
     * {@code try}-with-resources statement.
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
