package Coordernador;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
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
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            String linha = in.readLine();
            if (linha == null) {
                out.println("ERRO: vazio");
                return;
            }

            // Seperando a msg do serv de arquivos em "Comando" e "Porta".
            String[] msg = linha.split(":");
            if (msg.length < 2) {
                out.println("ERRO: formato_invalido");
                return;
            }
            String comando = msg[0];
            int porta;
            try {
                porta = Integer.parseInt(msg[1]);
            } catch (NumberFormatException nfe) {
                out.println("ERRO: porta_invalida");
                return;
            }
            String host = socket.getInetAddress().getHostAddress();
            ServidorInfo sInfo = new ServidorInfo(host, porta);

            if (comando.equals("CADASTRAR_SERVIDOR_DE_ARQUIVOS")) {
                servidores.add(sInfo);
                System.out.println("[Coordenador] Servidor de Arquivos registrado: " + host + " | " + porta);
                out.println("OK");
            } else if (comando.equals("DESCADASTRAR_SERVIDOR_DE_ARQUIVOS")) {
                boolean removed = servidores.remove(sInfo);
                if (removed) {
                    System.out.println("[Coordenador] Servidor de Arquivos removido: " + host + " | " + porta);
                    out.println("OK");
                } else {
                    System.out.println("[Coordenador] Tentativa de remover servidor não cadastrado: " + host + " | " + porta);
                    out.println("ERRO: nao_cadastrado");
                }
            } else {
                System.out.println("[Coordenador] Comando desconhecido");
                out.println("ERRO: comando_desconhecido");
            }
        } catch (Exception e) {
            System.out.println("Erro inesperado..." + e.getMessage());
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
            System.out.println("ERRO:" + e.getMessage());
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
        System.out.println("[Coordenador] lista de servidores cadastrados: " + servidores);
        try {
            String usuario = clienteIn.readUTF();
            // contar somente registros do usuario
            List<RegistroArquivo> lista = new ArrayList<>();
            for (RegistroArquivo r : registros) {
                if (r.getApelido().contains(usuario)) {
                    lista.add(r);
                }
            }
            System.out.println("[Coordenador] lista de registros do usuário: " + lista.toString());
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
        //Verifica se o servidor onde o arquivo está salvo ainda está cadastrado.
        String[] dados = registro.getServidor().split(":");
        ServidorInfo servidor = null;
        for (ServidorInfo s : servidores) {
            if(s.getIpServidor().equals(dados[0]) && s.getPorta() == Integer.parseInt(dados[1])) {
                servidor = new ServidorInfo(dados[0], Integer.parseInt(dados[1]));
            }
        }

        if(servidor == null) {
            clienteOut.writeUTF("ERRO: Servidor indisponível.");
            clienteOut.flush();
            return;
        }

        try (Socket socket = new Socket(servidor.getIpServidor(), servidor.getPorta());
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

            registros.remove(registro);
            System.out.println("[Coordenador] DOWNLOAD repassado com sucesso e registro removido: ID= " + id);

        } catch (IOException e) {
            System.err.println("[Coordenador] Erro ao recuperar arquivo do servidor: " + e.getMessage());
            clienteOut.writeUTF("ERRO: falha ao recuperar");
            clienteOut.flush();
        }
      }
    
    private ServidorInfo servidorDestino(List<ServidorInfo> servidores){
        if(servidores.isEmpty()) {
            return null;
        }

        ServidorInfo acessoMenosRecente = servidores.get(0);
        for (ServidorInfo servidor : servidores) {
            if (servidor.getUltimoAcesso().isBefore(acessoMenosRecente.getUltimoAcesso())) {
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
