package AdministrationServer;

import com.sun.jersey.api.container.httpserver.HttpServerFactory;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;

public class MainServer
{
    public final static String IP = "localhost";
    public final static int PORT = 9797;

    public static String GetAddress()
    {
        return  "http://" + IP + ":" + PORT + "/";
    }

    public static void main(String[] args) throws IOException
    {
        HttpServer httpServer = HttpServerFactory.create(GetAddress());
        httpServer.start();
        System.out.println("Administration server is now running on: " + GetAddress());

        System.out.println("-_-_--> Press any key to stop the server <--_-_-");
        System.in.read();
        System.out.println("Stopping the server...");
        httpServer.stop(0);
        System.out.println("Server has being stopped");

        System.exit(0);
    }
}
