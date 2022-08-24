package SensorPackage;

import AdministrationServer.Statistics;
import TaxiNetwork.TaxiData;

import java.util.List;

public class PM10ReaderThread extends Thread
{
    PM10Buffer buffer;
    TaxiData myData;

    public PM10ReaderThread(TaxiData myData, PM10Buffer buffer) {
        this.myData = myData;
        this.buffer = buffer;
    }

    @Override
    public void run()
    {
        System.out.println("Starting PM10 sensor buffer consumer...");

        while (true)
        {
            List<Measurement> list = buffer.readAllAndClean();
            System.out.println("Received 8 values from the PM10 sensor, computing the average...");
            computeAverage(list);
        }
    }

    private void computeAverage(List<Measurement> list)
    {
        double avg = 0.0;
        for (Measurement m : list) {
            avg += m.getValue();
        }
        avg /= 8.0;

        addAverageToStatistics(avg, myData.getLocalStatistics());
    }

    private void addAverageToStatistics(double avg, Statistics stats)
    {
        stats.addPM10AverageValue(avg);
    }
}
