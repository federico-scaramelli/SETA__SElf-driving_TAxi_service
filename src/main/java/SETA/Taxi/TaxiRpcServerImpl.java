package SETA.Taxi;

import SETA.RideRequest;
import Utils.GridHelper;
import io.grpc.stub.StreamObserver;
import project.taxi.grpc.TaxiGrpc;
import project.taxi.grpc.TaxiOuterClass.*;

import java.util.ArrayList;
import java.util.Optional;

// SERVER SIDE
public class TaxiRpcServerImpl extends TaxiGrpc.TaxiImplBase
{
    final TaxiData myData;
    final TaxiRidesData myRidesData;
    final TaxiChargingData myChargingData;
    final ArrayList<TaxiData> myList;

    public TaxiRpcServerImpl(TaxiData myData, TaxiRidesData myRidesData, TaxiChargingData myChargingData,
                             ArrayList<TaxiData> myList)
    {
        this.myData = myData;
        this.myRidesData = myRidesData;
        this.myChargingData = myChargingData;
        this.myList = myList;
    }

    @Override
    public void notifyJoin(StartingTaxiInfo startingInfo, StreamObserver<Ack> ackStreamObserver)
    {
        System.out.println("\nRPC Server: Taxi " + startingInfo.getId() + " has joined the Smart City.");
        // I'm passing also the address but for this project it's hard-coded as localhost, so I don't use it
        TaxiData newTaxi = new TaxiData(startingInfo.getId(), startingInfo.getPort());
        newTaxi.setPosition(new GridCell(startingInfo.getPos().getX(), startingInfo.getPos().getY()));
        newTaxi.setBattery(startingInfo.getBattery());

        // Sync to allow writing of one taxi at a time
        synchronized (myList) {
            myList.add(newTaxi);
        }

        Ack ack = Ack.newBuilder()
                .setAck(true)
                .build();

        System.out.println(myList);

        // Send the ACK to the client
        ackStreamObserver.onNext(ack);
        ackStreamObserver.onCompleted();
    }

