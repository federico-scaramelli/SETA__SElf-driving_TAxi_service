package SETA.Taxi;

public class TaxiChargeThread extends Thread
{
    TaxiData myData;

    public TaxiChargeThread(TaxiData myData)
    {
        this.myData = myData;
    }


    @Override
    public void run()
    {
        System.out.println("Starting charging...");

        // Recharge!
        try {
            Thread.sleep(10000);
        } catch (Exception e) {}

        myData.setBattery(100);
        System.out.println("Charging terminated. Sending reply to the queue...");

        // Recharge completed, notify enqueued taxis
        for (TaxiChargingRequest t : TaxiProcess.chargingQueue)
        {
            System.out.println("Sending reply to " + t.taxiId + " at port " + t.taxiPort);

            TaxiRpcChargingReplyThread replyThread = new TaxiRpcChargingReplyThread(myData, t.taxiId, t.taxiPort);
            replyThread.start();
        }

        // And terminate the charging process
        myData.isCharging = false;
        TaxiProcess.currentRechargeRequest = null;

        System.out.println("Charging process ended.");
    }
}
