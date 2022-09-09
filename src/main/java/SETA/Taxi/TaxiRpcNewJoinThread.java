package SETA.Taxi;

import SETA.Taxi.TaxiData;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import project.taxi.grpc.TaxiGrpc;
import project.taxi.grpc.TaxiGrpc.*;
import project.taxi.grpc.TaxiOuterClass;

import java.util.concurrent.TimeUnit;

// Thread to invoke an RPC on all the taxis on the network at the time of my entry
public class TaxiRpcNewJoinThread extends Thread
{
    TaxiData myData;
    TaxiChargingData myChargingData;
    TaxiData otherTaxiServer;

    public TaxiRpcNewJoinThread(TaxiData myData, TaxiChargingData myChargingData, TaxiData otherTaxiServer)
    {
        this.myData = myData;
        this.myChargingData = myChargingData;
        this.otherTaxiServer = otherTaxiServer;
    }

    @Override
    public void run()
    {
        final ManagedChannel channel =
                ManagedChannelBuilder.forTarget("localhost:" + otherTaxiServer.getPort()).usePlaintext().build();

        // Creating an asynchronous stub on the channel
        TaxiStub stub = TaxiGrpc.newStub(channel);

        TaxiOuterClass.StartingTaxiInfo info = TaxiOuterClass.StartingTaxiInfo.newBuilder()
                        .setId(myData.ID)
                        .setAddress(myData.address)
                        .setPort(myData.port)
                        .setPos(TaxiOuterClass.Position.newBuilder()
                                    .setX(myData.getPosition().getX())
                                    .setY(myData.getPosition().getY()))
                        .setBattery(myData.getBatteryLevel())
                        .build();

        // Call the notifyJoin method on the server of another taxi
        stub.withDeadlineAfter(10, TimeUnit.SECONDS)
            .notifyJoin(info, new StreamObserver<TaxiOuterClass.Timestamp>() {
            @Override
            public void onNext(TaxiOuterClass.Timestamp value) {
                System.out.println("ACK! From " + otherTaxiServer.getID() + " about [Taxi join]");
                if (myChargingData.logicalClock < value.getTimestamp()) {
                    myChargingData.logicalClock = value.getTimestamp() + 1;
                    System.out.println("Initial logical clock value updated: " + myChargingData.logicalClock);
                }
            }

            @Override
            public void onError(Throwable t) {
                TaxiProcess.removeTaxiFromList(otherTaxiServer);
                System.out.println("ERROR! New Join RPC Thread! " + t.getCause());

                // Ask the REST server the updated taxi list
                TaxiProcess.updateTaxiListAskingRestServer();

                channel.shutdownNow();
            }

            @Override
            public void onCompleted() {
                //System.out.println("Completed RPC client [Taxi join]");
                channel.shutdownNow();
            }
        });
    }
}
