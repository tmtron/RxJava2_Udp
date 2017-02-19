package com.tmtron.rxjava2udp;

import android.support.annotation.NonNull;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.functions.Cancellable;
import io.reactivex.plugins.RxJavaPlugins;

class UdpObservable {

    private static class UdpThread extends Thread {
        private final int portNo;
        private final int bufferSizeInBytes;
        private final ObservableEmitter<DatagramPacket> emitter;
        private DatagramSocket udpSocket;

        private UdpThread(@NonNull ObservableEmitter<DatagramPacket> emitter
                , int portNo, int bufferSizeInBytes) {
            this.emitter = emitter;
            this.portNo = portNo;
            this.bufferSizeInBytes = bufferSizeInBytes;
        }

        @Override
        public void run() {
            try {
                udpSocket = new DatagramSocket(portNo);
                try {
                    while (!isInterrupted() && !emitter.isDisposed()) {
                        byte[] rcvBuffer = new byte[bufferSizeInBytes];
                        DatagramPacket datagramPacket = new DatagramPacket(rcvBuffer, rcvBuffer.length);
                        udpSocket.receive(datagramPacket);
                        if (!isInterrupted() && !emitter.isDisposed()) {
                            emitter.onNext(datagramPacket);
                        }
                    }
                } finally {
                    closeUdpSocket();
                }
            } catch (Throwable th) {
                if (!isInterrupted()) {
                    if (!emitter.isDisposed()) {
                        emitter.onError(th);
                    } else {
                        RxJavaPlugins.onError(th);
                    }
                }
            }
        }

        private void closeUdpSocket() {
            if (!udpSocket.isClosed()) {
                udpSocket.close();
            }
        }

        @Override
        public void interrupt() {
            super.interrupt();
            closeUdpSocket();
        }
    }

    /**
     * creates an Observable that will emit all UDP datagrams of a UDP port.
     * <p>
     * This will be an infinite stream that ends when the observer unsubscribes, or when an error
     * occurs. The observer does not handle backpressure.
     * </p>
     */
    public static Observable<DatagramPacket> create(final int portNo, final int bufferSizeInBytes) {
        return Observable.create(
                new ObservableOnSubscribe<DatagramPacket>() {
                    @Override
                    public void subscribe(ObservableEmitter<DatagramPacket> emitter) throws Exception {
                        final UdpThread udpThread = new UdpThread(emitter, portNo, bufferSizeInBytes);
                        emitter.setCancellable(new Cancellable() {
                            @Override
                            public void cancel() throws Exception {
                                udpThread.interrupt();
                            }
                        });
                        udpThread.start();
                    }
                }
        );
    }

}
