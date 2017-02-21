package com.tmtron.rxjava2udp;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.functions.Cancellable;
import io.reactivex.schedulers.Schedulers;

class UdpObservable {

    private static Cancellable getCancellable(final DatagramSocket udpSocket) {
        return new Cancellable() {
            @Override
            public void cancel() throws Exception {
                if (!udpSocket.isClosed()) {
                    udpSocket.close();
                }
            }
        };
    }

    /**
     * creates an Observable that will emit all UDP datagrams of a UDP port.
     * <p>
     * This will be an infinite stream that ends when the observer unsubscribes, or when an error
     * occurs.
     * </p>
     */
    public static Observable<DatagramPacket> create(final int portNo, final int bufferSizeInBytes) {
        return Observable.create(
                new ObservableOnSubscribe<DatagramPacket>() {
                    @Override
                    public void subscribe(ObservableEmitter<DatagramPacket> emitter) throws Exception {

                        final DatagramSocket udpSocket = new DatagramSocket(portNo);
                        emitter.setCancellable(getCancellable(udpSocket));
                        //noinspection InfiniteLoopStatement
                        while (true) {
                            try {
                                byte[] rcvBuffer = new byte[bufferSizeInBytes];
                                DatagramPacket datagramPacket = new DatagramPacket(rcvBuffer, rcvBuffer.length);
                                udpSocket.receive(datagramPacket);
                                emitter.onNext(datagramPacket);
                            } catch (Exception e) {
                                emitter.onError(e);
                            }
                        }
                    }
                }).subscribeOn(Schedulers.io());
    }
}
