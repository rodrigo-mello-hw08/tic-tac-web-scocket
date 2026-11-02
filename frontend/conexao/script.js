const connectButton = document.querySelector("#connect-button");

if (connectButton) {
  const subscribeButton = document.querySelector("#subscribe-button");
  const pingButton = document.querySelector("#ping-button");
  const disconnectButton = document.querySelector("#disconnect-button");
  const connectionStatus = document.querySelector("#connection-status");
  const statusDot = document.querySelector("#status-dot");
  const errorMessage = document.querySelector("#error-message");
  let subscription;

  const client = new StompJs.Client({
    brokerURL: "ws://localhost:8080/ws",
    reconnectDelay: 5000,
    onConnect: () => {
      connectionStatus.textContent = "Conectado";
      statusDot.classList.add("connected");
      connectButton.disabled = true;
      disconnectButton.disabled = false;
      subscribeButton.disabled = false;
      errorMessage.hidden = true;
    },
    onWebSocketClose: () => {
      connectionStatus.textContent = "Desconectado";
      statusDot.classList.remove("connected");
      connectButton.disabled = false;
      disconnectButton.disabled = true;
      subscribeButton.disabled = true;
      pingButton.disabled = true;
      subscription = undefined;
    },
    onStompError: (frame) => {
      errorMessage.textContent = frame.headers.message || "O servidor recusou a conexão STOMP.";
      errorMessage.hidden = false;
    },
    onWebSocketError: () => {
      errorMessage.textContent = "Não foi possível conectar. Verifique se o backend está ativo em localhost:8080.";
      errorMessage.hidden = false;
    },
  });

  connectButton.addEventListener("click", () => {
    errorMessage.hidden = true;
    connectionStatus.textContent = "Conectando…";
    connectButton.disabled = true;
    client.activate();
  });

  disconnectButton.addEventListener("click", async () => {
    disconnectButton.disabled = true;
    connectionStatus.textContent = "Desconectando…";
    subscription?.unsubscribe();
    subscription = undefined;
    await client.deactivate();
    connectionStatus.textContent = "Desconectado";
    statusDot.classList.remove("connected");
    connectButton.disabled = false;
    subscribeButton.disabled = true;
    pingButton.disabled = true;
  });

  subscribeButton.addEventListener("click", () => {
    if (!client.connected || subscription) return;

    subscription = client.subscribe("/topic/ping-pong", (message) => {
      const event = JSON.parse(message.body);
      const received = event.data?.message ?? event.error ?? message.body;
      window.alert(received);
    });
    subscribeButton.disabled = true;
    pingButton.disabled = false;
    connectionStatus.textContent = "Conectado e pronto para jogar";
  });

  pingButton.addEventListener("click", () => {
    if (!client.connected || !subscription) return;

    client.publish({
      destination: "/app/ping-pong",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ message: "ping" }),
    });
  });
}
