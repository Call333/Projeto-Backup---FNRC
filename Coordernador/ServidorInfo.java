package Coordernador;

public class ServidorInfo {
    private String ipServidor;
    private int porta;

    public ServidorInfo(String ipServidor, int porta) {
        this.ipServidor = ipServidor;
        this.porta = porta;
    }

    public String getIpServidor() {
        return ipServidor;
    }

    public int getPorta() {
        return porta;
    }

    @Override
    public String toString() {
        return ipServidor + ":" + porta;
    }
}
