const connectButton = document.querySelector("#connect-button");

if (connectButton) {
  const subscribeButton = document.querySelector("#subscribe-button");
  const pingButton = document.querySelector("#ping-button");
  const disconnectButton = document.querySelector("#disconnect-button");
  const connectionStatus = document.querySelector("#connection-status");
  const statusDot = document.querySelector("#status-dot");
  const statusRow = statusDot.closest(".status-row");
  const errorMessage = document.querySelector("#error-message");
  const latencyValue = document.querySelector("#latency-value");
  const responseTime = document.querySelector("#response-time");
  const responseValue = document.querySelector("#response-value");
  let subscription;
  let pingStartedAt;
  let manualDisconnect = false;

  function setStatus(label, state = "idle") {
    connectionStatus.textContent = label;
    statusRow.dataset.state = state;
    statusDot.classList.toggle("connected", state === "connected");
  }

  function showError(message) {
    errorMessage.textContent = message;
    errorMessage.hidden = false;
    setStatus("Falha na conexão", "error");
  }

  function resetControls() {
    connectButton.disabled = false;
    disconnectButton.disabled = true;
    subscribeButton.disabled = true;
    pingButton.disabled = true;
    subscription = undefined;
  }

  const client = new StompJs.Client({
    brokerURL: "ws://localhost:8080/ws",
    reconnectDelay: 5000,
    onConnect: () => {
      manualDisconnect = false;
      setStatus("Conectado", "connected");
      connectButton.disabled = true;
      disconnectButton.disabled = false;
      subscribeButton.disabled = false;
      errorMessage.hidden = true;
    },
    onWebSocketClose: () => {
      resetControls();
      setStatus(manualDisconnect ? "Desconectado" : "Conexão interrompida", manualDisconnect ? "idle" : "error");
    },
    onStompError: (frame) => showError(frame.headers.message || "O servidor recusou a conexão STOMP."),
    onWebSocketError: () => showError("Não foi possível conectar. Verifique se o backend está ativo em localhost:8080."),
  });

  connectButton.addEventListener("click", () => {
    errorMessage.hidden = true;
    manualDisconnect = false;
    setStatus("Conectando…", "connecting");
    connectButton.disabled = true;
    client.activate();
  });

  disconnectButton.addEventListener("click", async () => {
    manualDisconnect = true;
    disconnectButton.disabled = true;
    setStatus("Desconectando…", "connecting");
    subscription?.unsubscribe();
    subscription = undefined;
    await client.deactivate();
    resetControls();
    setStatus("Desconectado");
  });

  subscribeButton.addEventListener("click", () => {
    if (!client.connected || subscription) return;

    subscription = client.subscribe("/topic/ping-pong", (message) => {
      let received;
      try {
        const event = JSON.parse(message.body);
        received = event.data?.message ?? event.error ?? message.body;
      } catch {
        received = message.body;
      }

      const now = performance.now();
      responseValue.textContent = String(received);
      responseTime.textContent = new Intl.DateTimeFormat("pt-BR", {
        hour: "2-digit", minute: "2-digit", second: "2-digit",
      }).format(new Date());
      latencyValue.textContent = pingStartedAt == null ? "—" : `${Math.max(0, Math.round(now - pingStartedAt))} ms`;
      pingStartedAt = undefined;
      setStatus("Resposta recebida", "connected");
      window.alert(String(received));
    });
    subscribeButton.disabled = true;
    pingButton.disabled = false;
    setStatus("Conectado e pronto", "connected");
  });

  pingButton.addEventListener("click", () => {
    if (!client.connected || !subscription) return;

    pingStartedAt = performance.now();
    responseValue.textContent = "Aguardando resposta…";
    latencyValue.textContent = "Calculando…";
    setStatus("Ping enviado", "connected");
    client.publish({
      destination: "/app/ping-pong",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ message: "ping" }),
    });
  });
}
