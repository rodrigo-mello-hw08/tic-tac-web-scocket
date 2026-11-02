const token = sessionStorage.getItem("sessionToken");
let room;
let savedUser;
try {
  room = JSON.parse(sessionStorage.getItem("activeRoom"));
  savedUser = JSON.parse(sessionStorage.getItem("currentUser"));
} catch { room = null; savedUser = null; }

if (!token || !room?.roomId) window.location.replace("../dashboard/index.html");
else {
  const $ = (selector) => document.querySelector(selector);
  const playerAName = $("#player-a-name"), playerBName = $("#player-b-name");
  const playerASymbol = $("#player-a-symbol"), playerBSymbol = $("#player-b-symbol");
  const playerAScore = $("#player-a-score"), playerBScore = $("#player-b-score");
  const currentPlayer = $("#current-player"), status = $("#game-status");
  const resignButton = $("#resign-button"), closeButton = $("#close-button");
  let currentUserId, authenticated = false, redirecting = false, actionPending = false, authTimeout;

  playerAName.textContent = room.playerA?.login || "Jogador A";
  playerBName.textContent = room.playerB?.login || "Jogador B";
  if (savedUser?.login) currentPlayer.textContent = savedUser.login;

  function publish(destination, data) {
    client.publish({ destination, headers: { "content-type": "application/json" }, body: JSON.stringify(data) });
  }
  function setActionsDisabled(disabled) {
    resignButton.disabled = disabled;
    closeButton.disabled = disabled;
  }
  function goDashboard() {
    if (redirecting) return;
    redirecting = true;
    if (client?.active) client.deactivate().finally(() => window.location.replace("../dashboard/index.html"));
    else window.location.replace("../dashboard/index.html");
  }
  function handleRoomResult(result) {
    const match = result.match;
    playerAScore.textContent = String(match.scoreA);
    playerBScore.textContent = String(match.scoreB);
    if (result.roomClosed) {
      setActionsDisabled(true);
      status.textContent = "Sala encerrada.";
      alert("A sala foi encerrada. Você será redirecionado para o dashboard.");
      goDashboard();
    } else if (result.nextMatch?.match) {
      const next = result.nextMatch.match;
      playerASymbol.textContent = `(${next.symbolA})`;
      playerBSymbol.textContent = `(${next.symbolB})`;
      document.querySelectorAll(".cell").forEach((cell) => { cell.textContent = ""; });
      actionPending = false;
      setActionsDisabled(false);
      status.textContent = "Nova partida iniciada. Boa sorte!";
    } else {
      setActionsDisabled(true);
      status.textContent = "Partida encerrada por desistência.";
    }
  }
  function handleMessage(message) {
    let event;
    try { event = JSON.parse(message.body); }
    catch { status.textContent = "Resposta inválida recebida do servidor."; return; }
    if (event.type === "LOGGED_IN") {
      if (authTimeout) clearTimeout(authTimeout);
      if (!event.data?.id) { status.textContent = "Não foi possível validar sua sessão."; goDashboard(); return; }
      currentUserId = event.data.id;
      currentPlayer.textContent = event.data.login || savedUser?.login || "Jogador";
      if (event.data.sessionToken) sessionStorage.setItem("sessionToken", event.data.sessionToken);
      const member = room.playerA?.id === currentUserId || room.playerB?.id === currentUserId;
      if (!member) { status.textContent = "Sua conta não pertence a esta sala."; goDashboard(); return; }
      authenticated = true;
      client.subscribe(`/topic/rooms/${room.roomId}`, handleMessage);
      client.subscribe(`/topic/players/${currentUserId}`, handleMessage);
      status.textContent = "Partida em andamento.";
      setActionsDisabled(false);
      return;
    }
    if (event.type === "ROOM_RESULT") { handleRoomResult(event.data); return; }
    if (event.type === "ERROR") {
      status.textContent = event.error || "Não foi possível concluir a ação.";
      actionPending = false;
      if (authenticated) setActionsDisabled(false);
    }
  }

  const client = new StompJs.Client({
    brokerURL: "ws://localhost:8080/ws", reconnectDelay: 0,
    onConnect: () => {
      client.subscribe("/user/queue/users", handleMessage);
      publish("/app/users/session", { sessionToken: token });
      authTimeout = setTimeout(() => { status.textContent = "A autenticação expirou."; goDashboard(); }, 8000);
    },
    onStompError: (frame) => { status.textContent = frame.headers.message || "O servidor recusou a conexão."; goDashboard(); },
    onWebSocketError: () => { status.textContent = "Não foi possível conectar ao servidor."; },
    onWebSocketClose: () => { if (!redirecting) goDashboard(); },
  });

  function submitAction(destination) {
    if (!authenticated || actionPending || !client.connected) return;
    actionPending = true;
    setActionsDisabled(true);
    status.textContent = "Atualizando placar…";
    publish(destination, { roomId: room.roomId });
  }
  resignButton.addEventListener("click", () => submitAction("/app/rooms/resign"));
  closeButton.addEventListener("click", () => submitAction("/app/rooms/close"));
  client.activate();
}
