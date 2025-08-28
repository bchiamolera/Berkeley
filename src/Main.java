
public class Main {

	public static void main(String[] args) {
		Master m = new Master(300, 2);
		Slave s1 = new Slave(250, "localhost", 8001);
		Slave s2 = new Slave(325, "localhost", 8002);
		
		(new Thread(s1)).start();
		(new Thread(s2)).start();
		
		m.enviarRequisicoes();
	}

}
