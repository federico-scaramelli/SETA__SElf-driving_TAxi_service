package SETA;

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
    TaxiData otherTaxiServer;

    public TaxiRpcNewJoinThread(TaxiData myData, TaxiData otherTaxiServer)
    {
        this.myData = myData;
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
        stub.notifyJoin(info, new StreamObserver<TaxiOuterClass.Ack>() {
            @Override
            public void onNext(TaxiOuterClass.Ack value) {
                System.out.println("ACK! From " + otherTaxiServer.getID() + " about [Taxi join]");
            }

            @Override
            public void onError(Throwable t) {
                System.out.println("ERROR! " + t.getCause());
                t.printStackTrace();
            }

            @Override
            public void onCompleted() {
                //System.out.println("Completed RPC client [Taxi join]");
                channel.shutdownNow();
            }
        });
    }
}
