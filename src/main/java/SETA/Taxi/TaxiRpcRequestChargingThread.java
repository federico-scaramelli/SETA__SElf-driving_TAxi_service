package SETA.Taxi;

import Utils.GridHelper;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import project.taxi.grpc.TaxiGrpc;
import project.taxi.grpc.TaxiOuterClass.*;

import java.util.concurrent.TimeUnit;

public class TaxiRpcRequestChargingThread extends Thread
{
    final TaxiData myData;
    final TaxiChargingData myChargingData;
    final TaxiData otherTaxiServer;
    final ManagedChannel channel;

    public TaxiRpcRequestChargingThread(TaxiData myData, TaxiChargingData myChargingData, TaxiData otherTaxiServer)
    {
        this.myData = myData;
        this.myChargingData = myChargingData;
        this.otherTaxiServer = otherTaxiServer;
        channel = ManagedChannelBuilder.forTarget("localhost:" + otherTaxiServer.getPort()).usePlaintext().build();

        myChargingData.chargingQueue.clear();
    }

    @Override
    public void run()
    {
        TaxiGrpc.TaxiStub stub = TaxiGrpc.newStub(channel);

        ChargingRequest request = ChargingRequest.newBuilder()
                .setTaxiId(myData.ID)
                .setTaxiPort(myData.port)
                .setDistrict(GridHelper.getDistrict(myData.getPosition()))
                .setTimestamp(myChargingData.logicalClock)
                .build();

        stub.requestCharging(request, new StreamObserver<Ack>()
        {
            @Override
            public void onNext(Ack value)
            {
                synchronized (myChargingData.chargingCompetitors) {
                    myChargingData.chargingCompetitors.remove(otherTaxiServer.getID());
                }
            }

            @Override
            public void onError(Throwable t) {
                // The error is raised because I don't send any answer in the case the receiver win the charging request
                //System.out.println("ERROR! on RPC Request Charging.");
                channel.shutdownNow();
            }

            @Override
            public void onCompleted()
            {
                // Received all the ACK! Take the recharge station!
                synchronized (myChargingData.chargingCompetitors)
                {
                    if (myChargingData.isCharging)
                        return;

                    if (myChargingData.chargingCompetitors.isEmpty()) {
                        myChargingData.isCharging = true;


                        //try { Thread.sleep(10000); } catch (Exception e) {}


                        TaxiChargeThread chargeThread = new TaxiChargeThread(myData, myChargingData);
                        chargeThread.start();
                    }
                }

                channel.shutdownNow();
            }
        });
    }
}
