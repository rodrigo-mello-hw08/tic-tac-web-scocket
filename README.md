# tic-tac-web-scocket

## Tecnologias utilizadas no backend
- Springboot (4.1.2)
- Java JDK 21
- Banco de dados H2
- Websocket (STOMP)

## Tecnologias utilizadas no backend
- HTML + CSS + Javascript

## Telas
- Homepage
- Teste de conexão
- Dashboard
- Jogo

## Features por tela
### Homepage
- Cadastro de usuário
- Login de usuário

### Teste de conexão
- Conectar ao servidor
- Desconectar
- Enviar mensagem
- Exibir mensagem

### Dashboard
- Mostrar usuário logado (cabeçalho)
- Deslogar da tela (botão do cabeçalho)
- Mostrar lista de jogadores ativos

## Como rodar a aplicação

### Pré-requisitos

- Java JDK 21
- Apache Maven instalado e disponível no terminal
- Python 3 para servir os arquivos do frontend (ou outro servidor HTTP estático, como a extensão Live Server do VS Code)
- Acesso à internet para carregar a biblioteca STOMP.js e as fontes utilizadas pelo frontend

Confirme as instalações com:

```bash
java -version
mvn -version
python --version
```

### 1. Iniciar o backend

Abra um terminal na pasta `backend` e execute:

```bash
mvn spring-boot:run
```

Aguarde a inicialização completa do Spring Boot. O servidor ficará disponível em:

```text
http://localhost:8080
```

Mantenha esse terminal aberto enquanto estiver usando a aplicação.

### 2. Iniciar o frontend

Abra outro terminal na raiz do projeto e execute:

```bash
python -m http.server 5500 --directory frontend
```

Se o comando `python` não estiver disponível no Windows, tente:

```bash
py -m http.server 5500 --directory frontend
```

A opção `--directory frontend` faz com que a aplicação seja aberta diretamente, sem exibir a listagem de diretórios do projeto.

Depois, acesse no navegador:

```text
http://localhost:5500
```

Não abra o `index.html` diretamente pelo explorador de arquivos. Use um servidor HTTP estático para que a navegação entre as páginas funcione de forma consistente.

### 3. Testar uma partida

1. Mantenha o backend e o frontend em execução.
2. Abra `http://localhost:5500` em dois navegadores ou em uma janela normal e outra anônima.
3. Cadastre e autentique um usuário diferente em cada janela.
4. No dashboard, envie um convite de um jogador para o outro.
5. Aceite o convite e realize as jogadas nos dois clientes.

A aplicação se conecta ao backend pelo endpoint WebSocket:

```text
ws://localhost:8080/ws
```

### Encerrar a aplicação

Pressione `Ctrl+C` nos terminais do backend e do frontend.
