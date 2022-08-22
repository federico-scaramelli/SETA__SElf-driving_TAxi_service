package AdministrationClient;

import AdministrationServer.MainServer;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

public class AdministrationClient
{
    public static void main(String[] argv)
    {
        Client client = Client.create();
        String serverAddress = "http://" + MainServer.IP + ":" + MainServer.PORT + "/";
        ClientResponse response = null;

        String getList = "statistics/get/taxi_list";
        response = getTaxiListRequest(client, serverAddress + getList);
        System.out.println(response);
    }

    public static ClientResponse getTaxiListRequest(Client client, String url)
    {
        WebResource webResource = client.resource(url);

        try {
            return webResource.get(ClientResponse.class);
        } catch (ClientHandlerException e) {
            System.out.println("Server not available! Reason: " + e);
            return null;
        }
    }
}