    @Override
    public void competeForRide(CompeteRequestData requestData,
                               StreamObserver<InterestedToCompetition> ackStreamObserver)
    {
        System.out.println("\nRPC Server: Received a request from " + requestData.getTaxiId()
                                        + " to compete for ride " + requestData.getRideId());

        try {
            Thread.sleep(0);
        } catch (Exception e) {}

        // If the ride is already been taken, answer true (block taking the ride from some other taxi)
        synchronized (myRidesData.completedRides) {
            if (myRidesData.completedRides.contains(requestData.getRideId())) {
                System.out.println("Ride " + requestData.getRideId() + " already taken.");
                sendInterest(true, ackStreamObserver);
                return;
            }
        }

        RideRequest lastReceivedRequest = myRidesData.currentRideRequest;

        // If you are / you are next to charge the taxi, drop the competition answering false
        synchronized (myChargingData)
        {
            if (myChargingData.isCharging) {
                System.out.println("Ride " + requestData.getRideId() + " refused. I'm in charging.");
                sendInterest(true, ackStreamObserver);
                return;
            }
            if (myChargingData.chargeCommandReceived) {
                System.out.println("Ride " + requestData.getRideId() + " refused. I've received the recharge command.");
                sendInterest(true, ackStreamObserver);
                return;
            }
            if (myChargingData.currentRechargeRequest != null){
                System.out.println("Ride " + requestData.getRideId() + " refused. I'm in queue to charge.");
                sendInterest(true, ackStreamObserver);
                return;
            }
        }

        // If it's a request from myself, answer false
        if (requestData.getTaxiId() == myData.getID())
        {
            System.out.println("Ride " + requestData.getRideId() + " received from myself.");
            sendInterest(false, ackStreamObserver);
            return;
        }

        // If I'm in another district, drop the competition answering false
        if (GridHelper.getDistrict(myData.getPosition()) != requestData.getRideDistrict())
        {
            System.out.println("Ride " + requestData.getRideId() + " refused. I'm in another district.");
            sendInterest(false, ackStreamObserver);
            return;
        }

        // If I'm quitting, drop the competition answering false
        synchronized (myData.isQuitting) {
            if (myData.isQuitting)
            {
                System.out.println("Ride " + requestData.getRideId() + " refused. I'm quitting the Smart City.");
                sendInterest(false, ackStreamObserver);
                return;
            }
        }

        synchronized (myRidesData.isRiding) {
            if (myRidesData.isRiding)
            {
                System.out.println("Ride " + requestData.getRideId() + " refused. I'm in a ride.");
                sendInterest(false, ackStreamObserver);
                return;
            }
        }


        /*if (myRidesData.currentRideRequest == null)
        {
            System.out.println("RPC Server: Ride request " + requestData.getRideId() + " not-received from MQTT.\n" +
                    "Waiting for it from MQTT.");

            // Don't send any ACK. Other taxi will send again the request to compete!
            return;
        }
        // If there is a difference between last received ride request from mqtt and the one received from RPC
        if (myRidesData.currentRideRequest.ID != requestData.getRideId())
        {
            System.out.println("RPC Server [WARNING!]: Ride request " + requestData.getRideId() +
                    " is different from the one received by MQTT.\n" +
                    "I'm not interested to this request.");
            sendInterest(false, ackStreamObserver);
            return;
        }*/

        // If the last received ride request from the broker is different from the one received via RPC, warning
        if (lastReceivedRequest.ID != requestData.getRideId())
        {
            System.out.println("WARNING! The last request received from the broker is different from the one" +
                    " of this request, thus " + requestData.getRideId() + ". " +
                    "Answering true to block the ride to be taken. If the ride is never been accomplished, SETA" +
                    " will send it again and I'll receive it, allowing the competition.");
            sendInterest(true, ackStreamObserver);
            return;
        }

        // Actual competition
        if (GridHelper.getDistance(myData.getPosition(),
                lastReceivedRequest.startingPos) > requestData.getDistance())
        {
            System.out.println("I lost the competition for the ride " + requestData.getRideId() +
                                "\nMy distance is " + GridHelper.getDistance(myData.getPosition(),
                                lastReceivedRequest.startingPos) + " while the distance of competitor is "
                                + requestData.getDistance());
            sendInterest(false, ackStreamObserver);
            return;
        }

        // The battery level can not change while a competition is in act, sync not needed
        if (myData.getBatteryLevel() < requestData.getBattery()
                || (myData.getBatteryLevel() == requestData.getBattery()
                && myData.getID() < requestData.getTaxiId()))
        {
            System.out.println("I lost the competition for the ride " + requestData.getRideId());
            sendInterest(false, ackStreamObserver);
        } else {
            System.out.println("I'm competing for the ride " + requestData.getRideId());
            sendInterest(true, ackStreamObserver);
        }

        //try {Thread.sleep(7000);}catch (Exception e){}
    }

    private void sendInterest (boolean interest, StreamObserver<InterestedToCompetition> ackStreamObserver)
    {
        InterestedToCompetition ack = InterestedToCompetition.newBuilder()
                .setInterested(interest)
                .build();
        ackStreamObserver.onNext(ack);
        ackStreamObserver.onCompleted();
    }

    @Override
    public void confirmRideTaken(RideId rideId, StreamObserver<Ack> ackStreamObserver)
    {
        synchronized (myRidesData.completedRides) {
            myRidesData.completedRides.add(rideId.getRideId());
        }

        Ack ack = Ack.newBuilder().setAck(true).build();
        ackStreamObserver.onNext(ack);
        ackStreamObserver.onCompleted();
    }

