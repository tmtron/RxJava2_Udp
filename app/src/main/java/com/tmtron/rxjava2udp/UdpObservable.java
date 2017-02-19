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
                // we don't want to create the DatagramSocket in the constructor, because this
                // might raise an Exception that the observer wants to handle
                udpSocket = new DatagramSocket(portNo);
                try {
                    /* QUESTION 1:
                       Do I really need to check isInterrupted() and emitter.isDisposed()?

                       When the thread is interrupted an interrupted exception will
                       be raised anyway and the emitter is being disposed (this is what
                       caused the interruption)
                    */
                    while (!isInterrupted() && !emitter.isDisposed()) {
                        byte[] rcvBuffer = new byte[bufferSizeInBytes];
                        DatagramPacket datagramPacket = new DatagramPacket(rcvBuffer, rcvBuffer.length);
                        udpSocket.receive(datagramPacket);
                        // QUESTION 1a: same as QUESTION 1 above
                        if (!isInterrupted() && !emitter.isDisposed()) {
                            emitter.onNext(datagramPacket);
                        }
                    }
                } finally {
                    closeUdpSocket();
                }
            } catch (Throwable th) {
                // the thread will only be interrupted when the observer has unsubscribed:
                // so we need not report it
                if (!isInterrupted()) {
                    if (!emitter.isDisposed()) {
                        emitter.onError(th);
                    } else {
                        // QUESTION 2: is this the correct way to handle errors, when the emitter
                        //             is already disposed?
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
            // QUESTION 3: this is called from an external thread, right, so
            //             how can we correctly synchronize the access to udpSocket?
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
                        /* QUESTION 4: Is this the right way to handle unsubscription?
                         */
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
