package SensorPackage;

import TaxiNetwork.TaxiData;

import java.util.*;

public class PM10Buffer implements Buffer
{
    private final Queue<Measurement> measurements = new LinkedList<>();

    public PM10Buffer() {}

    @Override
    public void addMeasurement(Measurement m)
    {
        synchronized (measurements) {
            measurements.add(m);

            if (measurements.size() >= 8) {
                //System.out.println("PM10 buffer: values capacity reached.");
                //System.out.println("Buffer: " + measurements);
                measurements.notify();
            }
        }
    }

    @Override
    public List<Measurement> readAllAndClean()
    {
        synchronized (measurements) {
            if (measurements.size() < 8) {
                try {
                    // Waiting 8 values
                    measurements.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            // Enable this to test internal sync on buffer
            /*try {
                Thread.sleep(5000);
            } catch (Exception e) {
                e.printStackTrace();
            }*/

            List<Measurement> list = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                // Remove the first 4 values
                Measurement m = measurements.poll();
                list.add(m);
            }
            // Add the remaining 4 values
            list.addAll(measurements);
            //System.out.println("Value sent: " + list);
            //System.out.println("Buffer: " + measurements);

            measurements.notify();

            return list;
        }
    }
}
