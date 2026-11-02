const token = sessionStorage.getItem("sessionToken");
let room;
let savedUser;
let initialMatch;
try {
  room = JSON.parse(sessionStorage.getItem("activeRoom"));
  initialMatch = JSON.parse(sessionStorage.getItem("activeMatch"));
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
  const cells = [...document.querySelectorAll(".cell")];
  let currentUserId, authenticated = false, redirecting = false, actionPending = false, authTimeout;
  let activeMatch = initialMatch || null, nextPlayerId = initialMatch
    ? (initialMatch.symbolA === "X" ? room.playerA.id : room.playerB.id) : null, roomClosed = false;

  playerAName.textContent = room.playerA?.login || "Jogador A";
  playerBName.textContent = room.playerB?.login || "Jogador B";
  if (savedUser?.login) currentPlayer.textContent = savedUser.login;

  function publish(destination, data) {
    client.publish({ destination, headers: { "content-type": "application/json" }, body: JSON.stringify(data) });
  }
  function setActionsDisabled(disabled) {
    resignButton.disabled = disabled || roomClosed;
    closeButton.disabled = disabled || roomClosed;
    renderBoard();
  }
  function renderBoard() {
    const isTurn = authenticated && activeMatch?.status === "ACTIVE" && !roomClosed && !actionPending && nextPlayerId === currentUserId;
    cells.forEach((cell, index) => {
      const value = activeMatch?.cells?.[index] || "";
      cell.textContent = value;
      cell.disabled = !isTurn || Boolean(value);
    });
  }
  function renderMatch(match, nextId) {
    activeMatch = match;
    if (nextId !== undefined) nextPlayerId = nextId;
    if (!match) return renderBoard();
    playerAScore.textContent = String(match.scoreA ?? 0);
    playerBScore.textContent = String(match.scoreB ?? 0);
    playerASymbol.textContent = `(${match.symbolA})`;
    playerBSymbol.textContent = `(${match.symbolB})`;
    renderBoard();
  }
  function handleRoundState(state) {
    actionPending = false;
    if (state.nextMatch?.match) {
      renderMatch(state.nextMatch.match, room.playerA.id === currentUserId
        ? (state.nextMatch.match.symbolA === "X" ? room.playerA.id : room.playerB.id)
        : (state.nextMatch.match.symbolA === "X" ? room.playerA.id : room.playerB.id));
    } else renderMatch(state.match, state.nextPlayerId);
    if (state.result === "WIN") status.textContent = `${state.winnerPlayerId === room.playerA.id ? room.playerA.login : room.playerB.login} venceu a rodada!`;
    else if (state.result === "DRAW") status.textContent = "Empate! Nova rodada iniciada.";
    else if (state.nextMatch) status.textContent = "Nova partida iniciada. Boa sorte!";
    else status.textContent = nextPlayerId === currentUserId ? "Sua vez de jogar." : "Vez do oponente.";
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
      renderMatch(next, next.symbolA === "X" ? room.playerA.id : room.playerB.id);
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
      renderMatch(activeMatch, nextPlayerId);
      client.subscribe(`/topic/rooms/${room.roomId}`, handleMessage);
      client.subscribe(`/topic/players/${currentUserId}`, handleMessage);
      status.textContent = nextPlayerId === currentUserId ? "Sua vez de jogar." : "Vez do oponente.";
      setActionsDisabled(false);
      return;
    }
    if (event.type === "ROUND_STATE") { handleRoundState(event.data); return; }
    if (event.type === "MATCH_STARTED") { renderMatch(event.data, event.data.symbolA === "X" ? room.playerA.id : room.playerB.id); status.textContent = "Nova partida iniciada. Sua vez será indicada no tabuleiro."; return; }
    if (event.type === "ROOM_RESULT") { handleRoomResult(event.data); return; }
    if (event.type === "ERROR") {
      status.textContent = event.error || "Não foi possível concluir a ação.";
      actionPending = false;
      if (authenticated) { setActionsDisabled(false); renderBoard(); }
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
  cells.forEach((cell) => cell.addEventListener("click", () => {
    if (!authenticated || !activeMatch || activeMatch.status !== "ACTIVE" || actionPending || !client.connected || cell.disabled) return;
    actionPending = true;
    renderBoard();
    status.textContent = "Aguardando confirmação da jogada…";
    publish("/app/rooms/move", { roomId: room.roomId, cellIndex: Number(cell.dataset.index) });
  }));
  client.activate();
}
