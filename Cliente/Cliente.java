package Cliente;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.InputMismatchException;
import java.util.Scanner;

public class Cliente {
    private static String apelido;
    private static String pastaDownload;
    private static String ipCoordenador;
    private static int portaCoordenador = 10000;
    private static Scanner sc;

    public static void main(String[] args) {
        while (true) {
            sc = new Scanner(System.in);
            System.out.println("\n-- Cliente Backup --");
            System.out.println("1. Transmitir Arquivos");
            System.out.println("2. Listar arquivos disponíveis por apelido");
            System.out.println("3. Baixar arquivos");
            System.out.println("4. Configurações");
            System.out.println("5. Exibir configuracoes");
            System.out.println("6. Sair");
            try {
                int opcao = sc.nextInt();
                sc.nextLine();

                switch (opcao) {
                    case 1:
                        transmitir_arquivos(sc);
                        break;
                    case 2:
                        listar_arquivos_usuario(sc);
                        break;
                    case 3:
                        baixar_arquivos(sc);
                        break;
                    case 4:
                        configuracoes(sc);
                        break;
                    case 5:
                        exibirConfiguracoes();
                        break;
                    case 6:
                        System.out.println("Saindo...");
                        break;
                    default:
                        System.out.println("Opção invalida!");
                        break;
                }
            } catch (InputMismatchException e) {
                System.out.println("Use apenas números inteiros.");
            } catch (Exception e) {
                System.out.println("Servidor indisponível");
            }
        }

    }

    private static void exibirConfiguracoes() {
        System.out.println("\n---- Configurações ----");
        System.out.println("Apelido: " + apelido);
        System.out.println("Pasta de download: " + pastaDownload);
        System.out.println("IP do Coordenador: " + ipCoordenador);
    }

    private static void configuracoes(Scanner sc) {
        System.out.println("\nConfigurações:");
        System.out.print("\n(a) Configurar apelido: ");
        apelido = sc.nextLine();
        System.out.print("\n(b) Configurar diretório de download: ");
        pastaDownload = sc.nextLine();
        File pasta = new File(pastaDownload);
        if (!pasta.exists()) {
            boolean criada = pasta.mkdirs();
            if (!criada) {
                System.out.println("Erro: Não foi possível criar diretório " + pasta.getAbsolutePath());
                return; // ← IMPORTANTE: evita quebrar o fluxo
            }
        }
        System.out.print("\n(c) Configurar endereço IP do Coordenador: ");
        ipCoordenador = sc.nextLine();
    }

    private static void transmitir_arquivos(Scanner sc) throws IOException{

        System.out.println("Arquivo para enviar: ");
        String localArquivo = sc.nextLine();

        File arquivo = new File(localArquivo);
        if (!arquivo.exists()) {
            System.out.println("arquivo não encontrado.");
            return;
        }

        try (Socket socket = new Socket(ipCoordenador, portaCoordenador);
                DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                FileInputStream fis = new FileInputStream(arquivo)) {

            out.writeUTF("TRANSMITIR_ARQUIVOS");
            out.writeUTF(apelido);
            out.writeUTF(arquivo.getName());
            out.writeLong(arquivo.length());
            out.flush();

            byte[] buffer = new byte[4096];
            int bytesLidos;
            while ((bytesLidos = fis.read(buffer)) != -1) {
                out.write(buffer, 0, bytesLidos);
            }
            out.flush();

            String resposta = in.readUTF();
            if (resposta.equals("TRANSMITIDO_OK")) {
                int id = in.readInt();
                System.out.println("Arquivo enviado com sucesso! ID: " + id);
            } else {
                System.out.println("Erro no envio do arquivo.");
            }
        } catch (NullPointerException e) {
            System.out.println("Você não definiu um apelido em Configuracoes no menu principal.");
        } catch (SocketException e) {
            System.out.println("Nenhum servidor disponível");
        }
    }

    private static void listar_arquivos_usuario(Scanner sc) throws IOException {
        try (Socket socket = new Socket(ipCoordenador, portaCoordenador);
                DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()))) {

            out.writeUTF("LISTAR_ARQUIVOS");
            out.writeUTF(apelido);
            out.flush();

            int total = in.readInt();
            System.out.println("-- Arquivos registrados --");
            for (int i = 0; i < total; i++) {
                int id = in.readInt();
                String nome = in.readUTF();
                String servidor = in.readUTF();
                System.out.printf("ID: %d | NOME: %s | SERVIDOR: %s%n", id, nome, servidor);
            }
        } catch (NullPointerException e) {
            System.out.println("Você não definiu um apelido em Configuracoes no menu principal.");
        }
    }

    private static void baixar_arquivos(Scanner sc) throws IOException {
        try {
            // entrada do ID do arquivo:
            System.out.println("Identificador do arquivo: ");
            Integer id = sc.nextInt();
            // Envia solicitacao para baixar arquivos e 
            try (Socket socket = new Socket(ipCoordenador, portaCoordenador);
                    DataInputStream in = new DataInputStream(socket.getInputStream());
                    DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {

                out.writeUTF("BAIXAR_ARQUIVOS");
                out.writeInt(id);
                out.writeUTF(apelido);
                out.flush();

                String resposta = in.readUTF();
                if (!"OK".equals(resposta)) {
                    System.out.println(resposta);
                    return;
                }
                // Nome arquivo salvo no servidor.
                String nomeOriginal = in.readUTF();
                long tamanho = in.readLong();
                File destino = new File(pastaDownload, nomeOriginal);

                try (FileOutputStream fos = new FileOutputStream(destino)) {
                    byte[] buffer = new byte[4096];
                    long recebido = 0;
                    while (recebido < tamanho) {
                        int toRead = (int) Math.min(buffer.length, tamanho - recebido);
                        int lido = in.read(buffer, 0, toRead);
                        if (lido == -1) {
                            throw new EOFException("EOF inesperado durante download");
                        }
                        fos.write(buffer, 0, lido);
                        recebido += lido;
                    }
                    fos.flush();
                }
                System.out.println("Download concluído: " + destino.getAbsolutePath());

            }
        } catch (InputMismatchException e) {
            System.out.println("Você digitou caractere inválido no campo ID.");
        }  catch (SocketException e) {
            System.out.println("Conexão recusada: O coordenador está off-line.");
        }

    }
}
