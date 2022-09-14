package SETA.Taxi;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import project.taxi.grpc.TaxiGrpc;
import project.taxi.grpc.TaxiOuterClass;

import java.util.concurrent.TimeUnit;

// Thread to notify another taxi about you are quitting
public class TaxiRpcNotifyQuitThread extends Thread
{
    TaxiData myData;
    TaxiData otherTaxiServer;
    final ManagedChannel channel;

    public TaxiRpcNotifyQuitThread(TaxiData myData, TaxiData otherTaxiServer)
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

        stub.withDeadlineAfter(10, TimeUnit.SECONDS)
            .notifyQuit(quitNotification, new StreamObserver<TaxiOuterClass.Null>()
        {
            @Override
            public void onNext(TaxiOuterClass.Null nullMsg) {
                System.out.println("QUITTING OK! From " + otherTaxiServer);
            }

            @Override
            public void onError(Throwable t) {
                System.out.println("ERROR! Notifying quitting: Taxi " + otherTaxiServer + " did not answer.");
                synchronized (TaxiInputThread.taxiToNotify) {
                    TaxiInputThread.taxiToNotify.remove(otherTaxiServer);
                    if (TaxiInputThread.taxiToNotify.isEmpty())
                        TaxiInputThread.taxiToNotify.notify();
                }
                channel.shutdownNow();
            }

            @Override
            public void onCompleted()
            {
                synchronized (TaxiInputThread.taxiToNotify) {
                    TaxiInputThread.taxiToNotify.remove(otherTaxiServer);
                    if (TaxiInputThread.taxiToNotify.isEmpty())
                        TaxiInputThread.taxiToNotify.notify();
                }
                channel.shutdownNow();
            }
        });
    }
}
