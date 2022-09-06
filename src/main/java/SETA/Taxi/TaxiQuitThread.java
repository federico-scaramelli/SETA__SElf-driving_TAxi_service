package SETA.Taxi;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Scanner;

public class TaxiQuitThread extends Thread
{
    TaxiData myData;
    public static ArrayList<TaxiData> taxiList;
    public volatile static ArrayList<TaxiData> taxiToNotify;

    public TaxiQuitThread(TaxiData myData, ArrayList<TaxiData> myList)
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
            } else {
                System.out.println("\n@WARNING! Command not valid. \n-->Insert 'quit' to quit the Smart City.");
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

        System.out.println("ADIOS!");
    }
}
