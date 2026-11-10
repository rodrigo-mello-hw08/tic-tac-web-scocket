const sessionToken = sessionStorage.getItem("sessionToken");
if (!sessionToken) window.location.replace("../index.html");
else {
  const $ = (selector) => document.querySelector(selector);
  const currentLogin = $("#current-login"), currentAvatar = $("#current-avatar"), connectionStatus = $("#connection-status");
  const connectionIndicator = $("#connection-indicator"), logoutButton = $("#logout-button");
  const userList = $("#user-list"), inviteList = $("#invite-list");
  const userCount = $("#user-count"), inviteCount = $("#invite-count"), errorMessage = $("#error-message");
  const historyList = $("#history-list");
  let authenticated = false, redirecting = false, authTimeout, currentUserId, client;
  const pendingOutgoing = new Map(), pendingIncoming = new Map();

  function publish(destination, data) {
    client.publish({ destination, headers: { "content-type": "application/json" }, body: JSON.stringify(data) });
  }
  function showError(message) {
    errorMessage.textContent = message;
    errorMessage.hidden = false;
    setTimeout(() => { errorMessage.hidden = true; }, 5000);
  }
  function goHome(message) {
    if (redirecting) return;
    redirecting = true;
    sessionStorage.removeItem("sessionToken");
    if (message) sessionStorage.setItem("authMessage", message);
    if (authTimeout) clearTimeout(authTimeout);
    if (client?.active) client.deactivate().finally(() => window.location.replace("../index.html"));
    else window.location.replace("../index.html");
  }
  function goToRoom(startedRoom) {
    if (redirecting) return;
    redirecting = true;
    sessionStorage.setItem("activeRoom", JSON.stringify(startedRoom.room));
    sessionStorage.setItem("activeMatch", JSON.stringify(startedRoom.match));
    sessionStorage.setItem("currentUser", JSON.stringify({ id: currentUserId, login: currentLogin.textContent }));
    if (client?.active) client.deactivate().finally(() => window.location.assign("../tabuleiro/index.html"));
    else window.location.assign("../tabuleiro/index.html");
  }
  function renderUsers(users) {
    if (!authenticated) return;
    const others = (Array.isArray(users) ? users : []).filter((user) => user.id !== currentUserId);
    userCount.textContent = String(others.length);
    userList.replaceChildren();
    if (!others.length) {
      const empty = document.createElement("li"); empty.className = "empty-state";
      empty.textContent = "Nenhum outro usuário está online no momento."; userList.append(empty); return;
    }
    for (const user of others) {
      const item = document.createElement("li"); item.className = "user-item";
      const identity = document.createElement("span"); identity.className = "user-identity";
      const avatar = document.createElement("span"); avatar.className = "user-avatar"; avatar.setAttribute("aria-hidden", "true");
      avatar.textContent = user.login.slice(0, 2);
      const dot = document.createElement("span"); dot.className = "online-dot";
      const login = document.createElement("span"); login.textContent = user.login;
      identity.append(avatar, dot, login);
      const button = document.createElement("button"); button.className = "button invite-button"; button.type = "button";
      const pending = pendingOutgoing.get(user.id);
      button.disabled = Boolean(pending) || !client.connected;
      if (pending) {
        const spinner = document.createElement("span"); spinner.className = "spinner"; spinner.setAttribute("aria-hidden", "true");
        button.append(spinner, " Aguardando");
      } else button.textContent = "Convidar";
      button.setAttribute("aria-label", pending ? `Aguardando resposta de ${user.login}` : `Enviar convite para ${user.login}`);
      button.title = pending ? "Aguardando resposta" : "Enviar convite";
      button.addEventListener("click", () => {
        if (!client.connected || pendingOutgoing.has(user.id)) return;
        button.disabled = true; button.textContent = "Aguardando…";
        pendingOutgoing.set(user.id, { inviteId: null, user });
        renderUsers(activeUsers);
        publish("/app/invites/send", { toPlayerId: user.id });
      });
      item.append(identity, button); userList.append(item);
    }
  }
  let activeUsers = [];
  function renderInvites() {
    inviteCount.textContent = String(pendingIncoming.size);
    inviteList.replaceChildren();
    if (!pendingIncoming.size) {
      const empty = document.createElement("li"); empty.className = "empty-state";
      empty.textContent = "Nenhum convite pendente."; inviteList.append(empty); return;
    }
    for (const invite of pendingIncoming.values()) {
      const item = document.createElement("li"); item.className = "invite-item";
      const label = document.createElement("span"); label.textContent = `${invite.from.login} convidou você`;
      const actions = document.createElement("div"); actions.className = "invite-actions";
      for (const [accepted, text] of [[true, "Aceitar"], [false, "Recusar"]]) {
        const button = document.createElement("button"); button.className = `button ${accepted ? "accept-button" : "decline-button"}`;
        button.type = "button"; button.textContent = text;
        button.addEventListener("click", () => publish("/app/invites/respond", { inviteId: invite.inviteId, accepted }));
        actions.append(button);
      }
      item.append(label, actions); inviteList.append(item);
    }
  }
  function renderHistory(entries) {
    historyList.replaceChildren();
    if (!entries.length) {
      const empty = document.createElement("li"); empty.className = "empty-state";
      empty.textContent = "Você ainda não tem salas finalizadas."; historyList.append(empty); return;
    }
    for (const entry of entries) {
      const item = document.createElement("li"); item.className = "history-item";
      const players = document.createElement("span"); players.className = "history-players";
      players.textContent = `${entry.playerName} ${entry.playerScore} × ${entry.opponentName} ${entry.opponentScore}`;
      const result = document.createElement("strong"); result.className = `history-result ${entry.result.toLowerCase()}`;
      result.textContent = entry.result;
      item.append(players, result); historyList.append(item);
    }
  }
  function handleMessage(message) {
    let event;
    try { event = JSON.parse(message.body); }
    catch { showError("O servidor retornou uma resposta inválida."); return; }
    if (event.type === "ERROR") { showError(event.error || "Não foi possível concluir a operação."); return; }
    if (event.type === "LOGGED_IN") {
      if (authTimeout) clearTimeout(authTimeout);
      if (!event.data?.id || !event.data?.login) { goHome("O servidor não confirmou sua sessão."); return; }
      authenticated = true; currentUserId = event.data.id; currentLogin.textContent = event.data.login;
      currentAvatar.textContent = event.data.login.slice(0, 2);
      connectionStatus.textContent = "Conectado e autenticado"; connectionIndicator.classList.add("connected");
      logoutButton.disabled = false;
      if (event.data.sessionToken) sessionStorage.setItem("sessionToken", event.data.sessionToken);
      client.subscribe(`/topic/players/${currentUserId}`, handleMessage);
      publish("/app/invites/pending", {});
      publish("/app/history/list", {});
      publish("/app/users/active", {}); renderUsers(activeUsers); return;
    }
    if (event.type === "ACTIVE_USERS") { activeUsers = Array.isArray(event.data) ? event.data : []; renderUsers(activeUsers); return; }
    if (event.type === "ROOM_HISTORY") { renderHistory(Array.isArray(event.data) ? event.data : []); return; }
    if (event.type === "INVITE_RECEIVED") { pendingIncoming.set(event.data.inviteId, event.data); renderInvites(); return; }
    if (event.type === "INVITES_PENDING") {
      pendingIncoming.clear();
      for (const invite of (Array.isArray(event.data) ? event.data : [])) pendingIncoming.set(invite.inviteId, invite);
      renderInvites(); return;
    }
    if (event.type === "INVITE_SENT") {
      const sent = event.data; pendingOutgoing.set(sent.to.id, { inviteId: sent.inviteId, user: sent.to }); renderUsers(activeUsers); return;
    }
    if (event.type === "INVITE_SEND_FAILED") {
      pendingOutgoing.delete(event.data.toPlayerId); renderUsers(activeUsers); showError(event.data.message); return;
    }
    if (event.type === "INVITE_DECLINED") {
      const declined = event.data;
      pendingIncoming.delete(declined.inviteId);
      pendingOutgoing.delete(declined.toPlayerId);
      renderInvites(); renderUsers(activeUsers);
      if (currentUserId === declined.fromPlayerId) showError(`${declined.toPlayer.login} recusou o convite.`);
      return;
    }
    if (event.type === "INVITE_CANCELLED") {
      const cancelled = event.data;
      pendingIncoming.delete(cancelled.inviteId);
      if (cancelled.otherPlayerId) pendingOutgoing.delete(cancelled.otherPlayerId);
      renderInvites(); renderUsers(activeUsers); return;
    }
    if (event.type === "ROOM_JOINED") { goToRoom(event.data); return; }
    if (event.type === "LOGGED_OUT") goHome();
  }
  client = new StompJs.Client({
    brokerURL: "ws://localhost:8080/ws", reconnectDelay: 0,
    onConnect: () => {
      connectionStatus.textContent = "Autenticando sessão…";
      client.subscribe("/user/queue/users", handleMessage);
      client.subscribe("/topic/users/active", (message) => {
        const event = JSON.parse(message.body); if (event.type === "ACTIVE_USERS") { activeUsers = event.data || []; renderUsers(activeUsers); }
      });
      publish("/app/users/session", { sessionToken });
      authTimeout = setTimeout(() => goHome("A autenticação expirou. Entre novamente."), 8000);
    },
    onStompError: (frame) => goHome(frame.headers.message || "O servidor recusou a conexão STOMP."),
    onWebSocketError: () => goHome("Não foi possível conectar ao servidor."),
    onWebSocketClose: () => { if (!redirecting) goHome("A conexão com o servidor foi encerrada. Entre novamente."); },
  });
  logoutButton.addEventListener("click", () => {
    if (!authenticated || !client.connected) return;
    logoutButton.disabled = true; connectionStatus.textContent = "Encerrando sessão…"; publish("/app/users/logout", {});
  });
  client.activate();
}
