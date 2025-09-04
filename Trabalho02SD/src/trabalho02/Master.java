package trabalho02;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.net.*;

public class Master {
    private static final int MULTICAST_PORT = 8000;
    private static final String MULTICAST_GROUP = "228.5.6.7";
    private static final int TIMEOUT = 5000;
    private static final int SYNC_INTERVAL = 10000;

	private int horario;
	private int numeroSlaves;
	private Map<InetSocketAddress, Integer> listaDifSlaves;
	
	public Master(int horario, int numeroSlaves) {
		this.horario = horario;
		this.numeroSlaves = numeroSlaves;
		listaDifSlaves = new ConcurrentHashMap<>();
	}

    public void enviarRequest() {
        try {
        	// Criando comunicação em grupo
            MulticastSocket socket = criarMulticastSocket();
            InetAddress group = InetAddress.getByName(MULTICAST_GROUP);
            socket.joinGroup(group);
            System.out.println("Master iniciado, clock: " + horario);

            // A cada 10s o master repete e processo e envia o horário para os slaves, recebendo as diferenças
            while (true) {
                enviarHorario(socket, group);
                receberDiferencaHorario(socket);
                Thread.sleep(SYNC_INTERVAL);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private MulticastSocket criarMulticastSocket() throws Exception {
        MulticastSocket socket = new MulticastSocket(MULTICAST_PORT);
        socket.setSoTimeout(TIMEOUT);
        return socket;
    }

    // O master envia o próprio horário para todos os slaves
    private void enviarHorario(MulticastSocket socket, InetAddress group) throws Exception {
        String msg = "HORARIO_MASTER:" + horario;
        NetworkUtils.sendMulticastPacket(socket, msg.getBytes(), group, MULTICAST_PORT);
        System.out.println("Master enviou horário: " + horario);
    }

    private void receberDiferencaHorario(MulticastSocket socket) throws Exception {
        listaDifSlaves.clear();
        int recebidos = 0;

        while (recebidos < numeroSlaves) {
            try {
            	// Master recebe os pacotes dos slaves
                DatagramPacket packet = NetworkUtils.receivePacket(socket);
                String message = new String(packet.getData(), 0, packet.getLength());
                
                // Master também recebe a própria resposta, então se a mensagem recebida for dele mesmo, ela é ignorada
                // Ou seja, só processa as mensagens recebidas pelos slaves
                if (!message.startsWith("HORARIO_MASTER")) {
                    processarDiferencasSlave(packet, message);
                    recebidos++;
                }
            } catch (SocketTimeoutException e) {
                if (listaDifSlaves.size() >= numeroSlaves) {
                    break;
                }
            }
        }

        // Se a lista não estiver vazia, o master calcula a média dos horários
        if (!listaDifSlaves.isEmpty()) {
            sincronizarHorarios(socket);
        }
    }

    private void processarDiferencasSlave(DatagramPacket packet, String message) {
    	// A mensagem recebida é dividida em partes 
        String[] parts = message.split(":");
        if (parts.length == 4 && parts[0].equals("DIF")) {

            int diff = Integer.parseInt(parts[1]);
            String ip = parts[2];
            int port = Integer.parseInt(parts[3]);
            
            // O endereço e a diferença de horário recebida dos slaves é armazenada na listaDifSlaves 
            InetSocketAddress address = new InetSocketAddress(ip, port);
            listaDifSlaves.put(address, diff);
            System.out.println("Recebeu diferença " + diff + " do Slave em " + address);
        }
    }

    private void sincronizarHorarios(MulticastSocket socket) throws Exception {
    	// O Master adiciona sua própria diferença na listaDifSlaves para calcular a média
        listaDifSlaves.put(new InetSocketAddress("localhost", MULTICAST_PORT), 0);
        System.out.println("Incluída diferença do Master: 0");

        int avgDiff = calcularDiferencaMedia(listaDifSlaves);
        
        // O Master calcula e aplica o ajuste no prórpio horário
        aplicarAjustesMaster(avgDiff);
        
        // O Master envia os ajustes para cada Slave individualmente
        enviarAjustes(socket, avgDiff);
    }

    private void aplicarAjustesMaster(int adjustment) {
        int oldTime = horario;
        horario += adjustment;
        System.out.println("Master ajustou horário em " + adjustment + ". Horário antigo: " + oldTime + ", Novo horário: " + horario);
    }

    // Para cada slave, o Master calcula o ajuste e envia a mensagem com a informação
    private void enviarAjustes(MulticastSocket socket, int avgDiff) throws Exception {
        for (Map.Entry<InetSocketAddress, Integer> entry : listaDifSlaves.entrySet()) {
            InetSocketAddress address = entry.getKey();
            if (address.getPort() == MULTICAST_PORT) { continue; } // Se o endereço do item for o mesmo do Master, isso quer dizer que é o próprio Master, logo, ignora (já processado)
            int clientDiff = entry.getValue();
            int adjustment = calcularAjuste(clientDiff, avgDiff);
            String msg = "AJUSTE:" + adjustment;
            NetworkUtils.sendPacket(socket, msg.getBytes(), address);
            System.out.println("Enviou ajuste " + adjustment + " para o Slave em " + address);
        }
    }

    public static int calcularDiferencaMedia(Map<InetSocketAddress, Integer> clientDiffs) {
        long sumDiff = 0;
        int count = 0;
        for (Integer diff : clientDiffs.values()) {
            sumDiff += diff;
            count++;
        }
        return (int) (sumDiff / count);
    }

    public static int calcularAjuste(int clientDiff, int avgDiff) {
        return -(clientDiff - avgDiff);
    }
}
