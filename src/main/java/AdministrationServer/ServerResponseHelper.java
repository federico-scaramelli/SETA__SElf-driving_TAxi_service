package AdministrationServer;

import TaxiNetwork.Taxi;

import java.util.ArrayList;
import java.util.HashMap;

public class ServerResponseHelper
{
    private static ServerResponseHelper instance;

    private ServerResponseHelper() {}

    public static ServerResponseHelper getInstance()
    {
        if (instance == null)
        {
            instance = new ServerResponseHelper();
        }
        return instance;
    }
}
