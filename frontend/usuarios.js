const loginForm = document.querySelector("#login-form");

if (loginForm) {
  const registerForm = document.querySelector("#register-form");
  const loginTab = document.querySelector("#login-tab");
  const registerTab = document.querySelector("#register-tab");
  const loginPanel = document.querySelector("#login-panel");
  const registerPanel = document.querySelector("#register-panel");
  const status = document.querySelector("#connection-status");
  const error = document.querySelector("#error-message");
  const forms = [loginForm, registerForm];
  let pendingRequest;
  let client;
  let navigating = false;

  function showPanel(panelName) {
    const showLogin = panelName === "login";
    loginPanel.hidden = !showLogin;
    registerPanel.hidden = showLogin;
    loginTab.classList.toggle("active", showLogin);
    registerTab.classList.toggle("active", !showLogin);
    loginTab.setAttribute("aria-selected", String(showLogin));
    registerTab.setAttribute("aria-selected", String(!showLogin));
    clearMessages();
  }

  function clearMessages() {
    error.hidden = true;
    error.textContent = "";
    status.textContent = "";
  }

  function setBusy(busy) {
    for (const form of forms) {
      for (const control of form.querySelectorAll("input, button")) control.disabled = busy;
    }
  }

  function fail(message) {
    pendingRequest = undefined;
    setBusy(false);
    status.textContent = "";
    error.textContent = message;
    error.hidden = false;
  }

  function publishPendingRequest() {
    if (!pendingRequest || !client?.connected) return;
    client.publish({
      destination: pendingRequest.destination,
      headers: { "content-type": "application/json" },
      body: JSON.stringify(pendingRequest.body),
    });
    status.textContent = "Aguardando resposta do servidor…";
  }

  function createClient() {
    client = new StompJs.Client({
      brokerURL: "ws://localhost:8080/ws",
      reconnectDelay: 0,
      onConnect: () => {
        status.textContent = "Conectado. Enviando solicitação…";
        client.subscribe("/user/queue/users", (message) => {
          let event;
          try {
            event = JSON.parse(message.body);
          } catch {
            fail("O servidor retornou uma resposta inválida.");
            return;
          }

          if (event.type === "ERROR") {
            fail(event.error || "Não foi possível concluir a solicitação.");
            return;
          }

          if (event.type === "REGISTERED") {
            pendingRequest = undefined;
            setBusy(false);
            registerForm.reset();
            showPanel("login");
            status.textContent = "Conta criada. Agora entre com seu login e senha.";
            return;
          }

          if (event.type === "LOGGED_IN") {
            const token = event.data?.sessionToken;
            if (!token) {
              fail("O servidor não retornou o token da sessão.");
              return;
            }

            pendingRequest = undefined;
            navigating = true;
            sessionStorage.setItem("sessionToken", token);
            status.textContent = "Login realizado. Abrindo dashboard…";
            client.deactivate().finally(() => {
              window.location.assign("dashboard/index.html");
            });
          }
        });
        publishPendingRequest();
      },
      onStompError: (frame) => {
        fail(frame.headers.message || "O servidor recusou a solicitação STOMP.");
      },
      onWebSocketError: () => {
        fail("Não foi possível conectar. Verifique se o backend está ativo em localhost:8080.");
      },
      onWebSocketClose: () => {
        if (!navigating && pendingRequest) fail("A conexão com o servidor foi encerrada.");
      },
    });
  }

  function submit(destination, body) {
    clearMessages();
    pendingRequest = { destination, body };
    setBusy(true);
    status.textContent = "Conectando ao servidor…";

    if (!client) createClient();
    if (client.connected) publishPendingRequest();
    else client.activate();
  }

  loginTab.addEventListener("click", () => showPanel("login"));
  registerTab.addEventListener("click", () => showPanel("register"));

  loginForm.addEventListener("submit", (event) => {
    event.preventDefault();
    const values = new FormData(loginForm);
    submit("/app/users/login", {
      login: values.get("login").trim(),
      password: values.get("password"),
    });
  });

  registerForm.addEventListener("submit", (event) => {
    event.preventDefault();
    const values = new FormData(registerForm);
    const password = values.get("password");
    if (password !== values.get("passwordRepeat")) {
      error.textContent = "As senhas não coincidem.";
      error.hidden = false;
      return;
    }

    submit("/app/users/register", {
      login: values.get("login").trim(),
      password,
    });
  });
}
