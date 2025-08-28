import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.io.*;
import java.net.*;

public class Master {
	private int horario;
	private int numeroSlaves;
	private Map<InetSocketAddress, Integer> listaDifSlaves;
	
	public Master(int horario, int numeroSlaves) {
		this.horario = horario;
		this.numeroSlaves = numeroSlaves;
		listaDifSlaves = new ConcurrentHashMap<>();
	}
	
	public void enviarRequisicoes() {
		String msg = "HORARIO_MASTER:" + horario;
		 try {
			 InetAddress group = InetAddress.getByName("228.5.6.7");
			 MulticastSocket s = new MulticastSocket(8000);
			s.joinGroup(group);
			
			DatagramPacket hi = new DatagramPacket(msg.getBytes(), msg.length(),
                    group, 8000);
			s.send(hi);
			
			byte[] buf = new byte[1000];
			DatagramPacket recv = new DatagramPacket(buf, buf.length);
			
			int tmp = 0;
			
			while (tmp != numeroSlaves + 1) {
				buf = new byte[1000];
				recv = new DatagramPacket(buf, buf.length);
				
				s.receive(recv);
				String message = new String(recv.getData(), 0, recv.getLength());
				
				if (!message.split(":")[0].equals("HORARIO_MASTER")) {
					System.out.println("Mensagem recebida dos slaves: " + message);
					
					
				}
			
				tmp++;
			}
			
			s.leaveGroup(group);
			s.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		 
	}
}
