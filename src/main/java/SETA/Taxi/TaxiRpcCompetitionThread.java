package SETA.Taxi;

import Utils.GridHelper;
import SETA.RideRequest;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.eclipse.paho.client.mqttv3.MqttException;
import project.taxi.grpc.TaxiGrpc;
import project.taxi.grpc.TaxiOuterClass.*;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

// Thread to handle a competition about a ride
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
                } else {
                    dropCompetition();
                }
            }

            @Override
            public void onError(Throwable t) {
                if (t.getCause() == null) {
                    // Time limit reached to receive ACK from a specific taxi. Delete him from competitors.
                    /*System.out.println("Taxi " + otherTaxiServer.getID() + " excluded from the competition" +
                                        " since an ACK from it has not been received.");
                    synchronized (myRidesData.competitorsCounter) {
                        myRidesData.competitorsCounter--;
                    }*/
                    System.out.println("ACK not received from taxi " + otherTaxiServer +
                            ".\nDropping the competition to avoid errors and infinite loops.");
                    dropCompetition();
                    TaxiProcess.updateTaxiListAskingRestServer();
                } else {
                    // This handles the exception caused by a not updated list in which it's contained a quit taxi
                    System.out.println("WARNING! RPC Competition.");

                    // Error on the connection with some other taxi. Drop the competition to avoid errors.
                    dropCompetition();

                    // Ask the REST server the updated taxi list
                    TaxiProcess.updateTaxiListAskingRestServer();

                    System.out.println(t.getCause());
                    //t.printStackTrace();
                }
            }

            @Override
            public void onCompleted()
            {
                channel.shutdownNow();

                synchronized (myData.isQuitting) {
                    if (myData.isQuitting) {
                        System.out.println("Dropping the competition since I'm quitting.");
                        dropCompetition();
                        return;
                    }
                }

                synchronized (myChargingData)
                {
                    if (myChargingData.currentRechargeRequest != null) {
                        System.out.println("I'm waiting to recharge. Dropping the competition.");
                        dropCompetition();
                        return;
                    }

                    if (myChargingData.chargeCommandReceived) {
                        System.out.println("Explicit request to recharge received. Dropping the competition.");
                        dropCompetition();
                        TaxiProcess.startChargingProcess();
                        return;
                    }
                }

                synchronized (myRidesData) {
                    // I'm the winner since no competitors remained valid to take this request
                    if (myRidesData.competitorsCounter == 0)
                    {
                        if (myRidesData.isRiding) return;

                        /*System.out.println("I won but wait..........");
                        try { Thread.sleep(10000); } catch (Exception e) {}*/

                        try {
                            TaxiProcess.takeRide(request);
                        } catch (MqttException e) {
                            throw new RuntimeException(e);
                        }

                        dropCompetition();
                    }
                }
            }
        });
    }

    private void dropCompetition()
    {
        synchronized (myRidesData) {
            myRidesData.rideCompetitors.clear();
            myRidesData.competitionState = TaxiRidesData.RideCompetitionState.Idle;
            myRidesData.competitorsCounter = Integer.MAX_VALUE;
        }
    }
}
