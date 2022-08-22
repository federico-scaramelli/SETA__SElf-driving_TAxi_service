package AdministrationServer;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

@Path("statistics")
public class StatisticsService
{
    @Path ("get/taxi_list")
    @GET
    @Produces ({"text/plain"})
    public Response getTaxiList()
    {
        System.out.println("Taxi list requested...");
        SmartCityManager smartCity = SmartCityManager.getInstance();

        StatisticsManager statsManager = StatisticsManager.getInstance();
        System.out.println( statsManager.getTaxiListString(smartCity.getTaxiList()) );

        return Response.ok().build();
    }
}
