package SETA;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import project.taxi.grpc.TaxiGrpc;
import project.taxi.grpc.TaxiOuterClass.*;
import java.util.concurrent.TimeUnit;

public class TaxiRpcCompetitionThread extends Thread
{
    TaxiData myData;
    TaxiData otherTaxiServer;
    RideRequest request;
    private int count = 0;
    private int requestTryingLimit = 3;

    public TaxiRpcCompetitionThread(TaxiData myData, TaxiData otherTaxiServer, RideRequest request)
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

        CompeteRequestData requestData = CompeteRequestData.newBuilder()
                .setTaxiId(myData.getID())
                .setRideId(request.ID)
                .setRideDistrict(GridHelper.getDistrict(request.startingPos))
                .setBattery(myData.getBatteryLevel())
                .setDistance(GridHelper.getDistance(myData.getPosition(), request.startingPos))
                .build();

        // Call the competeForRide method on the server of another taxi
        stub.withDeadlineAfter(5, TimeUnit.SECONDS)
                .competeForRide(requestData, new StreamObserver<InterestedToCompetition>() {
            @Override
            public void onNext(InterestedToCompetition interestToComp) {
                boolean interest = interestToComp.getInterested();
                System.out.println("ACK [" + interest + "]! From " + otherTaxiServer.getID()
                        + " about [Ride " + request.ID + " competition]");
                if (interest)
                    TaxiProcess.currentCompetitors.add(otherTaxiServer);
            }

            @Override
            public void onError(Throwable t) {
                System.out.println("ERROR! " + t.getCause());

                System.out.println("TIMEOUT! No ACK received from " + otherTaxiServer.getID() +
                        "\nThis was the request " + count + "...");
                if (count < requestTryingLimit) {
                    System.out.println("\nSending a new request to compete...");
                    channel.shutdownNow();
                    run();
                } else {
                    System.out.println("Taxi " + otherTaxiServer.getID() + " excluded from the competition.");
                }
            }

            @Override
            public void onCompleted() {
                //System.out.println("Completed RPC client [Ride " + request.ID + " competition]");
                channel.shutdown();
            }
        });
        count++;
    }

}
