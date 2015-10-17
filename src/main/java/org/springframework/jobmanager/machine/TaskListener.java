package org.springframework.jobmanager.machine;

import org.springframework.jobmanager.event.EventType;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.state.State;
import org.springframework.statemachine.transition.Transition;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class TaskListener extends StateMachineListenerAdapter<States, EventType> {

    final Object lock = new Object();

    volatile CountDownLatch stateChangedLatch = new CountDownLatch(1);
    volatile CountDownLatch stateEnteredLatch = new CountDownLatch(2);
    volatile CountDownLatch stateExitedLatch = new CountDownLatch(0);
    volatile CountDownLatch transitionLatch = new CountDownLatch(0);
    volatile int stateChangedCount = 0;
    volatile int transitionCount = 0;
    List<State<States, EventType>> statesEntered = new ArrayList<State<States, EventType>>();
    List<State<States, EventType>> statesExited = new ArrayList<State<States, EventType>>();

    @Override
    public void stateChanged(State<States, EventType> from, State<States, EventType> to) {
        synchronized (lock) {
            stateChangedCount++;
            stateChangedLatch.countDown();
        }
    }

    @Override
    public void stateEntered(State<States, EventType> state) {
        synchronized (lock) {
            statesEntered.add(state);
            stateEnteredLatch.countDown();
        }
    }

    @Override
    public void stateExited(State<States, EventType> state) {
        synchronized (lock) {
            statesExited.add(state);
            stateExitedLatch.countDown();
        }
    }

    @Override
    public void transitionEnded(Transition<States, EventType> transition) {
        synchronized (lock) {
            transitionCount++;
            transitionLatch.countDown();
        }
    }

    public void reset(int c1, int c2, int c3) {
        reset(c1, c2, c3, 0);
    }

    public void reset(int c1, int c2, int c3, int c4) {
        synchronized (lock) {
            stateChangedLatch = new CountDownLatch(c1);
            stateEnteredLatch = new CountDownLatch(c2);
            stateExitedLatch = new CountDownLatch(c3);
            transitionLatch = new CountDownLatch(c4);
            stateChangedCount = 0;
            transitionCount = 0;
            statesEntered.clear();
            statesExited.clear();
        }
    }
}