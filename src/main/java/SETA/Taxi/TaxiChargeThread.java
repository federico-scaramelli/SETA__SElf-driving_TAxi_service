package SETA.Taxi;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class TaxiChargeThread extends Thread
{
    TaxiData myData;
    ManagedChannel channel;


    public TaxiChargeThread(TaxiData myData)
    {
        this.myData = myData;
    }


    @Override
    public void run()
    {
        // Recharge!
        try {
            Thread.sleep(1000);
        } catch (Exception e) {}

        myData.setBattery(100);
        System.out.println(myData);

        // Recharge completed, notify enqueued taxis
        for (TaxiChargingRequest t : TaxiProcess.chargingQueue)
        {
            channel = ManagedChannelBuilder.forTarget("localhost:" + t.taxiPort).usePlaintext().build();

            TaxiRpcChargingReplyThread replyThread = new TaxiRpcChargingReplyThread(myData, t.taxiId, t.taxiPort);
            replyThread.start();
        }

        // And terminate the charging process
        myData.isCharging = false;
        myData.queuedForCharging = false;
    }
}
