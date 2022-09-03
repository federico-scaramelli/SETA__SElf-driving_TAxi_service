package SETA.Taxi;

import Utils.GridHelper;
import io.grpc.stub.StreamObserver;
import project.taxi.grpc.TaxiGrpc;
import project.taxi.grpc.TaxiOuterClass.*;

import java.util.ArrayList;
import java.util.Optional;

// SERVER SIDE
public class TaxiRpcServerImpl extends TaxiGrpc.TaxiImplBase
{
    TaxiData myData;
    final ArrayList<TaxiData> myList;

    public TaxiRpcServerImpl(TaxiData myData, ArrayList<TaxiData> myList)
    {
        this.myData = myData;
        this.myList = myList;
    }

    @Override
    public void notifyJoin(StartingTaxiInfo startingInfo, StreamObserver<Ack> ackStreamObserver)
    {
        System.out.println("\nRPC Server: Taxi " + startingInfo.getId() + " has joined the Smart City.");
        // I'm passing also the address but for this project it's hard-coded as localhost so I don't use it
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

        // If the ride is already been taken, answer true (block ride taking)
        if (TaxiProcess.completedRides.contains(requestData.getRideId()))
        {
            System.out.println("Ride " + requestData.getRideId() + " already taken.");
            sendInterest(true, ackStreamObserver);
            return;
        }

        // If it's a request from myself, answer false
        if (requestData.getTaxiId() == myData.getID())
        {
            System.out.println("Ride " + requestData.getRideId() + " received from myself.");
            sendInterest(false, ackStreamObserver);
            return;
        }

        // If you are from another district, or you are not available, send negative interest ack
        if (GridHelper.getDistrict(myData.getPosition()) != requestData.getRideDistrict()   // Different district
            || myData.getBatteryLevel() <= 30                                               // Charging
            || myData.isExiting                                                             // Exiting
            || (myData.isRiding && TaxiProcess.currentRideRequest != null                   // Riding
                && TaxiProcess.currentRideRequest.ID != requestData.getRideId()))
        {
            System.out.println("I'm not available for the request " + requestData.getRideId());
            sendInterest(false, ackStreamObserver);
            return;
        }

        // If you are in the same district, but you have not received the request from mqtt
        if (TaxiProcess.currentRideRequest == null)
        {
            System.out.println("RPC Server: Ride request " + requestData.getRideId() + " not-received from MQTT.\n" +
                    "Waiting for it from MQTT.");

            // Don't send any ACK. Other taxi will send again the request to compete!
            return;
        }

        // If there is a difference between last received ride request from mqtt and the one received from RPC
        if (TaxiProcess.currentRideRequest.ID != requestData.getRideId())
        {
            System.out.println("RPC Server [WARNING!]: Ride request " + requestData.getRideId() +
                    " is different from the one received by MQTT.\n" +
                    "I'm not interested to this request.");
            sendInterest(false, ackStreamObserver);
            return;
        }

        // Actual competition
        if (GridHelper.getDistance(myData.getPosition(),
                TaxiProcess.currentRideRequest.startingPos) > requestData.getDistance())
        {
            System.out.println("I lost the competition for the ride " + requestData.getRideId() +
                    "\nMy distance is " + GridHelper.getDistance(myData.getPosition(),
                    TaxiProcess.currentRideRequest.startingPos) + " while the distance of competitor is "
                    + requestData.getDistance());
            sendInterest(false, ackStreamObserver);
            return;
        }

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
        synchronized (TaxiProcess.completedRides) {
            TaxiProcess.completedRides.add(rideId.getRideId());
        }

        Ack ack = Ack.newBuilder().setAck(true).build();
        ackStreamObserver.onNext(ack);
        ackStreamObserver.onCompleted();
    }

    @Override
    public void notifyQuit(QuitNotification quitNotification, StreamObserver<Null> nullStreamObserver)
    {
        System.out.println("NOTIFICATION! Taxi " + quitNotification.getTaxiId() + " is quitting the city.");

        synchronized (TaxiProcess.currentCompetitors)
        {
            Optional<TaxiData> result = TaxiProcess.currentCompetitors.stream().parallel()
                    .filter(taxi -> taxi.getID() == quitNotification.getTaxiId()).findAny();
            if (result.isPresent()) {
                TaxiProcess.currentCompetitors.remove(result.get());
                System.out.println("Taxi " + quitNotification.getTaxiId() + " removed from competitors list.");
            }
        }

        synchronized (myList)
        {
            Optional<TaxiData> result = myList.stream().parallel()
                    .filter(taxi -> taxi.getID() == quitNotification.getTaxiId()).findAny();
            if (result.isPresent())
                myList.remove(result.get());
            else
                System.out.println("ERROR! Quitting taxi not found on list!");
        }
        nullStreamObserver.onNext(Null.newBuilder().build());
        nullStreamObserver.onCompleted();
    }
}
