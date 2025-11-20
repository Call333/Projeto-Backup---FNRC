package ServidorDeArquivo;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class ServidorDeArquivo {
    private static int porta_dados = 8000;
    private static int porta_controle = 999;
    private static String ipCoordenador;
    private static boolean estaAtivo = true;
    private static boolean cadastrado = false;

    public static void main(String[] args) {
        
        Scanner sc = new Scanner(System.in);
        System.out.print("Endereço IP do Coordenador: ");
        ipCoordenador = sc.nextLine();

        cadastrarNoCoordenador(ipCoordenador);

        new Thread(() -> escutarConsole(ipCoordenador)).start();

        // Criação do diretório de arquivos
        File pasta = new File("repo");
        if (!pasta.exists()) {
            pasta.mkdirs();
        }
        // Servidor principal
        try (ServerSocket serverSocket = new ServerSocket(porta_dados)) {
            System.out.println("[Servidor] Aguardando dados de um Coordenador na porta " + porta_dados);

            while (estaAtivo) {
                Socket coordenador = serverSocket.accept();
                tratarDados(coordenador);
            }
            
            System.out.println("[Servidor] Encerrando servidor de dados.");
        } catch (Exception e) {
            e.printStackTrace();
        }
        sc.close();
    }  

    private static void cadastrarNoCoordenador(String ipCoordenador) {
        // Envia solicitação de cadastro para o Coordenador
        try (Socket socket = new Socket(ipCoordenador, porta_controle);
                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {
            
            out.write("CADASTRAR_SERVIDOR_DE_ARQUIVOS:" + porta_dados);
            out.flush();
            
            cadastrado = true;
            System.out.println("[Servidor] Cadastro no Coordenador.");

        } catch (IOException e) {
            System.out.println("[Servidor] Não foi possível cadastrar no Coordenador: " + e.getMessage());
        }
    }

    private static void descadastrarNoCoordenador(String ipCoordenador) {
        if(!cadastrado) {
            System.out.println("O Servidor já está cadastrado.");
            return;
        }
        // Envia solicitação de descadastro para o Coordenador
        try (Socket socket = new Socket(ipCoordenador, porta_controle);
                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {
            
            out.write("DESCADASTRAR_SERVIDOR_DE_ARQUIVOS:" + porta_dados);
            out.flush();

            cadastrado = false;
            System.out.println("[Servidor] Descadastro do Coordenador.");

        } catch (IOException e) {
            System.out.println("[Servidor] Falha ao descadastrar: " + e.getMessage());
        }
    }

    private static void escutarConsole(String ipCoordenador){
        Scanner sc = new Scanner(System.in);

        while(true) {

            String cmd = sc.nextLine().trim().toLowerCase();

            if(cmd.equals("sair")  || cmd.equals("desconectar")) {
                System.out.println("[Servidor] Solicitando DESCONEXÃO.");
                descadastrarNoCoordenador(ipCoordenador);
                continue;
            }

            if(cmd.equals("conectar")) {
                System.out.println("[Servidor] Solicitando CONEXÃO.");
                cadastrarNoCoordenador(ipCoordenador);
                continue;
            }

            if(cmd.equals("shutdown")) {
                descadastrarNoCoordenador(cmd);
                estaAtivo = false;
                break;
            }

            System.out.println("[Servidor] Comando desconhecido");
        }
        sc.close();
    }

    private static void tratarDados(Socket socket) {
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()))) {

            String comando = in.readUTF();

            switch (comando) {
                case "SALVAR_ARQUIVOS":
                    salvarArquivos(in, out);
                    break;
                case "RECUPERAR_ARQUIVOS":
                    recuperaArquivos(in, out);
                default:
                    out.writeUTF("ERRO: Comando invalido.");
                    out.flush();
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void salvarArquivos(DataInputStream in, DataOutputStream out) throws IOException {
        String nome = in.readUTF();
        long tamanho = in.readLong();
        File arquivo = new File("repo/", nome);

        try (FileOutputStream fos = new FileOutputStream(arquivo)) {
            byte[] buffer = new byte[4096];
            long recebido = 0;
            while (recebido < tamanho) {
                int toRead = (int) Math.min(buffer.length, tamanho - recebido);
                int lido = in.read(buffer, 0, toRead);
                if (lido == -1) {
                    throw new EOFException("EOF inesperado ao salvar arquivo");
                }
                fos.write(buffer, 0, lido);
                recebido += lido;
            }
            fos.flush();
        }

        out.writeUTF("OK");
        out.flush();
        System.out.println("[Servidor] Arquivo recebido: " + nome);
    }

    private static void recuperaArquivos(DataInputStream in, DataOutputStream out) throws IOException {
        String nome = in.readUTF();
        File arquivo = new File("repo/", nome);
        if (!arquivo.exists()) {
            out.writeUTF("ERRO: Arquivo não encontrado.");
            out.flush();
            return;
        }

        out.writeUTF("OK");
        out.writeLong(arquivo.length());
        out.flush();

        try (FileInputStream fis = new FileInputStream(arquivo)) {
            byte[] buffer = new byte[4096];
            int lido;
            while ((lido = fis.read(buffer)) != -1) {
                out.write(buffer, 0, lido);
            }
            out.flush();
        }
        /*
         * if(!arquivo.delete()) {
         * System.out.
         * println("[Servidor] Aviso: não foi possível apagar arquivo local: " +
         * arquivo.getAbsolutePath());
         * } else{
         * System.out.println("[Servidor] Arquivo enviado e apagado localmente: " +
         * nome);
         * }
         */

        System.out.println("[Servidor] Arquivo enviado: " + nome);
    }
}
