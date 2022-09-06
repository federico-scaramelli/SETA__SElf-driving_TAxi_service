package SETA.Taxi;

import Utils.GridHelper;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import project.taxi.grpc.TaxiGrpc;
import project.taxi.grpc.TaxiOuterClass.*;

public class TaxiRpcRequestChargingThread extends Thread
{
    TaxiData myData;
    TaxiData otherTaxiServer;
    final ManagedChannel channel;

    public TaxiRpcRequestChargingThread(TaxiData myData, TaxiData otherTaxiServer)
    {
        this.myData = myData;
        this.otherTaxiServer = otherTaxiServer;
        channel = ManagedChannelBuilder.forTarget("localhost:" + otherTaxiServer.getPort()).usePlaintext().build();

        TaxiProcess.chargingQueue.clear();
    }

    @Override
    public void run()
    {
        TaxiGrpc.TaxiStub stub = TaxiGrpc.newStub(channel);

        ChargingRequest request = ChargingRequest.newBuilder()
                .setTaxiId(myData.ID)
                .setDistrict(GridHelper.getDistrict(myData.getPosition()))
                .setTimestamp(TaxiProcess.logicalClock)
                .build();

        stub.requestCharging(request, new StreamObserver<Ack>()
        {
            @Override
            public void onNext(Ack value)
            {
                synchronized (TaxiProcess.chargingRequestReceivers) {
                    TaxiProcess.chargingRequestReceivers.remove(otherTaxiServer);
                }

                System.out.println("\nCharging ACK from " + otherTaxiServer.getID());
            }

            @Override
            public void onError(Throwable t) {
                System.out.println(t.getCause());
                t.printStackTrace();
                channel.shutdownNow();
            }

            @Override
            public void onCompleted()
            {
                channel.shutdownNow();

                // Received all the ACK! Take the recharge station!
                if (TaxiProcess.chargingRequestReceivers.isEmpty())
                {
                    myData.isCharging = true;
                    myData.queuedForCharging = true;
                }
            }
        });
    }
}
