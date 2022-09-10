package SETA;
import Utils.GridHelper;
import static SETA.Seta.rideQueues;

public class SetaGenerateRidesThread extends Thread
{
    public SetaGenerateRidesThread() {}

    @Override
    public void run()
    {
        while (Seta.debugCount > 0) {
            for (int i = 0; i < 2; i++) {
                RideRequest request = new RideRequest();
                // Avoid conflicts
                synchronized (Seta.completedRides) {
                    while (Seta.completedRides.contains(request.ID)         // Already dispatched
                            || rideQueues.get(0).contains(request)          // Already generated
                            || rideQueues.get(1).contains(request)
                            || rideQueues.get(2).contains(request)
                            || rideQueues.get(3).contains(request))
                    {
                        request = new RideRequest();
                    }
                }

                int district = GridHelper.getDistrict(request.startingPos);

                // ====================================================================== debug
                /*int district2 = GridHelper.getDistrict(request.destinationPos);
                while (district != 1 || district2 != 1) {
                    request = new RideRequest();
                    district = GridHelper.getDistrict(request.startingPos);
                    district2 = GridHelper.getDistrict(request.destinationPos);
                }*/
                // ====================================================================== debug

                synchronized (rideQueues.get(district - 1)) {
                    System.out.println("--> Adding a request to the queue of district " + district);
                    rideQueues.get(district - 1).add(request);

                    // Notify the dispatcher!
                    rideQueues.get(district - 1).notify();
                }
            }

            try {
                Thread.sleep(5000);
            } catch (Exception e) {
            }

            // Debug counter to generate a specific amount of requests and verify the final results
            Seta.debugCount--;
        }
    }
}
