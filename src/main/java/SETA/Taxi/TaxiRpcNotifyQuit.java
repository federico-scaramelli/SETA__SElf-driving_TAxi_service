package SETA.Taxi;

import Utils.GridHelper;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import project.taxi.grpc.TaxiGrpc;
import project.taxi.grpc.TaxiOuterClass;


public class TaxiRpcNotifyQuit  extends Thread
{
    TaxiData myData;
    TaxiData otherTaxiServer;
    final ManagedChannel channel;

    public TaxiRpcNotifyQuit(TaxiData myData, TaxiData otherTaxiServer)
    {
        this.myData = myData;
        this.otherTaxiServer = otherTaxiServer;
        channel = ManagedChannelBuilder.forTarget("localhost:" + otherTaxiServer.getPort()).usePlaintext().build();
    }

    @Override
    public void run()
    {
        TaxiGrpc.TaxiStub stub = TaxiGrpc.newStub(channel);

        TaxiOuterClass.QuitNotification quitNotification = TaxiOuterClass.QuitNotification.newBuilder()
                .setTaxiId(myData.getID())
                .build();

        stub.notifyQuit(quitNotification, new StreamObserver<TaxiOuterClass.Null>()
        {
            @Override
            public void onNext(TaxiOuterClass.Null nullMsg) {}

            @Override
            public void onError(Throwable t) {
                System.out.println("ERROR! Notifying quitting: " + t.getCause());
                t.printStackTrace();
            }

            @Override
            public void onCompleted() {
                channel.shutdownNow();
            }
        });
    }
}
