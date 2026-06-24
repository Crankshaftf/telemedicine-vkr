const API_BASE = window.location.origin;

const Api = {
  token: localStorage.getItem("medconnect_token") || "",

  setToken(token) {
    this.token = token;
    localStorage.setItem("medconnect_token", token);
  },

  clearToken() {
    this.token = "";
    localStorage.removeItem("medconnect_token");
    localStorage.removeItem("medconnect_role");
  },

  async request(method, path, body = null, { auth = true, redirectOn401 = true } = {}) {
    const headers = { "Content-Type": "application/json" };
    if (auth && this.token) headers["Authorization"] = `Bearer ${this.token}`;

    const opts = { method, headers };
    if (body) opts.body = JSON.stringify(body);

    const res = await fetch(`${API_BASE}${path}`, opts);

    if (res.status === 401) {
      const isAuthEndpoint = path.startsWith("/auth/login") || path.startsWith("/auth/register");
      if (isAuthEndpoint) {
        let msg = "Неверный логин или пароль";
        try {
          const err = await res.json();
          msg = err.detail || msg;
        } catch (_) {}
        throw new Error(typeof msg === "string" ? msg : "Ошибка входа");
      }
      if (redirectOn401) {
        this.clearToken();
        App.showLogin();
        throw new Error("Сессия истекла. Войдите снова.");
      }
    }

    if (!res.ok) {
      let msg = `Ошибка ${res.status}`;
      try {
        const err = await res.json();
        msg = err.detail || msg;
      } catch (_) {}
      throw new Error(typeof msg === "string" ? msg : JSON.stringify(msg));
    }

    if (res.status === 204) return null;
    return res.json();
  },

  login(login, password) {
    this.clearToken();
    return this.request("POST", "/auth/login", { login, password }, { auth: false, redirectOn401: false });
  },

  getProfile() {
    return this.request("GET", "/profile");
  },

  getDoctorAppointments() {
    return this.request("GET", "/appointments/doctor/list");
  },

  getAdminAppointments() {
    return this.request("GET", "/admin/appointments");
  },

  getAppointment(id) {
    return this.request("GET", `/appointments/${id}`);
  },

  getConsultation(appointmentId) {
    return this.request("GET", `/consultations/${appointmentId}`);
  },

  sendMessage(consultationId, text) {
    return this.request("POST", `/consultations/${consultationId}/message`, { text });
  },

  saveResult(consultationId, data) {
    return this.request("POST", `/consultations/${consultationId}/result`, data);
  },

  getDoctors() {
    return this.request("GET", "/doctors");
  },

  getAdminSchedule(doctorId) {
    return this.request("GET", `/admin/schedule/${doctorId}`);
  },

  createSlot(data) {
    return this.request("POST", "/admin/slots", data);
  },

  updateSlotStatus(slotId, status) {
    return this.request("PUT", `/admin/slots/${slotId}`, { status });
  },

  updateAppointmentStatus(id, status) {
    return this.request("PUT", `/admin/appointments/${id}/status`, { status });
  },

  getReports() {
    return this.request("GET", "/reports");
  },

  cancelAppointment(id) {
    return this.request("PUT", `/appointments/${id}/cancel`);
  },
};

function fmtDate(iso) {
  try {
    return new Date(iso).toLocaleString("ru-RU", {
      day: "2-digit", month: "2-digit", year: "numeric",
      hour: "2-digit", minute: "2-digit",
    });
  } catch { return iso; }
}

function statusLabel(s) {
  const map = {
    created: "Создана", confirmed: "Подтверждена", rescheduled: "Перенесена",
    cancelled: "Отменена", completed: "Завершена",
    free: "Свободен", busy: "Занят", blocked: "Заблокирован",
  };
  return map[s] || s;
}

function statusClass(s) {
  if (["confirmed", "created"].includes(s)) return "status-confirmed";
  if (s === "completed") return "status-completed";
  if (s === "cancelled") return "status-cancelled";
  return "status-created";
}

function showError(elId, msg) {
  const el = document.getElementById(elId);
  if (el) { el.textContent = msg; el.classList.remove("hidden"); }
}

function hideError(elId) {
  const el = document.getElementById(elId);
  if (el) el.classList.add("hidden");
}

function showSuccess(elId, msg) {
  const el = document.getElementById(elId);
  if (el) { el.textContent = msg; el.classList.remove("hidden"); }
}
