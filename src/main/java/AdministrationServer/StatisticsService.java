package AdministrationServer;

import TaxiNetwork.TaxiData;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.ArrayList;

@Path("statistics")
public class StatisticsService
{
    @Path ("get/taxi_list")
    @GET
    @Produces( {"application/json", "application/xml"} )
    public Response getTaxiList()
    {
        //System.out.println("Taxi list requested...");
        ArrayList<TaxiData> taxiList = SmartCityManager.getInstance().getTaxiList();

        return Response.ok().entity(taxiList).build();
    }

    @Path("get/avg/last_{n}_from_{id}")
    @GET
    @Produces( {"application/json", "application/xml"} )
    public Response getLastNAvgFromSingle(@PathParam("n") int n, @PathParam("id")int id)
    {
        String additionalMsg = "";
        if (StatisticsManager.getInstance().getLocalStatsCount(id) < n)
        {
            int m = StatisticsManager.getInstance().getLocalStatsCount(id);
            if (m == 0)
            {
                return Response.noContent().build();
            }
            additionalMsg = "There are no " + n + " registered local statistics from taxi " + id +
                    ".\nReturned the available " + m + " local statistics average.";
            n = m;
        }

        AvgStatsResponse avgResponse = StatisticsManager.getInstance().getAverageLocalStats(id, n);
        if (avgResponse == null)
        {
            return Response.noContent().build();
        }
        avgResponse.setAdditionalMessage(additionalMsg);
        return Response.ok().entity(avgResponse).build();
    }

    @Path("get/avg/global_from_{t1}_to_{t2}")
    @GET
    @Produces( {"application/json", "application/xml"} )
    public Response getGlobalAvgOnPeriod(@PathParam("t1") long t1, @PathParam("t2") long t2)
    {
        String additionalMsg = "";

        AvgStatsResponse avgResponse = StatisticsManager.getInstance().getAverageGlobalStats(t1, t2);
        if (avgResponse == null)
        {
            return Response.noContent().build();
        }
        avgResponse.setAdditionalMessage(additionalMsg);
        return Response.ok().entity(avgResponse).build();
    }

    @Path("add")
    @POST
    @Consumes( {"application/json", "application/xml"} )
    public Response addLocalStats(Statistics localStats)
    {
        System.out.println("Receiving local statistics...");
        StatisticsManager.getInstance().addLocalStatistics(localStats);
        System.out.println(localStats.toString());
        return Response.ok().build();
    }
}
