package Coordernador;

public class RegistroArquivo {
    private int id;
    private String nome, apelido, servidor;

    public RegistroArquivo(int id, String nome, String apelido, String servidor) {
        this.id = id;
        this.nome = nome;
        this.apelido = apelido;
        this.servidor = servidor;
    }

    public int getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getApelido() {
        return apelido;
    }

    public String getServidor() {
        return servidor;
    }
    
    @Override
    public String toString() {
        return "ID: " + id + " | NOME: " + nome + " | USUARIO: " + apelido + " | SERVIDOR: " + servidor;
    }
}
