package SETA;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import project.taxi.grpc.TaxiGrpc;
import project.taxi.grpc.TaxiOuterClass;

public class TaxiCompetitionThread extends Thread
{
    TaxiData myData;
    TaxiData otherTaxiServer;
    RideRequest request;

    public TaxiCompetitionThread(TaxiData myData, TaxiData otherTaxiServer, RideRequest request)
    {
        this.myData = myData;
        this.otherTaxiServer = otherTaxiServer;
        this.request = request;
    }

    @Override
    public void run()
    {
        final ManagedChannel channel =
                ManagedChannelBuilder.forTarget("localhost:" + otherTaxiServer.getPort()).usePlaintext().build();

        // Creating an asynchronous stub on the channel
        TaxiGrpc.TaxiStub stub = TaxiGrpc.newStub(channel);

        TaxiOuterClass.CompeteRequestData requestData = TaxiOuterClass.CompeteRequestData.newBuilder()
                .setTaxiId(myData.getID())
                .setRideId(request.ID)
                .setBattery(myData.getBatteryLevel())
                .setDistance(GridHelper.getDistance(myData.getPosition(), request.startingPos))
                .build();

        // Call the competeForRide method on the server of another taxi
        stub.competeForRide(requestData, new StreamObserver<TaxiOuterClass.Ack>() {
            @Override
            public void onNext(TaxiOuterClass.Ack value) {
                System.out.println("ACK! From " + otherTaxiServer.getID()
                        + " about [Ride " + request.ID + " competition]");
            }

            @Override
            public void onError(Throwable t) {
                System.out.println("ERROR! " + t.getCause());
                t.printStackTrace();
            }

            @Override
            public void onCompleted() {
                //System.out.println("Completed RPC client [Ride " + request.ID + " competition]");
                channel.shutdownNow();
            }
        });
    }

}
