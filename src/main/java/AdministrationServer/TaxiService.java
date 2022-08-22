package AdministrationServer;

import TaxiNetwork.Taxi;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

@Path("taxi")
public class TaxiService
{
    @Path("add")
    @POST
    @Produces( {"application/json", "application/xml"} )
    @Consumes( {"application/json", "application/xml"} )
    public Response addTaxi(Taxi taxi)
    {
        System.out.println("Request to add a new taxi received...");
        SmartCityManager smartCity = SmartCityManager.getInstance();
        if (smartCity.addTaxi(taxi))
        {
            ServerResponseHelper responseHelper = ServerResponseHelper.getInstance();
            return Response.ok().entity(responseHelper).build();
        }
        return Response.status(Response.Status.CONFLICT).build();
    }

    @Path("remove/{id}")
    @DELETE
    @Consumes( {"text/plain"} )
    public Response removeTaxi(@PathParam("id") Integer id)
    {
        System.out.println("Request to remove a taxi received...");
        SmartCityManager smartCity = SmartCityManager.getInstance();
        if (smartCity.removeTaxi(id))
        {
            return Response.ok().build();
        }
        return Response.status(Response.Status.NOT_FOUND).build();
    }
}
