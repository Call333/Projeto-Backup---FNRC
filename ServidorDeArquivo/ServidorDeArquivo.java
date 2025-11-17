package ServidorDeArquivo;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
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

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.print("Endereço IP do Coordenador: ");
        ipCoordenador = sc.nextLine();

        // Envia solicitação de registro para o Coordenador
        try (Socket socket = new Socket(ipCoordenador, porta_controle);
                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {
            out.write("CADASTRAR_SERVIDOR_DE_ARQUIVOS | " + porta_controle);
            out.flush();

        } catch (Exception e) {
            e.printStackTrace();
        }

        // Criação do diretório de arquivos
        File pasta = new File("repo");
        if (!pasta.exists()) {
            pasta.mkdirs();
        }

        // Servidor principal
        try (ServerSocket serverSocket = new ServerSocket(porta_dados)) {
            System.out.println("[Servidor] Aguardando conexões na porta " + porta_dados);
            while (true) {
                Socket socket = serverSocket.accept();
                tratarDados(socket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        File arquivo = new File("repo/" + nome);
        try (FileOutputStream fos = new FileOutputStream(arquivo)) {
            byte[] buffer = new byte[4096];
            long recebido = 0;
            while(recebido < tamanho) {
                int lido = in.read(buffer, 0, (int) Math.min(buffer.length, tamanho - recebido));
                if(lido == -1) {
                    break;
                }
                fos.write(buffer, 0 ,lido);
                recebido += lido;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        out.flush();
        System.out.println("[Servidor] Arquivo recebido: " + nome);
    }

    private static void recuperaArquivos(DataInputStream in, DataOutputStream out) throws IOException{
        String nome = in.readUTF();
        File arquivo = new File("repo/" + nome);
        if(!arquivo.exists()) {
            out.writeUTF("ERRO: Arquivo não encontrado.");
            out.flush();
            return;
        }

        out.writeUTF("OK");
        out.writeLong(arquivo.length());
        try (FileInputStream fis = new FileInputStream(arquivo)) {
            byte[] buffer = new byte[4096];
            int lido;
            while ((lido = fis.read(buffer)) != -1) {
                out.write(buffer, 0, lido);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        out.flush();
        System.out.println("[Servidor] Arquivo enviado: " + nome);
    }
}
