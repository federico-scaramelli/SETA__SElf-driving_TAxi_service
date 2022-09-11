package SETA.Taxi;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Scanner;

// Thread to receive input on the taxi and handle the quitting process
public class TaxiInputThread extends Thread
{
    TaxiData myData;
    final TaxiRidesData myRidesData;
    final TaxiChargingData myChargingData;
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

            if (Objects.equals(command, "quit") || Objects.equals(command, "q")) {
                System.out.println("Waiting to quit...");
                try {
                    Quit();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                break;
            } else if (Objects.equals(command, "recharge") || Objects.equals(command, "r"))
            {
                synchronized (myData.batteryLevel) {
                    if (myData.getBatteryLevel() == 100) {
                        System.out.println("Your battery is already charged.");
                        continue;
                    }
                }

                synchronized (myChargingData) {
                    if (myChargingData.chargeCommandReceived) {
                        System.out.println("Recharge already requested.");
                        continue;
                    }

                    if (myChargingData.isCharging) {
                        System.out.println("You're already recharging.");
                        continue;
                    }

                    if (myChargingData.currentRechargeRequest != null) {
                        System.out.println("You're already waiting for recharging.");
                        continue;
                    }
                }

                synchronized (myRidesData) {
                    if (myRidesData.isRiding
                            || myRidesData.competitionState == TaxiRidesData.RideCompetitionState.Pending) {
                        System.out.println("Recharge requested. Waiting for it...");
                        myChargingData.chargeCommandReceived = true;
                        continue;
                    }
                }

                TaxiProcess.startChargingProcess();
            } else {
                System.out.println("\n@WARNING! Command not valid. " +
                        "\n-->Insert 'quit' to quit the Smart City." +
                        "\n-->Insert 'recharge' to request a recharge.");
            }
        }
    }

    private void Quit() throws InterruptedException {
        synchronized (myData.isQuitting) {
            myData.isQuitting = true;
        }

        // Wait until the ride is completed
        while (myRidesData.isRiding) {}

        System.out.println("QUITTING: I'm not riding.");

        // Wait to terminate charging operations
        while (myChargingData.isCharging || myChargingData.chargeCommandReceived
                || myChargingData.currentRechargeRequest != null) {}

        System.out.println("QUITTING: I'm not charging.");

        // Notify the other taxis
        taxiToNotify = new ArrayList<>();
        ArrayList<TaxiRpcNotifyQuitThread> threads = new ArrayList<>();
        synchronized (taxiToNotify) {
            taxiToNotify.addAll(taxiList);
            for (TaxiData t : taxiToNotify) {
                TaxiRpcNotifyQuitThread notifyQuitThread = new TaxiRpcNotifyQuitThread(myData, t);
                threads.add(notifyQuitThread);
            }
        }
        for (TaxiRpcNotifyQuitThread thread : threads) {
            thread.start();
        }

        for (TaxiRpcNotifyQuitThread thread : threads) {
            thread.join();
        }

        // Wait all the ACKs
        while (!taxiToNotify.isEmpty()) {}
        System.out.println("All the ACK received about my quitting.");

        // Close the RPC server
        TaxiProcess.rpcThread.terminate();
        TaxiProcess.rpcThread.join();

        System.out.println("Leaving the city. Bye bye!");
    }
}
