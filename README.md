# tic-tac-web-scocket

Jogo da velha multiplayer com backend Java/Spring Boot e frontend em HTML, CSS e JavaScript.

## Tecnologias

- Java JDK 21, Spring Boot, H2 e WebSocket (STOMP)
- HTML, CSS e JavaScript

## Como executar a aplicação

### Backend

Instale o Java JDK 21. A página de downloads do Oracle está disponível em:

<https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html>

Abra um terminal na raiz do projeto e execute o JAR incluído em `planos`:

```powershell
java -jar planos\tic-1.0.0.jar
```

O backend ficará disponível em `http://localhost:8080`. Mantenha o terminal aberto enquanto estiver usando a aplicação.

### Frontend

Com o backend em execução, abra o arquivo `frontend/index.html` no navegador. A aplicação frontend se conecta ao backend pelo endereço WebSocket `ws://localhost:8080/ws`.

Para fazer login, use uma das contas padrão criadas pelo backend:

| Login | Senha |
| --- | --- |
| `admin` | `admin` |
| `rodrigo` | `1234` |
| `pedro` | `1234` |

Para testar uma partida, abra o frontend em dois navegadores ou em uma janela normal e outra anônima, autentique um usuário diferente em cada uma e jogue.

## Opcional: gerar o JAR

Para compilar o backend e gerar um novo JAR, instale também o Apache Maven. No terminal, entre na pasta `backend` e execute:

```powershell
mvn clean install
```

O Maven gerará o JAR em `backend/target/`. Para executá-lo, use o nome correspondente ao arquivo gerado, por exemplo:

```powershell
java -jar target\tic-0.0.1-SNAPSHOT.jar
```

O comando `mvn clean install` precisa de acesso à internet para baixar as dependências do projeto.
