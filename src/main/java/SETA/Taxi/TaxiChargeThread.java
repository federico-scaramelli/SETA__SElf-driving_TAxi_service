package SETA.Taxi;

import java.util.ArrayList;

public class TaxiChargeThread extends Thread
{
    final TaxiData myData;
    final TaxiChargingData myChargingData;

    public TaxiChargeThread(TaxiData myData, TaxiChargingData myChargingData)
    {
        this.myData = myData;
        this.myChargingData = myChargingData;
    }


    @Override
    public void run()
    {
        System.out.println("Starting charging...");

        // Recharge!
        try {
            Thread.sleep(10000);
        } catch (Exception e) {}

        synchronized (myData.batteryLevel) {
            myData.setBattery(100);
        }
        System.out.println("Charging terminated. Sending reply to the queue...");

        synchronized (myChargingData) {
            // Recharge completed, notify enqueued taxis
            ArrayList<TaxiRpcChargingReplyThread> threads = new ArrayList<>();
            for (TaxiChargingRequest t : myChargingData.chargingQueue) {
                System.out.println("Sending reply to " + t.taxiId + " at port " + t.taxiPort);

                TaxiRpcChargingReplyThread replyThread = new TaxiRpcChargingReplyThread(myData, t.taxiId, t.taxiPort);
                threads.add(replyThread);
                replyThread.start();
            }
            for (TaxiRpcChargingReplyThread thread : threads) {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            System.out.println("All the taxi in the queue received the message.");

            // And terminate the charging process
            myChargingData.isCharging = false;
            myChargingData.chargeCommandReceived = false;
            myChargingData.currentRechargeRequest = null;
        }

        System.out.println("Charging process ended.");
    }
}
