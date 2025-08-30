import java.io.*;
import java.net.*;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

public class Slave implements Runnable {
	private int horario;
	private final String ip;
	private final int porta;
    private static final int MULTICAST_PORT = 8000;
    private static final String MULTICAST_GROUP = "228.5.6.7";
    private static final int TIMEOUT = 15000;

	public Slave(int horario, String ip, int porta) {
		this.horario = horario;
		this.ip = ip;
		this.porta = porta;
	}

	public String getIp() {
		return ip;
	}

	public int getPorta() {
		return porta;
	}

    @Override
    public void run() {
        System.out.println("Rodando Slave em " + porta + ", clock: " + horario);
        escutarMaster();
    }

	public void escutarMaster() {
        MulticastSocket multicastSocket = null;
        DatagramSocket unicastSocket = null;
        try {
            multicastSocket = criarMulticastSocket();
            InetAddress group = InetAddress.getByName(MULTICAST_GROUP);
            multicastSocket.joinGroup(group);

            unicastSocket = new DatagramSocket(porta);

            DatagramSocket finalUnicastSocket = unicastSocket;
            new Thread(() -> escutarUnicast(finalUnicastSocket)).start();

            while (true) {
                processarMensagensMulticast(multicastSocket, group);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (multicastSocket != null) {
                try {
                    multicastSocket.leaveGroup(InetAddress.getByName(MULTICAST_GROUP));
                    multicastSocket.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (unicastSocket != null) {
                unicastSocket.close();
            }
        }
	}

    private MulticastSocket criarMulticastSocket() throws Exception {
        MulticastSocket socket = new MulticastSocket(MULTICAST_PORT);
        socket.setSoTimeout(TIMEOUT);
        return socket;
    }

    private void processarMensagensMulticast(MulticastSocket socket, InetAddress group) throws Exception {
        try {
            DatagramPacket packet = NetworkUtils.receivePacket(socket);
            String message = new String(packet.getData(), 0, packet.getLength());
            if (message.startsWith("HORARIO_MASTER")) {
                processarHorarioMaster(socket, group, message);
            }
        } catch (SocketTimeoutException e) {
            System.out.println("Slave on port " + porta + ": No multicast message received");
        } catch (Exception e) {
            System.out.println("Slave on port " + porta + " error processing multicast message: " + e.getMessage());
        }
    }

    private void escutarUnicast(DatagramSocket socket) {
        while (true) {
            try {
                DatagramPacket packet = NetworkUtils.receivePacket(socket);
                String message = new String(packet.getData(), 0, packet.getLength());
                if (message.startsWith("AJUSTE")) {
                    processarAjuste(message);
                }
            } catch (SocketTimeoutException e) {
                System.out.println("Slave on port " + porta + ": No unicast message received");
            } catch (Exception e) {
                System.out.println("Slave on port " + porta + " error processing unicast message: " + e.getMessage());
            }
        }
    }

    private void processarHorarioMaster(MulticastSocket socket, InetAddress group, String message) throws Exception {
        int masterTime = Integer.parseInt(message.split(":")[1]);
        System.out.println("Slave na porta " + porta + " recebeu horário do Master: " + masterTime);
        int diff = calcularDiferenca(masterTime);
        String sDiff = "DIF:" + diff + ":" + getIp() + ":" + getPorta();
        NetworkUtils.sendMulticastPacket(socket, sDiff.getBytes(), group, MULTICAST_PORT);
    }

    private void processarAjuste(String message) {
        int adjustment = Integer.parseInt(message.split(":")[1]);
        int oldTime = horario;
        horario += adjustment;
        System.out.println("Slave na porta " + porta + " ajustou horário em " + adjustment +
                ". Horário antigo: " + oldTime + ", Novo horário: " + horario);
    }

	private int calcularDiferenca(int horarioMaster) {
		return horario - horarioMaster;
	}
}
