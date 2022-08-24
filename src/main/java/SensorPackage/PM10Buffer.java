package SensorPackage;

import TaxiNetwork.TaxiData;

import java.util.*;

public class PM10Buffer implements Buffer
{
    private Queue<Measurement> measurements = new LinkedList<>();
    // https://anmolsehgal.medium.com/multi-threading-producer-consumer-pattern-using-wait-notify-3dde8fd49f65
    static final Object key = new Object();

    public PM10Buffer() {}

    @Override
    public void addMeasurement(Measurement m)
    {
        synchronized (key) {
            if (measurements.size() >= 8) {
                System.out.println("PM10 buffer: values capacity reached.");
                System.out.println("Buffer: " + measurements);
                try {
                    key.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            measurements.add(m);

            key.notify();
        }
    }

    @Override
    public List<Measurement> readAllAndClean()
    {
        synchronized (key) {
            while (measurements.size() < 8) {
                try {
                    // Waiting 8 values
                    key.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            List<Measurement> list = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                // Remove the first 4 values
                Measurement m = measurements.poll();
                list.add(m);
            }
            // Add the remaining 4 values
            list.addAll(measurements);
            System.out.println("Value sent: " + list);
            System.out.println("Buffer: " + measurements);

            key.notify();

            return list;
        }
    }



    /*
    These implementations allow to not return any List using an accumulator and a FIFO data structure as buffer

    private TaxiData myTaxi;
    private Queue<Measurement> measurements = new LinkedList<>();
    private double accumulator = 0.0;


    @Override
    public void addMeasurement(Measurement m)
    {
        measurements.add(m);
        accumulator += m.getValue();

        if (measurements.size() == 8) {
            readAllAndClean();
        }
    }

    @Override
    public List<Measurement> readAllAndClean()
    {
        myTaxi.getLocalStatistics().addPM10AverageValue(accumulator / 8.0);
        List<Measurement> list = new ArrayList<>();

        for ( int i = 0; i < 4; i++)
        {
            Measurement m = measurements.poll();
            accumulator -= m.getValue();
            list.add(m);
        }
        return list;
    }
    */
}
