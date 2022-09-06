package SETA.Taxi;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class TaxiChargeThread extends Thread
{
    TaxiData myData;
    ManagedChannel channel;


    public void TaxiChargeThread(TaxiData myData)
    {
        this.myData = myData;
    }


    @Override
    public void run()
    {
        // Recharge!
        try {
            Thread.sleep(10000);
        } catch (Exception e) {}

        myData.setBattery(100);

        for (TaxiChargingRequest t : TaxiProcess.chargingQueue)
        {
            channel = ManagedChannelBuilder.forTarget("localhost:" + t.taxi.getPort()).usePlaintext().build();

            // Needs a new thread to send reply about charging (like an ACK but it's not an answer)
        }
    }
}
