package SETA.Taxi;

import java.util.Objects;
import java.util.Scanner;

public class TaxiInputThread extends Thread
{
    TaxiData myData;

    public TaxiInputThread(TaxiData myData) { this.myData = myData; }

    @Override
    public void run()
    {
        while (true) {
            Scanner scan = new Scanner(System.in);
            String command = scan.nextLine();

            if (Objects.equals(command, "quit")) {
                System.out.println("Waiting to quit...");
                synchronized (myData.isExiting) {
                    myData.isExiting = true;
                    synchronized (myData.isRiding) {
                        myData.exited = true;
                    }
                }
                TaxiProcess.notifyQuit();
                break;
            } else {
                System.out.println("\n@WARNING! Command not valid. \n-->Insert 'quit' to quit the Smart City.");
            }
        }
    }
}
