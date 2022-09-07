package SETA.Taxi;

import Utils.GridHelper;
import SETA.RideRequest;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.eclipse.paho.client.mqttv3.MqttException;
import project.taxi.grpc.TaxiGrpc;
import project.taxi.grpc.TaxiOuterClass.*;
import java.util.concurrent.TimeUnit;

public class TaxiRpcCompetitionThread extends Thread
{
    TaxiData myData;
    TaxiData otherTaxiServer;
    SETA.RideRequest request;
    private static int requestCount = 0;
    private int requestTryingLimit = 10;
    final ManagedChannel channel;

    public TaxiRpcCompetitionThread(TaxiData myData, TaxiData otherTaxiServer, RideRequest request)
    {
        this.myData = myData;
        this.otherTaxiServer = otherTaxiServer;
        this.request = request;
        channel = ManagedChannelBuilder.forTarget("localhost:" + otherTaxiServer.getPort()).usePlaintext().build();
    }

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

        /*if (myData.getID() < otherTaxiServer.getID() && myData.getPosition() != otherTaxiServer.getPosition())
            try { Thread.sleep(5000); } catch (Exception e) {}*/

        // Call the competeForRide method on the server of another taxi
        stub.withDeadlineAfter(5, TimeUnit.SECONDS)
            .competeForRide(requestData, new StreamObserver<InterestedToCompetition>()
        {
            @Override
            public void onNext(InterestedToCompetition interestToComp)
            {
                /*synchronized (TaxiProcess.currentCompetitors) {
                    if (!TaxiProcess.currentCompetitors.contains(otherTaxiServer))
                        TaxiProcess.currentCompetitors.remove(otherTaxiServer);
                }*/

                boolean interest = interestToComp.getInterested();
                System.out.println("ACK [" + interest + "]! From " + otherTaxiServer.getID()
                        + " about [Ride " + request.ID + " competition]");
                // Remove from the list not interested or losing taxis
                if (!interest)
                {
                    synchronized (TaxiProcess.rideCompetitors) {
                        TaxiProcess.rideCompetitors.remove(otherTaxiServer);
                    }
                }
            }

            @Override
            public void onError(Throwable t) {
                if (t.getCause() == null) {
                    System.out.println("\nTIMEOUT! No ACK received from " + otherTaxiServer.getID() +
                            "\nThis was the request " + requestCount + "...");
                    if (requestCount < requestTryingLimit) {
                        System.out.println("Sending a new request to compete...");
                        channel.shutdownNow();
                        TaxiRpcCompetitionThread newThread =
                                new TaxiRpcCompetitionThread(myData, otherTaxiServer, request);
                        newThread.start();
                        //run();
                    } else {
                        System.out.println("Taxi " + otherTaxiServer.getID() + " excluded from the competition.");
                    }
                } else {
                    System.out.println(t.getCause());
                }
            }

            @Override
            public void onCompleted() {
                /*System.out.println("Completed RPC client [Ride " + request.ID + " competition with "
                                                                     + otherTaxiServer.getID() + "]");*/
                channel.shutdownNow();

                if (myData.isExiting) {
                    System.out.println("Dropping the competition. I'm quitting.");
                    return;
                }

                if (myData.explicitChargingRequest) {
                    System.out.println("Explicit request to recharge. Dropping the competition.");
                    TaxiProcess.startChargingProcess();
                    return;
                }

                // If I win, take the ride
                synchronized (TaxiProcess.rideCompetitors) {
                    // Ride already taken
                    if (myData.isRiding)
                        return;

                    if (TaxiProcess.rideCompetitors.isEmpty()) {
                        try {
                            Thread.sleep(0);
                            myData.setRidingState(true);
                            TaxiProcess.takeRide(request);
                        } catch (MqttException e) {
                            throw new RuntimeException(e);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        });

        requestCount++;
    }
}
