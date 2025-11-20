package Coordernador;

import java.util.Objects;

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
    public boolean equals(Object obj) {
        if(this == obj) {
            return true;
        }
        if(obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ServidorInfo x = (ServidorInfo) obj;
        return porta == x.porta && Objects.equals(ipServidor, x.ipServidor);
    }
    @Override
    public String toString() {
        return ipServidor + ":" + porta;
    }
}
