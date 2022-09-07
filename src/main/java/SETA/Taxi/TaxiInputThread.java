package SETA.Taxi;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Scanner;

public class TaxiInputThread extends Thread
{
    TaxiData myData;
    public static ArrayList<TaxiData> taxiList;
    public volatile static ArrayList<TaxiData> taxiToNotify;

    public TaxiInputThread(TaxiData myData, ArrayList<TaxiData> myList)
    {
        this.myData = myData;
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
                if (myData.explicitChargingRequest) {
                    System.out.println("Recharge already requested...");
                    continue;
                }

                if (myData.isCharging) {
                    System.out.println("Recharge not available now. You're already recharging.");
                    continue;
                }

                if (TaxiProcess.currentRechargeRequest != null) {
                    System.out.println("Recharge not available now. You're already enqueued for recharging.");
                    continue;
                }

                if (myData.isRiding || TaxiProcess.currentRideRequest != null) {
                    System.out.println("Recharge requested. Waiting for it...");
                    myData.explicitChargingRequest = true;
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
        synchronized (myData.isExiting) {
            myData.isExiting = true;
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

        while (myData.isRiding) {}

        while (myData.isCharging) {}

        System.out.println("ADIOS!");
    }
}
