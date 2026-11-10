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
  const playerACard = $("#player-a-card"), playerBCard = $("#player-b-card");
  const currentPlayer = $("#current-player"), status = $("#game-status");
  const turnIndicator = $("#turn-indicator");
  const resignButton = $("#resign-button"), closeButton = $("#close-button");
  const confirmationDialog = $("#confirmation-dialog");
  const confirmationTitle = $("#confirmation-title"), confirmationDescription = $("#confirmation-description");
  const confirmationSubmit = $("#confirmation-submit");
  const roomResultDialog = $("#room-result-dialog"), roomResultDescription = $("#room-result-description");
  const roomResultButton = $("#room-result-button");
  const cells = [...document.querySelectorAll(".cell")];
  let currentUserId, authenticated = false, redirecting = false, actionPending = false, authTimeout;
  let pendingDestination = null;
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
  function updateTurnIndicator(isTurn) {
    const active = authenticated && activeMatch?.status === "ACTIVE" && !roomClosed;
    turnIndicator.textContent = !active
      ? "Aguardando o estado da partida"
      : isTurn ? "Você é o jogador da vez" : "O oponente é o jogador da vez";
    playerACard.classList.toggle("current-turn", active && nextPlayerId === room.playerA.id);
    playerBCard.classList.toggle("current-turn", active && nextPlayerId === room.playerB.id);
  }
  function renderBoard() {
    const isTurn = authenticated && activeMatch?.status === "ACTIVE" && !roomClosed && !actionPending && nextPlayerId === currentUserId;
    cells.forEach((cell, index) => {
      const value = activeMatch?.cells?.[index] || "";
      cell.textContent = value;
      cell.classList.toggle("mark-x", value === "X");
      cell.classList.toggle("mark-o", value === "O");
      cell.setAttribute("aria-label", `Casa ${index + 1}, ${value ? `marcada com ${value}` : "vazia"}`);
      cell.disabled = !isTurn || Boolean(value);
    });
    updateTurnIndicator(isTurn);
  }
  function renderMatch(match, nextId) {
    activeMatch = match;
    if (nextId !== undefined) nextPlayerId = nextId;
    if (!match) return renderBoard();
    playerAScore.textContent = String(match.scoreA ?? 0);
    playerBScore.textContent = String(match.scoreB ?? 0);
    playerASymbol.textContent = `(${match.symbolA})`;
    playerBSymbol.textContent = `(${match.symbolB})`;
    playerACard.classList.toggle("player-x", match.symbolA === "X");
    playerACard.classList.toggle("player-o", match.symbolA === "O");
    playerBCard.classList.toggle("player-x", match.symbolB === "X");
    playerBCard.classList.toggle("player-o", match.symbolB === "O");
    renderBoard();
  }
  function handleRoundState(state) {
    actionPending = false;
    if (state.nextMatch?.match) {
      renderMatch(state.nextMatch.match, state.nextMatch.match.symbolA === "X" ? room.playerA.id : room.playerB.id);
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
  function showRoomResult(message) {
    roomResultDescription.textContent = message;
    if (!roomResultDialog.open) roomResultDialog.showModal();
  }
  function handleRoomResult(result) {
    const match = result.match;
    if (match) {
      playerAScore.textContent = String(match.scoreA ?? 0);
      playerBScore.textContent = String(match.scoreB ?? 0);
    }
    if (result.roomClosed) {
      roomClosed = true;
      setActionsDisabled(true);
      status.textContent = "Sala encerrada.";
      showRoomResult("A sala foi encerrada. Volte ao painel para entrar em outra partida.");
    } else if (result.nextMatch?.match) {
      const next = result.nextMatch.match;
      renderMatch(next, next.symbolA === "X" ? room.playerA.id : room.playerB.id);
      actionPending = false;
      setActionsDisabled(false);
      status.textContent = "Nova partida iniciada. Boa sorte!";
    } else {
      setActionsDisabled(true);
      status.textContent = "Partida encerrada por desistência.";
      showRoomResult("A partida foi encerrada por desistência. Volte ao painel quando estiver pronto.");
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
    onWebSocketClose: () => { if (!redirecting && !roomResultDialog.open) goDashboard(); },
  });

  function submitAction(destination) {
    if (!authenticated || actionPending || !client.connected) return;
    actionPending = true;
    setActionsDisabled(true);
    status.textContent = "Atualizando placar…";
    publish(destination, { roomId: room.roomId });
  }
  function requestConfirmation(destination, title, description, buttonLabel) {
    if (!authenticated || actionPending || !client.connected) return;
    pendingDestination = destination;
    confirmationTitle.textContent = title;
    confirmationDescription.textContent = description;
    confirmationSubmit.textContent = buttonLabel;
    confirmationDialog.showModal();
  }
  resignButton.addEventListener("click", () => requestConfirmation("/app/rooms/resign", "Desistir da partida?", "A vitória será concedida ao oponente e esta ação não poderá ser desfeita.", "Sim, desistir"));
  closeButton.addEventListener("click", () => requestConfirmation("/app/rooms/close", "Encerrar a sala?", "A partida terminará para os dois jogadores e esta ação não poderá ser desfeita.", "Sim, encerrar"));
  confirmationDialog.addEventListener("close", () => {
    if (confirmationDialog.returnValue === "confirm" && pendingDestination) submitAction(pendingDestination);
    pendingDestination = null;
  });
  roomResultButton.addEventListener("click", goDashboard);
  cells.forEach((cell) => cell.addEventListener("click", () => {
    if (!authenticated || !activeMatch || activeMatch.status !== "ACTIVE" || actionPending || !client.connected || cell.disabled) return;
    actionPending = true;
    renderBoard();
    status.textContent = "Aguardando confirmação da jogada…";
    publish("/app/rooms/move", { roomId: room.roomId, cellIndex: Number(cell.dataset.index) });
  }));
  client.activate();
}
