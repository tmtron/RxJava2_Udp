package com.tmtron.rxjava2udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;

import io.reactivex.Completable;
import io.reactivex.functions.Action;
import io.reactivex.schedulers.Schedulers;

public class UdpWriter {

    private final int portNo;
    private DatagramSocket udpSocket;

    public UdpWriter(int portNo) {
        this.portNo = portNo;
    }

    public void sendBroadcast(final String data) throws IOException {
        if (udpSocket == null) {
            udpSocket = new DatagramSocket();
            udpSocket.setBroadcast(true);
        }

        Completable.fromAction(new Action() {
            @Override
            public void run() throws Exception {
                byte[] dataBytes = data.getBytes();
                InetAddress udpSenderAddress = Inet4Address.getByName("127.0.0.1");
                DatagramPacket datagramPacket = new DatagramPacket(dataBytes, dataBytes.length, udpSenderAddress, portNo);
                udpSocket.send(datagramPacket);
            }
        }).subscribeOn(Schedulers.io()).subscribe();
    }

}