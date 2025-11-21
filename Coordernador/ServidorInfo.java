package Coordernador;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class ServidorInfo {
    private String ipServidor;
    private int porta;
    private LocalDateTime ultimoAcesso;
    private DateTimeFormatter formatador = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public ServidorInfo(String ipServidor, int porta) {
        this.ipServidor = ipServidor;
        this.porta = porta;
        this.ultimoAcesso = LocalDateTime.now();
    }

    public String getIpServidor() {
        return ipServidor;
    }

    public int getPorta() {
        return porta;
    }

    public LocalDateTime getUltimoAcesso() {
        return ultimoAcesso;
    }

    public void atualizarAcesso() {
        ultimoAcesso = LocalDateTime.now();
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
        return ipServidor + ":" + porta + ":" + ultimoAcesso.format(formatador);
    }
}
