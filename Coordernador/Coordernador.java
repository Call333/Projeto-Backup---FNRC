package Coordernador;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
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
        coord.escutarCliente();
    }

    private void escutarControle() {
        try (ServerSocket serverSocket = new ServerSocket(PORTA_CONTROLE)) {
            System.out.println("[Coordenador] Aguardando conexões de CADASTRO/DESCADASTRO de Servidores na porta "
                    + PORTA_CONTROLE);
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
            String[] msg = linha.split(":");
            String comando = msg[0];
            int porta = Integer.parseInt(msg[1]);
            String host = socket.getInetAddress().getHostAddress();
            ServidorInfo sInfo = new ServidorInfo(host, porta);
            
            if (comando.equals("CADASTRAR_SERVIDOR_DE_ARQUIVOS")) {
                servidores.add(sInfo);
                System.out.println("[Coordenador] Servidor de Arquivos registrado: " + host + " | " + porta);
            } else if (comando.equals("DESCADASTRAR_SERVIDOR_DE_ARQUIVOS")) {
                servidores.remove(sInfo);
                System.out.println("[Coordenador] Servidor de Arquivos removido: " + host + " | " + porta);
            } else {
                System.out.println("[Coordenador] Comando desconhecido");
            }
        } catch (Exception e) {
            System.out.println("Erro inesperado...");
        }
    }

    private void escutarCliente() {
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
                    processarUpload(in, out);
                    break;
                case "LISTAR_ARQUIVOS":
                    processarListagem(in, out);
                    break;
                case "BAIXAR_ARQUIVOS":
                    processarDownload(in, out);
                    break;
                default:
                    out.writeUTF("ERRO: Comando inválido");
                    out.flush();
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processarUpload(DataInputStream clienteIn, DataOutputStream clienteOut) throws IOException {
        if (servidores.isEmpty()) {
            clienteOut.writeUTF("ERRO: Nenhum servidor disponível.");
            clienteOut.flush();
            return;
        }

        ServidorInfo destino = servidorDestino(servidores);
        System.out.println("[Coordenador] Encamilhando UPLOAD para " + destino);
        try {
            String usuario = clienteIn.readUTF();
            String nomeArquivo = clienteIn.readUTF();
            long tamanhoArquivo = clienteIn.readLong();

            try (Socket socket = new Socket(destino.getIpServidor(), destino.getPorta());
                    DataInputStream servidorIn = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                    DataOutputStream servidorOut = new DataOutputStream(
                            new BufferedOutputStream(socket.getOutputStream()))) {

                servidorOut.writeUTF("SALVAR_ARQUIVOS");
                servidorOut.writeUTF(nomeArquivo);
                servidorOut.writeLong(tamanhoArquivo);
                servidorOut.flush();

                byte[] buffer = new byte[4096];
                long enviado = 0;
                while (enviado < tamanhoArquivo) {
                    int toRead = (int) Math.min(buffer.length, tamanhoArquivo - enviado);

                    int lido = clienteIn.read(buffer, 0, toRead);
                    if (lido == -1) {
                        throw new EOFException("EOF inesperado do cliente durante upload");
                    }
                    servidorOut.write(buffer, 0, lido);
                    enviado += lido;
                }
                servidorOut.flush();

                String respostaServidor = servidorIn.readUTF();
                if ("OK".equals(respostaServidor)) {
                    int id = gerarIdUnico();
                    registros.add(new RegistroArquivo(id, nomeArquivo, usuario, destino.toString()));
                    clienteOut.writeUTF("TRANSMITIDO_OK");
                    clienteOut.writeInt(id);
                } else {
                    clienteOut.writeUTF("ERRO");
                }
            clienteOut.flush();
            }
        } catch (IOException e) {
            System.err.println("[Coordenador] Erro ao encaminhar upload: Usuario não digitou o apelido");
            clienteOut.writeUTF("ERRO: Falha ao encaminhar para servidor");
            clienteOut.flush();
        }
    }

    private void processarListagem(DataInputStream clienteIn, DataOutputStream clienteOut) throws IOException {
        System.out.println(servidores);
        try {
            String usuario = clienteIn.readUTF();
            // contar somente registros do usuario
            List<RegistroArquivo> lista = new ArrayList<>();
            for (RegistroArquivo r : registros) {
                if (r.getApelido().contains(usuario)) {
                    lista.add(r);
                }
            }
            System.out.println(lista.toString());
            clienteOut.writeInt(lista.size());
            for (RegistroArquivo r : lista) {
                clienteOut.writeInt(r.getId());
                clienteOut.writeUTF(r.getNome());
                clienteOut.writeUTF(r.getServidor());
            }
            clienteOut.flush();
        } catch (EOFException e) {
            System.out.println("[Coordenador] O Cliente não enviou o apelido: " + e.getMessage());
        }

    }

    private void processarDownload(DataInputStream clienteIn, DataOutputStream clienteOut) throws IOException {
        if (servidores.isEmpty()) {
            clienteOut.writeUTF("ERRO: Nenhum servidor disponível.");
            clienteOut.flush();
            return;
        }
        
        int id = clienteIn.readInt();
        String usuario = clienteIn.readUTF();
        RegistroArquivo registro = null;

        for (RegistroArquivo reg : registros) {
            if (id == reg.getId() && usuario.equals(reg.getApelido())) {
                registro = reg;
            }
        }

        if (registro == null) {
            clienteOut.writeUTF("ERRO: Arquivo não encontrado");
            clienteOut.flush();
            return;
        }

        String[] servidor = registro.getServidor().split(":");
        ServidorInfo s = new ServidorInfo(servidor[0], Integer.parseInt(servidor[1]));

        try (Socket socket = new Socket(s.getIpServidor(), s.getPorta());
                DataInputStream servidorIn = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                DataOutputStream servidorOut = new DataOutputStream(
                        new BufferedOutputStream(socket.getOutputStream()))) {

            servidorOut.writeUTF("RECUPERAR_ARQUIVOS");
            servidorOut.writeUTF(registro.getNome());
            servidorOut.flush();

            String respostaServidor = servidorIn.readUTF();
            if (!"OK".equals(respostaServidor)) {
                clienteOut.writeUTF("ERRO: Arquivo indisponível");
                clienteOut.flush();
                return;
            }

            long tamanho = servidorIn.readLong();
            clienteOut.writeUTF("OK");
            clienteOut.writeUTF(registro.getNome()); // -> passa o nome do arquivo para o cliente
            clienteOut.writeLong(tamanho);
            clienteOut.flush();

            byte[] buffer = new byte[4096];
            long recebido = 0;
            while (recebido < tamanho) {
                int toRead = (int) Math.min(buffer.length, tamanho - recebido);
                int lido = servidorIn.read(buffer, 0, toRead);
                if (lido == -1) {
                    throw new EOFException("EOF inesperado do servidor durante download");
                }
                clienteOut.write(buffer, 0, lido);
                recebido += lido;
            }
            clienteOut.flush();

            // registros.remove(id);
            System.out.println("[Coordenador] DOWNLOAD repassado com sucesso e registro removido: ID= " + id);

        } catch (IOException e) {
            System.err.println("[Coordenador] Erro ao recuperar arquivo do servidor: " + e.getMessage());
            clienteOut.writeUTF("ERRO: falha ao recuperar");
            clienteOut.flush();
        }
      }
    
    private ServidorInfo servidorDestino(List<ServidorInfo> servidores){
        ServidorInfo acessoMenosRecente = servidores.get(0);

        if(servidores.isEmpty()) {
            return null;
        }

        for (ServidorInfo servidor : servidores) { 
            if(servidor.getUltimoAcesso().isBefore(acessoMenosRecente.getUltimoAcesso())) {
                acessoMenosRecente = servidor;
            }
        }
        acessoMenosRecente.atualizarAcesso();
        return acessoMenosRecente;
    }

    private int gerarIdUnico() {
        Integer id;
        while (true) {
            id = new Random().nextInt(1, 10000);
            boolean existe = false;

            for (RegistroArquivo r : registros) {
                if (r.getId() == id) {
                    existe = true;
                    break;
                }
            }

            if (!existe) {
                break;
            }
        }
        return id;
    }
}
