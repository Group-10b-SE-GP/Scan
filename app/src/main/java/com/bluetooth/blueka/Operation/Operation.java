package com.bluetooth.blueka.Operation;

public abstract class Operation {
    public String serverID;
    public Operation(String serverID){
        this.serverID = serverID;
    }
    public String getServerID(){
        return serverID;
    }
}
