package Cliente;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.Socket;
import java.util.Scanner;

public class Cliente {
    private static String apelido;
    private static String pastaDownload;
    private static String ipCoordenador;
    private static int portaCoordenador = 10000;

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        while (true) {
            System.out.println("\n-- Cliente Backup --");
            System.out.println("1. Transmitir Arquivos");
            System.out.println("2. Listar arquivos disponíveis por apelido");
            System.out.println("3. Baixar arquivos");
            System.out.println("4. Configurações");
            System.out.println("5. Sair");
            int opcao = sc.nextInt();
            sc.nextLine();

            try {
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
                        System.out.println("Saindo...");
                        break;
                    default:
                        System.out.println("Opção invalida!");
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    private static void configuracoes(Scanner sc) {
        System.out.println("\nConfigurações:");
        System.out.println("(a) Configurar apelido");
        System.out.println("(b) Configurar diretório de download");
        System.out.println("(c) Configurar endereço IP do Coordenador");
        String opcao = sc.next();

        try {
            switch (opcao) {
                case "a":
                    System.out.println("Apelido: ");
                    apelido = sc.next();
                    break;
                case "b":
                    System.out.println("Diretório: ");
                    pastaDownload = sc.nextLine();
                    break;
                case "c":
                    System.out.println("IP Coordenador: ");
                    ipCoordenador = sc.nextLine();
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void transmitir_arquivos(Scanner sc) {
        System.out.println("Arquivo para enviar: ");
        String localArquivo = sc.nextLine();

        File arquivo = new File(localArquivo);
        if (!arquivo.exists()) {
            System.out.println("arquivo não encontrado.");
            return;
        }

        try (Socket socket = new Socket(ipCoordenador, portaCoordenador);
                DataInputStream in = new DataInputStream(socket.getInputStream());
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                FileInputStream fis = new FileInputStream(arquivo)) {
            out.writeUTF("TRANSMITIR_ARQUIVOS");
            out.writeUTF(apelido);
            out.writeUTF(arquivo.getName());
            out.writeLong(arquivo.length());

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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void listar_arquivos_usuario(Scanner sc) {
        System.out.println("\n --- arquivos ---");

        try (Socket socket = new Socket(ipCoordenador, portaCoordenador);
                DataInputStream in = new DataInputStream(socket.getInputStream());
                DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {
            out.writeUTF("LISTAR_ARQUIVOS");
            out.writeUTF(apelido);
            out.flush();

            int total = in.readInt();
            System.out.println("-- Arquivos registrados --");
            for (int i = 0; i < total; i++) {
                int id = in.readInt();
                String nome = in.readUTF();
                System.out.printf("ID: %d | %s%n", id, nome);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static void baixar_arquivos(Scanner sc) {
        System.out.println("Identificador do arquivo: ");
        int id = sc.nextInt();

        try (Socket socket = new Socket(ipCoordenador, portaCoordenador);
                DataInputStream in = new DataInputStream(socket.getInputStream());
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());) {
            out.writeUTF("BAIXAR_ARQUIVOS");
            out.writeInt(id);
            out.flush();

            String resposta = in.readUTF();
            if(!resposta.equals("OK")) {
                System.out.println("Arquivo encontrado");
                return;
            }

            long tamanho = in.readLong();
            File pasta = new File(pastaDownload);
            if(!pasta.exists()) {
                pasta.mkdirs();
            }
            File arquivo = new File(pasta, "arquivo_" + id); //Cria o "arquivo_id" para receber os dados no diretório de download.

            try (FileOutputStream fos = new FileOutputStream(arquivo)) {
                byte[] buffer = new byte[4096];
                long recebido = 0;
                while(recebido < tamanho) {
                int lido = in.read(buffer);
                    if(lido == -1){
                        break;
                    }
                    fos.write(buffer, 0, lido);
                    recebido += lido;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            System.out.println("Download concluído: " + arquivo.getAbsolutePath());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
