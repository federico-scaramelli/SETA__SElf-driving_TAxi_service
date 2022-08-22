package AdministrationServer;

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
