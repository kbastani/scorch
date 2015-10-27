package demo.scorch.message;

import demo.scorch.machine.Status;

import java.io.Serializable;

public class StateChange implements Serializable {
    private String targetId;
    private Status sourceState;
    private Status targetState;

    public StateChange() {
    }

    public StateChange(String targetId, Status sourceState, Status targetState) {
        this.targetId = targetId;
        this.sourceState = sourceState;
        this.targetState = targetState;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public Status getSourceState() {
        return sourceState;
    }

    public void setSourceState(Status sourceState) {
        this.sourceState = sourceState;
    }

    public Status getTargetState() {
        return targetState;
    }

    public void setTargetState(Status targetState) {
        this.targetState = targetState;
    }

    @Override
    public String toString() {
        return "StateChange{" +
                "targetId='" + targetId + '\'' +
                ", sourceState=" + sourceState +
                ", targetState=" + targetState +
                '}';
    }
}
