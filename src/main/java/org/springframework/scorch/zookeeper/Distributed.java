package org.springframework.scorch.zookeeper;

import java.io.Serializable;

/**
 * A contract for distributed objects that have their state shared
 * across a cluster.
 *
 * @author Kenny Bastani
 */
public interface Distributed extends Serializable {

    /**
     * Each distributed object must have an identifier associated with it.
     *
     * @return an object representing the identifier of the distributed object
     */
    Object getId();
}
