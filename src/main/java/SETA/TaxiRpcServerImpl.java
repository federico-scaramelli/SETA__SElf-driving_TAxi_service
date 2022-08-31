package SETA;

import io.grpc.stub.StreamObserver;
import project.taxi.grpc.TaxiGrpc;
import project.taxi.grpc.TaxiOuterClass.*;

import java.util.ArrayList;

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
        System.out.println("RPC Server: Received a request from " + requestData.getTaxiId()
                                        + " to compete for ride " + requestData.getRideId());

        if (GridHelper.getDistrict(myData.getPosition()) != requestData.getRideDistrict()
                || myData.getBatteryLevel() <= 30
                || myData.isRiding)
        {
            InterestedToCompetition ack = InterestedToCompetition.newBuilder()
                    .setInterested(false)
                    .build();
            ackStreamObserver.onNext(ack);
        } else {
            InterestedToCompetition ack = InterestedToCompetition.newBuilder()
                    .setInterested(true)
                    .build();
            ackStreamObserver.onNext(ack);
        }

        //try {Thread.sleep(7000);}catch (Exception e){}

        ackStreamObserver.onCompleted();
    }

}
