package Coordernador;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Coordernador {
    private static final int PORTA_CONTROLE = 999;
    private static final int PORTA_DADOS = 10000;

    private final ExecutorService pool = Executors.newCachedThreadPool();
    private final List<ServidorInfo> servidores = new ArrayList<>();
    private final List<RegistroArquivo> registros = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        Coordernador coord = new Coordernador();
        new Thread(coord::escutarControle).start();
        coord.escutarDados();
    }

    private void escutarControle() {
        try (ServerSocket serverSocket = new ServerSocket(PORTA_CONTROLE)) {
            System.out.println("[Coordenador] Aguardando conexões de Servidores na porta " + PORTA_CONTROLE);
            while (true) {
                Socket socket = serverSocket.accept();
                pool.execute(() -> tratarControle(socket));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void tratarControle(Socket socket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            String linha = in.readLine();
            // Seperando a msg do serv de arquivos em "Comando" e "Porta".
            String[] msg = linha.split(" | ");
            String comando = msg[0];
            int porta = Integer.parseInt(msg[1]);
            String host = socket.getInetAddress().getHostAddress();

            if (comando.equals("CADASTRAR_SERVIDOR_DE_ARQUIVOS")) {
                servidores.add(new ServidorInfo(host, porta));
                System.out.println("[Coordenador] Servidor de Arquivos registrado: " + host + " | " + porta);
            } else if (comando.equals("DESCADASTRAR_SERVIDOR_DE_ARQUIVOS")) {
                servidores.remove(new ServidorInfo(host, porta));
                System.out.println("[Coordenador] Servidor de Arquivos removido: " + host + " | " + porta);
            } else {
                System.out.println("[Coordenador] Comando desconhecido");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void escutarDados() {
        try (ServerSocket serverSocket = new ServerSocket(PORTA_DADOS)) {
            System.out.println("[Coordenador] Aguardando conexões de Clientes na porta " + PORTA_DADOS);
            while (true) {
                Socket socket = serverSocket.accept();
                pool.execute(() -> tratarCliente(socket));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void tratarCliente(Socket socket) {
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()))) {
            String comando = in.readUTF();

            switch (comando) {
                case "TRANSMITIR_ARQUIVOS":
                    break;
                case "LISTAR_ARQUIVOS":
                    break;
                case "BAIXAR_ARQUIVOS":
                    break;
                default:
                    out.writeUTF("ERRO: Comando inválido");
                    out.flush();
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    
}
