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
    private int requestCount = 0;
    private int requestTryingLimit = 3;
    final ManagedChannel channel;

    public TaxiRpcCompetitionThread(TaxiData myData, TaxiData otherTaxiServer, RideRequest request)
    {
        this.myData = myData;
        this.otherTaxiServer = otherTaxiServer;
        this.request = request;
        channel = ManagedChannelBuilder.forTarget("localhost:" + otherTaxiServer.getPort()).usePlaintext().build();}

    @Override
    public void run()
    {
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
                // Remove from the list not interested or losing taxis
                if (!interest)
                {
                    synchronized (TaxiProcess.currentCompetitors) {
                        TaxiProcess.currentCompetitors.remove(otherTaxiServer);
                    }
                }
                // If my taxi is the only competitor remained, take the ride
                if (TaxiProcess.currentCompetitors.size() == 1
                    && TaxiProcess.currentCompetitors.contains(myData))
                {
                    TaxiProcess.startRide(request);
                }
            }

            @Override
            public void onError(Throwable t) {
                System.out.println("ERROR! " + t.getCause());

                System.out.println("TIMEOUT! No ACK received from " + otherTaxiServer.getID() +
                        "\nThis was the request " + requestCount + "...");
                if (requestCount < requestTryingLimit) {
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
        requestCount++;
    }
}
