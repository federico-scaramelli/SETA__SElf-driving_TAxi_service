package SETA;
import Utils.GridHelper;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static SETA.Seta.completedRides;
import static SETA.Seta.rideQueues;

public class SetaGenerateRidesThread extends Thread
{
    public SetaGenerateRidesThread() {}

    @Override
    public void run()
    {
        int count = 10;
        while (count > 0) {
            for (int i = 0; i < 2; i++) {
                RideRequest request = new RideRequest();
                // Avoid conflicts
                synchronized (Seta.completedRides) {
                    while (Seta.completedRides.contains(request)) {
                        request = new RideRequest();
                    }
                }

                int district = GridHelper.getDistrict(request.startingPos);

                // ====================================================================== debug
                /*while (district != 1) {
                    request = new RideRequest();
                    district = GridHelper.getDistrict(request.startingPos);
                }*/
                // ====================================================================== debug

                synchronized (rideQueues.get(district - 1)) {
                    System.out.println("--> Adding a request to the queue of district " + district);
                    rideQueues.get(district - 1).add(request);

                    //System.out.println("--> Generator: notify!");
                    rideQueues.get(district - 1).notify();
                }
            }

            try {
                Thread.sleep(5000);
            } catch (Exception e) {
            }

            count--;
        }
    }
}
