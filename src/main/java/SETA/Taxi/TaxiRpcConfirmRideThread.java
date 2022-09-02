package SETA.Taxi;

import SETA.RideRequest;
import Utils.GridHelper;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import project.taxi.grpc.TaxiGrpc;
import project.taxi.grpc.TaxiOuterClass;

public class TaxiRpcConfirmRideThread extends Thread
{
    TaxiData otherTaxiServer;
    final ManagedChannel channel;
    SETA.RideRequest request;


    public TaxiRpcConfirmRideThread(TaxiData otherTaxiServer, RideRequest request)
    {
        this.otherTaxiServer = otherTaxiServer;
        this.request = request;
        channel = ManagedChannelBuilder.forTarget("localhost:" + otherTaxiServer.getPort()).usePlaintext().build();
    }


    @Override
    public void run()
    {
        // Creating an asynchronous stub on the channel
        TaxiGrpc.TaxiStub stub = TaxiGrpc.newStub(channel);

        TaxiOuterClass.RideId rideId = TaxiOuterClass.RideId.newBuilder()
                .setRideId(request.ID)
                .build();

        stub.confirmRideTaken(rideId, new StreamObserver<TaxiOuterClass.Ack>() {
            @Override
            public void onNext(TaxiOuterClass.Ack value) {
//                System.out.println("ACK! From " + otherTaxiServer.getID());
            }

            @Override
            public void onError(Throwable t) {
                System.out.println("ERROR RPC! " + t.getCause());
                t.printStackTrace();
            }

            @Override
            public void onCompleted() {
                System.out.println("Completed RPC client [Ride confirmation " + request.ID + "]");
            }
        });
    }

}
