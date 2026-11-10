(() => {
  const storageKey = "ticTacTheme";
  const root = document.documentElement;
  const systemTheme = window.matchMedia("(prefers-color-scheme: dark)");

  function savedTheme() {
    const value = localStorage.getItem(storageKey);
    return value === "light" || value === "dark" ? value : null;
  }

  function updateControls(theme) {
    document.querySelectorAll("[data-theme-toggle]").forEach((button) => {
      const nextTheme = theme === "dark" ? "claro" : "escuro";
      button.setAttribute("aria-label", `Ativar tema ${nextTheme}`);
      button.setAttribute("title", `Ativar tema ${nextTheme}`);
      button.setAttribute("aria-pressed", String(theme === "dark"));
    });
  }

  function applyTheme(theme, persist = false) {
    root.dataset.theme = theme;
    if (persist) localStorage.setItem(storageKey, theme);
    updateControls(theme);
  }

  applyTheme(savedTheme() || (systemTheme.matches ? "dark" : "light"));

  document.addEventListener("DOMContentLoaded", () => {
    updateControls(root.dataset.theme);
    document.querySelectorAll("[data-theme-toggle]").forEach((button) => {
      button.addEventListener("click", () => {
        applyTheme(root.dataset.theme === "dark" ? "light" : "dark", true);
      });
    });
  });

  systemTheme.addEventListener("change", (event) => {
    if (!savedTheme()) applyTheme(event.matches ? "dark" : "light");
  });
})();
