import java.io.*;
import java.net.*;

public class Slave implements Runnable {
	private int horario;
	private String ip;
	private int porta;

	public Slave(int horario, String ip, int porta) {
		this.horario = horario;
		this.ip = ip;
		this.porta = porta;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public int getPorta() {
		return porta;
	}

	public void setPorta(int porta) {
		this.porta = porta;
	}

	public void escutarPorta() {
		int multicastPort = 8000;
		try {
			MulticastSocket socket = new MulticastSocket(multicastPort);
			InetAddress group = InetAddress.getByName("228.5.6.7");
			socket.joinGroup(group);

			byte[] buffer = new byte[1024];
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

			System.out.println("Esperando pacotes");
			socket.receive(packet);
			String message = new String(packet.getData(), 0, packet.getLength());

			if (message.split(":")[0].equals("HORARIO_MASTER")) {
				System.out.println("Mensagem recebida do master: " + message);

				int dif = calcularDiferenca(Integer.parseInt(message.split(":")[1]));
				String sDif = "DIF:" + dif + ":" + getIp() + ":" + getPorta();

				DatagramPacket resp = new DatagramPacket(sDif.getBytes(), sDif.length(), group, 8000);
				socket.send(resp);
			}

			socket.leaveGroup(group);
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		System.out.println("Rodando thread Slave");
		escutarPorta();
	}

	private int calcularDiferenca(int horarioMaster) {
		return horario - horarioMaster;
	}
}
