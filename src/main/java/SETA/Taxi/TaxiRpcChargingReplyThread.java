package SETA.Taxi;

import Utils.GridHelper;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import project.taxi.grpc.TaxiGrpc;
import project.taxi.grpc.TaxiOuterClass;

public class TaxiRpcChargingReplyThread extends Thread
{
    TaxiData myData;
    int otherTaxiId;
    int otherTaxiPort;
    final ManagedChannel channel;

    public TaxiRpcChargingReplyThread(TaxiData myData, int otherTaxiId, int otherTaxiPort)
    {
        this.myData = myData;
        this.otherTaxiId = otherTaxiId;
        this.otherTaxiPort = otherTaxiPort;
        channel = ManagedChannelBuilder.forTarget("localhost:" + otherTaxiPort).usePlaintext().build();
    }

    @Override
    public void run()
    {
        TaxiGrpc.TaxiStub stub = TaxiGrpc.newStub(channel);

        TaxiOuterClass.ChargingReply reply = TaxiOuterClass.ChargingReply.newBuilder()
                .setTaxiId(myData.ID)
                .build();

        stub.replyCharging(reply, new StreamObserver<TaxiOuterClass.Null>()
        {
            @Override
            public void onNext(TaxiOuterClass.Null value) { }

            @Override
            public void onError(Throwable t) {
                System.out.println(t.getCause());
                t.printStackTrace();
                channel.shutdownNow();
            }

            @Override
            public void onCompleted()
            {
                channel.shutdownNow();
            }
        });
    }
}
