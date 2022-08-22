package TaxiNetwork;
import AdministrationServer.TaxiService;
import com.google.gson.Gson;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

public class TaxiProcess
{
    public static final Gson serializer = new Gson();

    public static void main(String[] argv)
    {
        // My data
        TaxiData myData = new TaxiData();

        // REST Client to communicate with the administration server
        Client client = Client.create();

        String serializedTaxiData = serializer.toJson(myData);

        try {
            WebResource webResource = client.resource(TaxiService.addTaxiAddress);
            ClientResponse response = webResource
                    .accept("application/json")
                    .type("application/json")
                    .post(ClientResponse.class, serializedTaxiData);

            if (response.getStatus() != 200)
            {
                throw new RuntimeException("Failed to add the taxi to the network.\n" +
                        "HTTP Server response:\n" +
                        "--> Error code: " + response.getStatus() + "\n" +
                        "--> Info: " + response.getStatusInfo());
            }

        } catch (Exception e) {
            System.out.println(e.toString());
            System.exit(0);
        }
    }
}
