# Projeto: Servidor de Backup em Rede

Este repositório implementa um protótipo de sistema de backup em rede (Cliente, Coordenador e Servidor de Arquivos) em Java, para a disciplina Fundamentos de Redes de Computadores.

Resumo rápido:
- Canal de controle (Coordenador <-> Servidor de Arquivos): porta `999` (cadastro / descadastro)
- Canal de dados (Coordenador <-> Cliente): porta `10000` (upload, listagem, download)
- Servidor de Arquivos expõe seu próprio servidor de dados em uma porta configurada (ex.: `8000`)

Requisitos mínimos
- Java 11+ instalado
- Sistema operacional: Linux (testado)

Compilar
---------
No diretório raiz do projeto execute:

```bash
mkdir -p out
javac -d out Cliente/Cliente.java Coordernador/*.java ServidorDeArquivo/ServidorDeArquivo.java
```

Executar (fluxo de teste mínimo)
--------------------------------
1) Iniciar o Coordenador com permissão de administrador(em um terminal):

```bash
sudo java -cp out Coordernador.Coordernador
```

2) Iniciar um Servidor de Arquivos (outro terminal):

```bash
java -cp out ServidorDeArquivo.ServidorDeArquivo
# Quando solicitado, informe:
# - Endereço IP do Coordenador (por exemplo: 127.0.0.1)
# - Porta que o servidor irá funcionar (por exemplo: 8000)
```

Observação: o servidor de arquivos tentará cadastrar-se no Coordenador pela porta `999`. O Coordenador agora responde com `OK` ou `ERRO` para confirmar cadastro/descadastro.

3) Iniciar o Cliente (outro terminal):

```bash
java -cp out Cliente.Cliente
```

No menu do cliente:
- Vá em `4. Configurações` e configure: apelido, diretório de download e IP do Coordenador.
- `1. Transmitir Arquivos`: informe o caminho do arquivo local a enviar. O cliente enviará ao Coordenador, que encaminha ao Servidor de Arquivos e retorna um ID.
- `2. Listar arquivos disponíveis por apelido`: solicitará ao Coordenador a lista de arquivos do apelido configurado.
- `3. Baixar arquivos`: informe o ID retornado anteriormente para baixar o arquivo. O Coordenador recuperará do servidor e repassará ao cliente; após envio o Coordenador remove o registro e o Servidor apaga a cópia local.

Observações sobre o protocolo e alterações realizadas
-------------------------------------------------
- O controle (cadastro/descadastro) usa mensagens em linha terminada por `\n` (por exemplo: `CADASTRAR_SERVIDOR_DE_ARQUIVOS:8000`).
- O Coordenador agora valida a mensagem de controle e responde `OK` ou `ERRO:...` para que o Servidor confirme o cadastro.
- O canal de dados Entre Cliente<->Coordenador e Coordenador<->Servidor usa `DataInputStream`/`DataOutputStream` e `writeUTF`/`readUTF` para os comandos e metadados, seguidos pelo envio dos bytes do arquivo.

Limitações atuais
------------------
- Os registros (lista de servidores e registros de arquivo) são mantidos em memória (não há persistência entre reinicializações do Coordenador).
- As listas não foram completamente convertidas para estruturas thread-safe; em cargas muito altas, condições de corrida podem aparecer. Para um uso acadêmico/avaliativo isso é aceitável, mas pode ser melhorado.

Dicas de depuração
-------------------
- Se o servidor não aparece como cadastrado, verifique:
  - O IP informado no servidor corresponde ao host onde o Coordenador está rodando.
  - A porta `999` não está bloqueada por firewall.
- Mensagens de erro aparecem no console do Coordenador e do Servidor para ajudar no diagnóstico.

Contribuidores
--------------
- (Adicione aqui os nomes dos membros do grupo)

Se quiser, eu também gero um `protocol.md` com descrição formal de cada mensagem ou aplico melhorias opcionais (persistência ou sincronização de coleções). Diga o que prefere.
