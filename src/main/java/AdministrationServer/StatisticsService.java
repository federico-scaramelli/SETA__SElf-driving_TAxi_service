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
        AvgStatsResponse avgResponse = StatisticsManager.getInstance().getAverageLocalStats(id, n);

        return Response.ok().entity(avgResponse).build();
    }

    @Path("add")
    @POST
    @Consumes( {"application/json", "application/xml"} )
    public Response addLocalStats(Statistics localStats)
    {
        System.out.println("Receiving local statistics...");
        StatisticsManager.getInstance().AddLocalStatistics(localStats);
        System.out.println(localStats.toString());
        return Response.ok().build();
    }
}
