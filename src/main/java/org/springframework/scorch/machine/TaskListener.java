package org.springframework.scorch.machine;

import org.springframework.scorch.event.EventType;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.state.State;
import org.springframework.statemachine.transition.Transition;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class TaskListener extends StateMachineListenerAdapter<Status, EventType> {

    final Object lock = new Object();
    private StateMachine<Status, EventType> stateMachine;

    public TaskListener(StateMachine<Status, EventType> stateMachine) {
        this.stateMachine = stateMachine;
    }

    volatile CountDownLatch stateChangedLatch = new CountDownLatch(1);
    volatile CountDownLatch stateEnteredLatch = new CountDownLatch(2);
    volatile CountDownLatch stateExitedLatch = new CountDownLatch(0);
    volatile CountDownLatch transitionLatch = new CountDownLatch(0);
    volatile int stateChangedCount = 0;
    volatile int transitionCount = 0;
    List<State<Status, EventType>> statesEntered = new ArrayList<State<Status, EventType>>();
    List<State<Status, EventType>> statesExited = new ArrayList<State<Status, EventType>>();

    @Override
    public void stateChanged(State<Status, EventType> from, State<Status, EventType> to) {
        synchronized (lock) {
            stateChangedCount++;
            stateChangedLatch.countDown();
            if(from == null && containsState(to, Status.READY)) {
                // Transition from stored state
                stateMachine.sendEvent(EventType.RUN);
            } else if (containsState(from, Status.READY) && containsState(to, Status.STARTED)) {
                // Transition from stored state
                stateMachine.sendEvent(EventType.END);
            } else if (containsState(from, Status.STARTED) && containsState(to, Status.RUNNING)) {
                // Transition from stored state
                stateMachine.sendEvent(EventType.CONTINUE);
            }
        }
    }

    private boolean containsState(State<Status, EventType> state, Status status) {
        return state.getStates().stream().anyMatch(a -> a.getId() == status);
    }

    @Override
    public void stateEntered(State<Status, EventType> state) {
        synchronized (lock) {
            statesEntered.add(state);
            stateEnteredLatch.countDown();
        }
    }

    @Override
    public void stateExited(State<Status, EventType> state) {
        synchronized (lock) {
            statesExited.add(state);
            stateExitedLatch.countDown();
        }
    }

    @Override
    public void transitionEnded(Transition<Status, EventType> transition) {
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