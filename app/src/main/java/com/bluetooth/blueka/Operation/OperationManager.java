package com.bluetooth.blueka.Operation;

import java.util.LinkedList;
import java.util.Queue;

public class OperationManager {
    private Queue<Operation> operations = new LinkedList<>();
    private Operation currentOp = null;
    public OperationManager(){

    }
    public synchronized void request(Operation operation){
        operations.add(operation);
        if(currentOp == null){
            currentOp = operations.poll();
            currentOp.performOperation();

        }
    }
    public synchronized void operationCompleted(){
        currentOp = null;
        if(operations.peek() != null){
            currentOp = operations.poll();
            currentOp.performOperation();
        }
    }
}
