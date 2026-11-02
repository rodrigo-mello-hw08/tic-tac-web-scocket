const sessionToken = sessionStorage.getItem("sessionToken");

if (!sessionToken) {
  window.location.replace("../index.html");
} else {
  const currentLogin = document.querySelector("#current-login");
  const connectionStatus = document.querySelector("#connection-status");
  const connectionIndicator = document.querySelector("#connection-indicator");
  const logoutButton = document.querySelector("#logout-button");
  const userList = document.querySelector("#user-list");
  const userCount = document.querySelector("#user-count");
  const errorMessage = document.querySelector("#error-message");
  let authenticated = false;
  let redirecting = false;
  let authTimeout;
  let activeUsers = [];
  let client;

  function goHome(message) {
    if (redirecting) return;
    redirecting = true;
    sessionStorage.removeItem("sessionToken");
    if (message) sessionStorage.setItem("authMessage", message);
    if (authTimeout) clearTimeout(authTimeout);
    if (client?.active) {
      client.deactivate().finally(() => window.location.replace("../index.html"));
    } else {
      window.location.replace("../index.html");
    }
  }

  function renderUsers(users) {
    activeUsers = Array.isArray(users) ? users : [];
    if (!authenticated) return;

    const otherUsers = activeUsers.filter((user) => user.id !== currentUserId);
    userCount.textContent = String(otherUsers.length);
    userList.replaceChildren();

    if (otherUsers.length === 0) {
      const empty = document.createElement("li");
      empty.className = "empty-state";
      empty.textContent = "Nenhum outro usuário está online no momento.";
      userList.append(empty);
      return;
    }

    for (const user of otherUsers) {
      const item = document.createElement("li");
      item.className = "user-item";
      const dot = document.createElement("span");
      dot.className = "online-dot";
      const login = document.createElement("span");
      login.textContent = user.login;
      item.append(dot, login);
      userList.append(item);
    }
  }

  let currentUserId;

  function handleMessage(message) {
    let event;
    try {
      event = JSON.parse(message.body);
    } catch {
      goHome("O servidor retornou uma resposta inválida.");
      return;
    }

    if (event.type === "ERROR") {
      goHome(event.error || "Não foi possível validar sua sessão.");
      return;
    }

    if (event.type === "LOGGED_IN") {
      if (authTimeout) clearTimeout(authTimeout);
      if (!event.data?.id || !event.data?.login) {
        goHome("O servidor não confirmou sua sessão.");
        return;
      }
      authenticated = true;
      currentUserId = event.data.id;
      currentLogin.textContent = event.data.login;
      connectionStatus.textContent = "Conectado e autenticado";
      connectionIndicator.classList.add("connected");
      logoutButton.disabled = false;
      sessionStorage.removeItem("sessionToken");
      client.publish({
        destination: "/app/users/active",
        headers: { "content-type": "application/json" },
        body: JSON.stringify({}),
      });
      renderUsers(activeUsers);
      return;
    }

    if (event.type === "ACTIVE_USERS") {
      renderUsers(event.data);
      return;
    }

    if (event.type === "LOGGED_OUT") {
      goHome();
    }
  }

  client = new StompJs.Client({
    brokerURL: "ws://localhost:8080/ws",
    reconnectDelay: 0,
    onConnect: () => {
      connectionStatus.textContent = "Autenticando sessão…";
      client.subscribe("/user/queue/users", handleMessage);
      client.subscribe("/topic/users/active", handleMessage);
      client.publish({
        destination: "/app/users/session",
        headers: { "content-type": "application/json" },
        body: JSON.stringify({ sessionToken }),
      });
      authTimeout = setTimeout(() => goHome("A autenticação expirou. Entre novamente."), 8000);
    },
    onStompError: (frame) => goHome(frame.headers.message || "O servidor recusou a conexão STOMP."),
    onWebSocketError: () => goHome("Não foi possível conectar ao servidor."),
    onWebSocketClose: () => {
      if (!redirecting) goHome("A conexão com o servidor foi encerrada. Entre novamente.");
    },
  });

  logoutButton.addEventListener("click", () => {
    if (!authenticated || !client.connected) return;
    logoutButton.disabled = true;
    connectionStatus.textContent = "Encerrando sessão…";
    client.publish({
      destination: "/app/users/logout",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({}),
    });
  });

  client.activate();
}
