package trabalho02;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;

public class NetworkUtils {
    public static void sendPacket(DatagramSocket socket, byte[] buf, InetSocketAddress address) throws Exception {
        DatagramPacket packet = new DatagramPacket(
                buf, buf.length,
                address.getAddress(),
                address.getPort()
        );
        socket.send(packet);
    }

    public static void sendMulticastPacket(DatagramSocket socket, byte[] buf, InetAddress group, int port) throws Exception {
        DatagramPacket packet = new DatagramPacket(buf, buf.length, group, port);
        socket.send(packet);
    }

    public static DatagramPacket receivePacket(DatagramSocket socket) throws Exception {
        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);
        return packet;
    }
}