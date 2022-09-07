package SETA.Taxi;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Scanner;

public class TaxiInputThread extends Thread
{
    TaxiData myData;
    TaxiRidesData myRidesData;
    TaxiChargingData myChargingData;
    public static ArrayList<TaxiData> taxiList;
    public volatile static ArrayList<TaxiData> taxiToNotify;

    public TaxiInputThread(TaxiData myData, TaxiRidesData myRidesData, TaxiChargingData myChargingData,
                           ArrayList<TaxiData> myList)
    {
        this.myData = myData;
        this.myRidesData = myRidesData;
        this.myChargingData = myChargingData;
        taxiList = myList;
    }

    @Override
    public void run()
    {
        while (true) {
            Scanner scan = new Scanner(System.in);
            String command = scan.nextLine();

            if (Objects.equals(command, "quit")) {
                System.out.println("Waiting to quit...");
                Quit();
                break;
            } else if (Objects.equals(command, "recharge"))
            {
                if (myChargingData.chargeCommandReceived) {
                    System.out.println("Recharge already requested...");
                    continue;
                }

                if (myChargingData.isCharging) {
                    System.out.println("Recharge not available now. You're already recharging.");
                    continue;
                }

                if (myChargingData.currentRechargeRequest != null) {
                    System.out.println("Recharge not available now. You're already enqueued for recharging.");
                    continue;
                }

                if (myRidesData.isRiding || myRidesData.currentRideRequest != null) {
                    System.out.println("Recharge requested. Waiting for it...");
                    myChargingData.chargeCommandReceived = true;
                    continue;
                }

                if (myData.getBatteryLevel() == 100)
                {
                    System.out.println("Your battery is already charged.");
                    continue;
                }

                TaxiProcess.startChargingProcess();
            } else {
                System.out.println("\n@WARNING! Command not valid. " +
                        "\n-->Insert 'quit' to quit the Smart City." +
                        "\n-->Insert 'recharge' to request a recharge.");
            }
        }
    }

    private void Quit()
    {
        synchronized (myData.isQuitting) {
            myData.isQuitting = true;
        }

        taxiToNotify = new ArrayList<>();
        taxiToNotify.addAll(taxiList);

        for (TaxiData t : taxiToNotify)
        {
            TaxiRpcNotifyQuitThread notifyQuitThread = new TaxiRpcNotifyQuitThread(myData, t);
            notifyQuitThread.start();
        }

        // Wait all the ACKs
        while (!taxiToNotify.isEmpty()) {  }
        System.out.println("DAJE!");

        while (myRidesData.isRiding) {}

        while (myChargingData.isCharging) {}

        System.out.println("ADIOS!");
    }
}