    @Override
    public void notifyQuit(QuitNotification quitNotification, StreamObserver<Null> nullStreamObserver)
    {
        System.out.println("\nNOTIFICATION! Taxi " + quitNotification.getTaxiId() + " is quitting the city.");

        // Remove it from competitor list
        synchronized (myRidesData)
        {
            Optional<TaxiData> result = myRidesData.rideCompetitors.stream().parallel()
                    .filter(taxi -> taxi.getID() == quitNotification.getTaxiId()).findAny();
            if (result.isPresent()) {
                myRidesData.rideCompetitors.remove(result.get());
                System.out.println("Taxi " + quitNotification.getTaxiId() + " removed from competitors list.");
            }
        }

        // Remove it from taxi list
        synchronized (myList)
        {
            Optional<TaxiData> result = myList.stream().parallel()
                    .filter(taxi -> taxi.getID() == quitNotification.getTaxiId()).findAny();
            if (result.isPresent())
                myList.remove(result.get());
            else
                System.out.println("\nERROR! Quitting taxi not found in the taxi list!");
        }
        nullStreamObserver.onNext(Null.newBuilder().build());
        nullStreamObserver.onCompleted();
    }

    @Override
    public void requestCharging(ChargingRequest request, StreamObserver<Ack> ackStreamObserver)
    {
        if (myChargingData.logicalClock < request.getTimestamp())
        {
            // Increment logical clock value wtf to the received one since the received request has a grater timestamp
            myChargingData.logicalClock = request.getTimestamp() + myChargingData.logicalClockOffset;
        } else {
            // Otherwise increment the clock with your offset since you received a message
            myChargingData.logicalClock += myChargingData.logicalClockOffset;
        }


        // Request from me, say OK
        if (request.getTaxiId() == myData.getID())
        {
            Ack ack = Ack.newBuilder().setAck(true).build();
            ackStreamObserver.onNext(ack);
            ackStreamObserver.onCompleted();
            return;
        }

        // Different district -> Send OK since I'm not interested on charging in that station
        if (GridHelper.getDistrict(myData.getPosition()) != request.getDistrict())
        {
            Ack ack = Ack.newBuilder().setAck(true).build();
            ackStreamObserver.onNext(ack);
            ackStreamObserver.onCompleted();
            return;
        }

        // I'm not interested on battery recharging, say OK
        if (!myChargingData.isCharging && myChargingData.currentRechargeRequest == null)
        {
            Ack ack = Ack.newBuilder().setAck(true).build();
            ackStreamObserver.onNext(ack);
            ackStreamObserver.onCompleted();
            return;
        }

        TaxiChargingRequest receivedRequest =
                new TaxiChargingRequest(request.getTaxiId(), request.getTaxiPort(), request.getTimestamp());

        // I'm charging the battery, enqueue the new request
        if (myChargingData.isCharging)
        {
            myChargingData.chargingQueue.add(receivedRequest);
            System.out.println("\nEnqueueing: " + receivedRequest);
            return;
        }

        // I'm waiting to charge..
        if (myChargingData.currentRechargeRequest != null)
        {
            // My request has the priority wrt the received one (Smaller timestamp)
            if (myChargingData.currentRechargeRequest.compareTo(receivedRequest) > 0)
            {
                // Enqueue
                myChargingData.chargingQueue.add(receivedRequest);
                System.out.println(myChargingData.chargingQueue);
            } else {
                // Say OK since you're request has a greater timestamp
                Ack ack = Ack.newBuilder().setAck(true).build();
                ackStreamObserver.onNext(ack);
                ackStreamObserver.onCompleted();
            }
        }
    }

    @Override
    public void replyCharging(ChargingReply reply, StreamObserver<Null> nullStreamObserver)
    {
        synchronized (myChargingData.chargingCompetitors) {
            myChargingData.chargingCompetitors.remove(reply.getTaxiId());

            // Received all the ACK! Take the recharge station!
            if (myChargingData.chargingCompetitors.isEmpty())
            {
                myChargingData.isCharging = true;
                TaxiChargeThread chargeThread = new TaxiChargeThread(myData, myChargingData);
                chargeThread.start();
            }
        }
    }
}
