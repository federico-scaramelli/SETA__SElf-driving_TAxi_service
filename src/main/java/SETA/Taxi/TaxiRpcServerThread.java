package SETA.Taxi;

import io.grpc.Server;

import java.io.IOException;

// RPC server
public class TaxiRpcServerThread extends Thread
{
    Server rpcServer;

    public TaxiRpcServerThread(Server sever) {
        rpcServer = sever;
    }

    @Override
    public void run()
    {
        try {
            rpcServer.start();
            System.out.println("\nRPC Server started.");
            rpcServer.awaitTermination();
            System.out.println("RPC Server terminated.");
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }

    public void terminate()
    {
        rpcServer.shutdownNow();
    }

}
