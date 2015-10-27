# Scorch

This project provides a management API that exposes stateful operations for distributed jobs and tasks. The code used in this application is extended from the [Spring Statemachine](http://projects.spring.io/spring-statemachine/) project samples.

## Usage

Scorch provides consistency guarantees and a global event transaction log that is sequentially ordered using ZooKeeper. Events are sent to Scorch from a cluster of services using a globally unique target ID and an event type. A RabbitMQ message queue is sent durable state change notifications from Scorch as events change the state of an object's state machine.

There is no need to worry about maintaining local state of objects in your cluster. State change notifications are subscribed to and when a notification of the desired state of an object is received, it confirms that a multi-phase commit was made.

The status of a job is also always consistent, allowing subscribers to passively and actively monitor state changes over HTTP or RabbitMQ.

### Consistency

Scorch provides consistency guarantees that make it strongly consistent and fault tolerant.

### Metrics

Scorch nodes run an embedded HTTP server that exposes health metrics using Spring Boot Actuator and embedded Tomcat.

### State management

The diagram below is an example system architecture that shows how Scorch works.

![State Management](http://i.imgur.com/CweJus3.png)

* **Jobs** are created, which can contain **Stages**, which contain **Tasks**.

* Each job is managed as a state machine on the Scorch cluster. State changes are emitted to a cluster of services that are monitoring the state of a job.

* The state of jobs are managed by ingesting a stream of **Events**. Each event affects the state of a job, which drives the total state of the system.

#### Why is this useful?

In a distributed system a commonly encountered problem is guaranteeing the state of a domain object will always be consistent. Take the following example:

##### Example

A user of an online banking platform's website registers for a new account. When the user submits the registration form, a microservices backend orchestrates the creation of the user's customer record in multiple systems across multiple databases.

Each backend service is highly available and in the form of a REST API, meaning that each application is running on multiple servers that are load balanced in a round robin fashion. When user registers, the frontend's microservice will orchestrate the creation of the new user's account. Let's call this service the *UI Service*.

The UI service will first create a record of the user's new account. The initial state of the user's account is **SUBMITTED** and is stored in the *UI Service's* exclusive database.

The *UI Service* now needs to call the *Registration Service* to register the new user with the host system of the bank. The *Registration Service* then needs to orchestrate multiple calls (multi-phase commit) to two other microservices.

* If the response from the *Registration Service* to the *UI Service* fails due to the network or database unavailability, how do you guarantee the state of the user's account before returning a response to the user's browser?

* How do you know which events happened in other services in order to compensate the transaction and roll back any state changes and try again?

### Task management

State machines are replicated across the cluster using ZooKeeper. These state machines rely on the Spring Statemachine project.

![Scorch Architecture](http://i.imgur.com/qetTXFr.png)

## Fallacies of distributed computing

The following points are the famous fallacies of distributed computing. These are statements that are framed as assumptions that tend to always be incorrect when moving from a single node to many nodes.

* The network is reliable.
* Latency is zero.
* Bandwidth is infinite.
* The network is secure.
* Topology doesn't change.
* There is one administrator.
* Transport cost is zero.
* The network is homogeneous.

## Scorch guarantees

* State is guaranteed to be consistent across the cluster.
* Nodes can be removed without loss of data.
* Nodes that are added will not emit state changes until consistent.
* A receipt of an event is a guarantee of stable storage.
* All state change messages are sent once and only once.
* All guarantees provided by Scorch are extended from ZooKeeper guarantees.

## ZooKeeper guarantees

ZooKeeper is a centralized service for providing distributed synchronization. Scorch relies on ZooKeeper to maintain its global transaction log and as a messaging system that keeps that state of all Scorch nodes consistent. The following are the guarantees that ZooKeeper provides.

### Reliable delivery

If a message, m, is delivered by one server, it will be eventually delivered by all servers.

### Total order

If a message is delivered before message b by one server, a will be delivered before b by all servers. If a and b are delivered messages, either a will be delivered before b or b will be delivered before a.

### Causal order

If a message b is sent after a message a has been delivered by the sender of b, a must be ordered before b. If a sender sends c after sending b, c must be ordered after b.

### Ordered delivery

Data is delivered in the same order it is sent and a message m is delivered only after all messages sent before m have been delivered. (The corollary to this is that if message m is lost all messages after m will be lost.)

### No message after close

Once a FIFO channel is closed, no messages will be received from it.


License
================

This library is licensed under the Apache License, Version 2.0.
