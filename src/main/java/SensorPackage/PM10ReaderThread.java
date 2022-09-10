package SensorPackage;

import AdministrationServer.StatisticsService.Statistics;

import java.util.List;

// Consumer
public class PM10ReaderThread extends Thread
{
    PM10Buffer buffer;
    Statistics stats;

    public PM10ReaderThread(Statistics stats, PM10Buffer buffer) {
        this.stats = stats;
        this.buffer = buffer;
    }

    @Override
    public void run()
    {
        //System.out.println("Starting PM10 sensor buffer consumer...");

        // Read continuously from the buffer with batches of size 8 (sliding window)
        while (true)
        {
            List<Measurement> list = buffer.readAllAndClean();
            //System.out.println("Received 8 values from the PM10 sensor, computing the average...");
            computeAverage(list);
        }
    }

    private void computeAverage(List<Measurement> list)
    {
        try {
            if (list.size() > 8) throw new ArrayIndexOutOfBoundsException();
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("ERROR! You're trying to compute a PM10 average of more then 8 values.");
        }

        double avg = 0.0;
        for (Measurement m : list) {
            avg += m.getValue();
        }
        avg /= list.size();

        addAverageToStatistics(avg, stats);
    }

    private void addAverageToStatistics(double avg, Statistics stats)
    {
        stats.addPM10AverageValue(avg);
    }
}
