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
    final TaxiData myData;
    final TaxiRidesData myRidesData;
    final TaxiChargingData myChargingData;
    final TaxiData otherTaxiServer;
    SETA.RideRequest request;
    final ManagedChannel channel;

    public TaxiRpcCompetitionThread(TaxiData myData, TaxiRidesData myRidesData, TaxiChargingData myChargingData,
                                    TaxiData otherTaxiServer,
                                    RideRequest request)
    {
        this.myData = myData;
        this.myRidesData = myRidesData;
        this.myChargingData = myChargingData;
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
                .setStartPos(Position.newBuilder().setX(request.startingPos.x).setY(request.startingPos.y).build())
                .build();

        // Call the competeForRide method on the server of another taxi
        stub.withDeadlineAfter(5, TimeUnit.SECONDS)
            .competeForRide(requestData, new StreamObserver<InterestedToCompetition>()
        {
            @Override
            public void onNext(InterestedToCompetition interestToComp)
            {
                boolean interest = interestToComp.getInterested();
                System.out.println("ACK [" + interest + "]! From " + otherTaxiServer.getID()
                                    + " about [Ride " + request.ID + " competition]");
                if (!interest) {
                    synchronized (myRidesData.competitorsCounter) {
                        // If the other taxi answered with negative interest, delete him from competitors
                        myRidesData.competitorsCounter--;
                    }
                }
            }

            @Override
            public void onError(Throwable t) {
                if (t.getCause() == null) {
                    // Time limit reached to receive ACK from a specific taxi. Delete him from competitors.
                    System.out.println("Taxi " + otherTaxiServer.getID() + " excluded from the competition" +
                                        " since an ACK from it has not been received.");
                    synchronized (myRidesData.competitorsCounter) {
                        myRidesData.competitorsCounter--;
                    }
                } else {
                    System.out.println("ERROR! RPC Competition.");
                    synchronized (myRidesData)
                    {
                        myRidesData.rideCompetitors.remove(otherTaxiServer);
                        myRidesData.competitorsCounter--;
                    }
                    System.out.println(t.getCause());
                    t.printStackTrace();
                }
            }

            @Override
            public void onCompleted()
            {
                channel.shutdownNow();

                synchronized (myData.isQuitting) {
                    if (myData.isQuitting) {
                        System.out.println("Dropping the competition since I'm quitting.");
                        myRidesData.competitionState = TaxiRidesData.RideCompetitionState.Idle;
                        return;
                    }
                }

                synchronized (myChargingData)
                {
                    if (myChargingData.currentRechargeRequest != null) {
                        System.out.println("I'm waiting to recharge. Dropping the competition.");
                        myRidesData.competitionState = TaxiRidesData.RideCompetitionState.Idle;
                        return;
                    }

                    if (myChargingData.chargeCommandReceived) {
                        System.out.println("Explicit request to recharge received. Dropping the competition.");
                        myRidesData.competitionState = TaxiRidesData.RideCompetitionState.Idle;
                        TaxiProcess.startChargingProcess();
                        return;
                    }
                }

                synchronized (myRidesData) {
                    // I'm the winner since no competitors remained valid to take this request
                    if (myRidesData.competitorsCounter == 0)
                    {
                        if (myRidesData.isRiding) return;

                        myRidesData.isRiding = true;
                        try {
                            TaxiProcess.takeRide(request);
                        } catch (MqttException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        });
    }
}
