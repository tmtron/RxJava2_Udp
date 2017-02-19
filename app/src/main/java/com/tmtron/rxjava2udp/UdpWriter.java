package com.tmtron.rxjava2udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;

public class UdpWriter {

    private final int portNo;
    private DatagramSocket udpSocket;

    public UdpWriter(int portNo) {
        this.portNo = portNo;
    }

    public void sendBroadcast(String data) throws IOException {
        if (udpSocket == null) {
            udpSocket = new DatagramSocket();
            udpSocket.setBroadcast(true);
        }

        byte[] dataBytes = data.getBytes();
        InetAddress udpSenderAddress = Inet4Address.getByName("127.0.0.1");
        DatagramPacket datagramPacket = new DatagramPacket(dataBytes, dataBytes.length, udpSenderAddress, portNo);
        udpSocket.send(datagramPacket);
    }

}