const App = {
  profile: null,
  currentView: null,
  selectedAppointmentId: null,

  init() {
    document.getElementById("login-form").addEventListener("submit", (e) => {
      e.preventDefault();
      this.doLogin();
    });
    document.getElementById("logout-btn").addEventListener("click", () => this.doLogout());
    document.getElementById("topbar-back").addEventListener("click", () => this.backToList());

    if (Api.token) {
      this.loadProfile();
    } else {
      this.showLogin();
    }
  },

  showLogin() {
    document.getElementById("login-page").classList.remove("hidden");
    document.getElementById("app-layout").classList.add("hidden");
  },

  showApp() {
    document.getElementById("login-page").classList.add("hidden");
    document.getElementById("app-layout").classList.remove("hidden");
    this.setupNav();
  },

  async doLogin() {
    hideError("login-error");
    const login = document.getElementById("login-input").value.trim();
    const password = document.getElementById("password-input").value;
    try {
      const data = await Api.login(login, password);
      Api.setToken(data.access_token);
      await this.loadProfile();
    } catch (err) {
      showError("login-error", err.message);
    }
  },

  doLogout() {
    Api.clearToken();
    this.profile = null;
    this.showLogin();
  },

  async loadProfile() {
    try {
      this.profile = await Api.getProfile();
      localStorage.setItem("medconnect_role", this.profile.role);
      document.getElementById("user-name").textContent = this.profile.full_name;
      document.getElementById("user-role").textContent = this.roleLabel(this.profile.role);

      if (this.profile.role === "patient") {
        this.showPatientNotice();
        return;
      }

      this.showApp();
      const startView = this.profile.role === "admin" ? "admin-appointments" : "doctor-appointments";
      this.navigate(startView);
    } catch (err) {
      Api.clearToken();
      this.showLogin();
      if (err.message && !err.message.includes("истекла")) {
        showError("login-error", err.message);
      }
    }
  },

  showPatientNotice() {
    document.getElementById("login-page").classList.remove("hidden");
    document.getElementById("app-layout").classList.add("hidden");
    Api.clearToken();
    showError(
      "login-error",
      "Аккаунт пациента. Войдите через Android-приложение «МедКоннект». " +
      "Эта панель только для врачей и администраторов.",
    );
  },

  roleLabel(role) {
    return { doctor: "Врач", admin: "Администратор", manager: "Менеджер" }[role] || role;
  },

  setupNav() {
    const nav = document.getElementById("sidebar-nav");
    nav.innerHTML = "";
    const role = this.profile.role;

    if (role === "doctor") {
      this.addNavItem(nav, "Мои записи", "doctor-appointments");
    }
    if (role === "admin") {
      this.addNavItem(nav, "Все заявки", "admin-appointments");
      this.addNavItem(nav, "Расписание", "admin-schedule");
    }
    if (role === "admin" || role === "manager") {
      this.addNavItem(nav, "Отчёты", "reports");
    }
  },

  addNavItem(nav, label, view) {
    const btn = document.createElement("button");
    btn.className = "nav-item";
    btn.textContent = label;
    btn.dataset.view = view;
    btn.addEventListener("click", () => this.navigate(view));
    nav.appendChild(btn);
  },

  navigate(view) {
    this.currentView = view;
    document.querySelectorAll(".nav-item").forEach((el) => {
      el.classList.toggle("active", el.dataset.view === view);
    });
    document.getElementById("detail-view").classList.add("hidden");
    document.getElementById("list-view").classList.remove("hidden");
    document.getElementById("topbar-back").classList.add("hidden");

    document.getElementById("appointments-section").classList.toggle("hidden", !view.includes("appointments"));
    document.getElementById("schedule-section").classList.toggle("hidden", view !== "admin-schedule");
    document.getElementById("reports-section").classList.toggle("hidden", view !== "reports");

    const titles = {
      "doctor-appointments": "Мои записи",
      "admin-appointments": "Все заявки",
      "admin-schedule": "Расписание",
      reports: "Статистика",
    };
    document.getElementById("page-title").textContent = titles[view] || "МедКоннект";

    if (view === "doctor-appointments") DoctorView.loadAppointments();
    else if (view === "admin-appointments") AdminView.loadAppointments();
    else if (view === "admin-schedule") AdminView.loadSchedulePage();
    else if (view === "reports") AdminView.loadReports();
  },

  renderAppointmentCards(list, mode) {
    const cards = document.getElementById("cards-list");
    if (!list.length) {
      cards.innerHTML = `<div class="card"><p>Записей нет</p></div>`;
      return;
    }
    cards.innerHTML = list.map((a) => `
      <div class="appointment-card" data-id="${a.appointment_id}">
        <div class="row-top">
          <span class="name">${a.patient_name}</span>
          <span class="status ${statusClass(a.status)}">${statusLabel(a.status)}</span>
        </div>
        <div class="meta">${mode === "admin" ? a.doctor_name + " · " : ""}${fmtDate(a.start_time)}</div>
        <div class="meta">${a.complaint.substring(0, 60)}${a.complaint.length > 60 ? "..." : ""}</div>
      </div>
    `).join("");
    cards.querySelectorAll(".appointment-card").forEach((el) => {
      el.addEventListener("click", () => App.openAppointment(+el.dataset.id));
    });
  },

  async openAppointment(id) {
    this.selectedAppointmentId = id;
    document.getElementById("list-view").classList.add("hidden");
    document.getElementById("detail-view").classList.remove("hidden");
    document.getElementById("page-title").textContent = `Обращение №${id}`;
    document.getElementById("topbar-back").classList.remove("hidden");

    const container = document.getElementById("detail-content");
    container.innerHTML = "<p>Загрузка...</p>";

    try {
      const appt = await Api.getAppointment(id);
      const isDoctor = this.profile.role === "doctor";
      container.innerHTML = this.renderDetail(appt, isDoctor);
      if (isDoctor && appt.consultation) {
        DoctorView.setupChat(appt);
        DoctorView.setupResultForm(appt);
      }
      if (this.profile.role === "admin") {
        AdminView.setupStatusForm(appt);
      }
    } catch (err) {
      container.innerHTML = `<div class="error-msg">${err.message}</div>`;
    }
  },

  renderDetail(appt, isDoctor) {
    const files = appt.files?.length
      ? appt.files.map((f) => `<li>${f.file_name} (${f.file_type})</li>`).join("")
      : "<li>Нет файлов</li>";

    const consultation = appt.consultation;
    let resultHtml = "";
    if (consultation?.result_text) {
      resultHtml = `
        <div class="card">
          <h3>Заключение</h3>
          <p><strong>Оценка:</strong> ${consultation.preliminary_assessment || "—"}</p>
          <p><strong>Результат:</strong> ${consultation.result_text}</p>
          <p><strong>Рекомендации:</strong> ${consultation.recommendation || "—"}</p>
          ${consultation.needs_in_person ? "<p>⚠ Рекомендован очный приём</p>" : ""}
        </div>`;
    }

    let doctorPanel = "";
    if (isDoctor && consultation) {
      doctorPanel = `
        <div class="card">
          <h3>Чат консультации</h3>
          <div id="chat-box" class="chat-box"></div>
          <div class="chat-input-row">
            <input id="chat-input" placeholder="Сообщение пациенту..." />
            <button class="btn btn-primary" id="chat-send-btn">Отправить</button>
          </div>
        </div>
        <div class="card">
          <h3>Заполнить заключение</h3>
          <div id="result-error" class="error-msg hidden"></div>
          <div id="result-success" class="success-msg hidden"></div>
          <div class="form-group">
            <label>Предварительная оценка</label>
            <textarea id="result-assessment">${consultation.preliminary_assessment || ""}</textarea>
          </div>
          <div class="form-group">
            <label>Результат консультации *</label>
            <textarea id="result-text">${consultation.result_text || ""}</textarea>
          </div>
          <div class="form-group">
            <label>Рекомендации *</label>
            <textarea id="result-recommendation">${consultation.recommendation || ""}</textarea>
          </div>
          <div class="form-group">
            <label><input type="checkbox" id="result-in-person" ${consultation.needs_in_person ? "checked" : ""} /> Необходим очный приём</label>
          </div>
          <button class="btn btn-primary" id="save-result-btn">Сохранить заключение</button>
        </div>`;
    }

    let adminPanel = "";
    if (this.profile.role === "admin") {
      adminPanel = `
        <div class="card">
          <h3>Изменить статус</h3>
          <div id="status-error" class="error-msg hidden"></div>
          <div class="form-group">
            <select id="status-select">
              ${["created","confirmed","rescheduled","cancelled","completed"].map((s) =>
                `<option value="${s}" ${appt.status === s ? "selected" : ""}>${statusLabel(s)}</option>`
              ).join("")}
            </select>
          </div>
          <button class="btn btn-outline" id="save-status-btn">Обновить статус</button>
        </div>`;
    }

    return `
      <div class="detail-grid">
        <div class="card">
          <h3>Информация о записи</h3>
          <div class="info-row"><div class="label">Врач</div><div class="value">${appt.doctor_name} (${appt.specialization})</div></div>
          <div class="info-row"><div class="label">Дата</div><div class="value">${fmtDate(appt.start_time)}</div></div>
          <div class="info-row"><div class="label">Статус</div><div class="value"><span class="status ${statusClass(appt.status)}">${statusLabel(appt.status)}</span></div></div>
          <div class="info-row"><div class="label">Жалобы</div><div class="value">${appt.complaint}</div></div>
          ${appt.symptoms ? `<div class="info-row"><div class="label">Симптомы</div><div class="value">${appt.symptoms}</div></div>` : ""}
          ${appt.comment ? `<div class="info-row"><div class="label">Комментарий</div><div class="value">${appt.comment}</div></div>` : ""}
          <div class="info-row"><div class="label">Файлы</div><ul>${files}</ul></div>
        </div>
        ${resultHtml}
        ${doctorPanel}
        ${adminPanel}
      </div>`;
  },

  backToList() {
    DoctorView.stopChatPoll();
    document.getElementById("detail-view").classList.add("hidden");
    document.getElementById("list-view").classList.remove("hidden");
    document.getElementById("topbar-back").classList.add("hidden");
    this.navigate(this.currentView);
  },
};

const DoctorView = {
  _chatPollInterval: null,
  _chatLastCount: 0,
  _chatAppointmentId: null,

  stopChatPoll() {
    if (this._chatPollInterval) {
      clearInterval(this._chatPollInterval);
      this._chatPollInterval = null;
    }
    this._chatAppointmentId = null;
  },

  renderMessages(messages) {
    const box = document.getElementById("chat-box");
    if (!box) return;
    const myId = App.profile.user_id;
    const atBottom = box.scrollHeight - box.scrollTop - box.clientHeight < 60;
    box.innerHTML = messages.map((m) => {
      const out = m.sender_id === myId;
      return `<div class="chat-msg ${out ? "outgoing" : "incoming"}">
        ${!out ? `<div class="sender">${m.sender_name}</div>` : ""}
        <div>${m.text}</div>
        <div class="time">${fmtDate(m.created_at)}</div>
      </div>`;
    }).join("") || "<p style='color:#5a6472;font-size:0.9rem'>Нет сообщений</p>";
    if (atBottom || messages.length === 0) box.scrollTop = box.scrollHeight;
  },

  async loadAppointments() {
    const tbody = document.getElementById("appointments-table-body");
    tbody.innerHTML = "<tr><td colspan='6'>Загрузка...</td></tr>";
    document.getElementById("cards-list").innerHTML = "<div class='card'>Загрузка...</div>";
    try {
      const list = await Api.getDoctorAppointments();
      App.renderAppointmentCards(list, "doctor");
      if (!list.length) {
        tbody.innerHTML = "<tr><td colspan='6'>Записей нет</td></tr>";
        return;
      }
      tbody.innerHTML = list.map((a) => `
        <tr data-id="${a.appointment_id}">
          <td>${a.patient_name}</td>
          <td>${a.patient_phone}</td>
          <td>${fmtDate(a.start_time)}</td>
          <td>${a.complaint.substring(0, 50)}${a.complaint.length > 50 ? "..." : ""}</td>
          <td><span class="status ${statusClass(a.status)}">${statusLabel(a.status)}</span></td>
          <td><button class="btn btn-primary btn-sm">Открыть</button></td>
        </tr>
      `).join("");
      tbody.querySelectorAll("tr").forEach((row) => {
        row.addEventListener("click", () => App.openAppointment(+row.dataset.id));
      });
    } catch (err) {
      tbody.innerHTML = `<tr><td colspan='6' class='error-msg'>${err.message}</td></tr>`;
      document.getElementById("cards-list").innerHTML = `<div class="error-msg">${err.message}</div>`;
    }
  },

  setupChat(appt) {
    this.stopChatPoll();
    this._chatAppointmentId = appt.appointment_id;
    this._chatLastCount = (appt.consultation?.messages || []).length;
    this.renderMessages(appt.consultation?.messages || []);

    // Автообновление: каждые 4 секунды проверяем новые сообщения
    this._chatPollInterval = setInterval(async () => {
      if (!document.getElementById("chat-box")) { this.stopChatPoll(); return; }
      try {
        const fresh = await Api.getConsultation(this._chatAppointmentId);
        if (fresh.messages.length > this._chatLastCount) {
          this._chatLastCount = fresh.messages.length;
          this.renderMessages(fresh.messages);
        }
      } catch (_) {}
    }, 4000);

    document.getElementById("chat-send-btn").addEventListener("click", async () => {
      const input = document.getElementById("chat-input");
      const text = input.value.trim();
      if (!text) return;
      const btn = document.getElementById("chat-send-btn");
      btn.disabled = true;
      try {
        const msg = await Api.sendMessage(appt.consultation.consultation_id, text);
        input.value = "";
        this._chatLastCount++;
        // Добавляем сообщение сразу без перезагрузки страницы
        const box = document.getElementById("chat-box");
        const placeholder = box.querySelector("p");
        if (placeholder) box.innerHTML = "";
        box.insertAdjacentHTML("beforeend", `
          <div class="chat-msg outgoing">
            <div>${msg.text}</div>
            <div class="time">${fmtDate(msg.created_at)}</div>
          </div>`);
        box.scrollTop = box.scrollHeight;
      } catch (err) {
        alert(err.message);
      } finally {
        btn.disabled = false;
      }
    });
  },

  setupResultForm(appt) {
    document.getElementById("save-result-btn").addEventListener("click", async () => {
      hideError("result-error");
      const assessment = document.getElementById("result-assessment").value;
      const resultText = document.getElementById("result-text").value.trim();
      const recommendation = document.getElementById("result-recommendation").value.trim();
      const needsInPerson = document.getElementById("result-in-person").checked;

      if (!resultText || !recommendation) {
        showError("result-error", "Заполните результат и рекомендации");
        return;
      }
      try {
        await Api.saveResult(appt.consultation.consultation_id, {
          preliminary_assessment: assessment || null,
          result_text: resultText,
          recommendation,
          needs_in_person: needsInPerson,
        });
        showSuccess("result-success", "Заключение сохранено. Пациент получит уведомление.");
      } catch (err) {
        showError("result-error", err.message);
      }
    });
  },
};

const AdminView = {
  doctors: [],

  async loadAppointments() {
    const tbody = document.getElementById("appointments-table-body");
    tbody.innerHTML = "<tr><td colspan='7'>Загрузка...</td></tr>";
    document.getElementById("cards-list").innerHTML = "<div class='card'>Загрузка...</div>";
    try {
      const list = await Api.getAdminAppointments();
      App.renderAppointmentCards(list, "admin");
      if (!list.length) {
        tbody.innerHTML = "<tr><td colspan='7'>Заявок нет</td></tr>";
        return;
      }
      tbody.innerHTML = list.map((a) => `
        <tr data-id="${a.appointment_id}">
          <td>${a.patient_name}</td>
          <td>${a.doctor_name}</td>
          <td>${fmtDate(a.start_time)}</td>
          <td>${a.complaint.substring(0, 40)}...</td>
          <td><span class="status ${statusClass(a.status)}">${statusLabel(a.status)}</span></td>
          <td>${a.patient_phone}</td>
          <td><button class="btn btn-primary btn-sm">Открыть</button></td>
        </tr>
      `).join("");
      tbody.querySelectorAll("tr").forEach((row) => {
        row.addEventListener("click", () => App.openAppointment(+row.dataset.id));
      });
    } catch (err) {
      tbody.innerHTML = `<tr><td colspan='7' class='error-msg'>${err.message}</td></tr>`;
    }
  },

  setupStatusForm(appt) {
    document.getElementById("save-status-btn").addEventListener("click", async () => {
      hideError("status-error");
      const status = document.getElementById("status-select").value;
      try {
        await Api.updateAppointmentStatus(appt.appointment_id, status);
        showSuccess("status-error", "Статус обновлён");
        document.getElementById("status-error").classList.remove("error-msg");
        document.getElementById("status-error").classList.add("success-msg");
      } catch (err) {
        showError("status-error", err.message);
      }
    });
  },

  async loadSchedulePage() {
    const container = document.getElementById("schedule-section");
    container.innerHTML = "<p>Загрузка...</p>";
    try {
      this.doctors = await Api.getDoctors();
      container.innerHTML = `
        <div class="card">
          <h3>Создать слот</h3>
          <div id="slot-error" class="error-msg hidden"></div>
          <div id="slot-success" class="success-msg hidden"></div>
          <div class="grid-2">
            <div class="form-group">
              <label>Врач</label>
              <select id="slot-doctor">
                ${this.doctors.map((d) => `<option value="${d.doctor_id}">${d.full_name} (${d.specialization})</option>`).join("")}
              </select>
            </div>
            <div class="form-group">
              <label>Начало (ISO: 2026-06-20T10:00:00)</label>
              <input id="slot-start" placeholder="2026-06-20T10:00:00" />
            </div>
            <div class="form-group">
              <label>Окончание</label>
              <input id="slot-end" placeholder="2026-06-20T10:30:00" />
            </div>
            <div class="form-group" style="display:flex;align-items:flex-end">
              <button class="btn btn-primary" id="create-slot-btn">Создать слот</button>
            </div>
          </div>
        </div>
        <div class="card">
          <h3>Расписание врача</h3>
          <div class="form-group">
            <select id="schedule-doctor-select">
              ${this.doctors.map((d) => `<option value="${d.doctor_id}">${d.full_name}</option>`).join("")}
            </select>
          </div>
          <table>
            <thead><tr><th>Начало</th><th>Окончание</th><th>Статус</th><th>Действие</th></tr></thead>
            <tbody id="slots-table-body"></tbody>
          </table>
        </div>`;

      document.getElementById("create-slot-btn").addEventListener("click", () => this.createSlot());
      document.getElementById("schedule-doctor-select").addEventListener("change", (e) => {
        this.loadSlots(+e.target.value);
      });
      this.loadSlots(this.doctors[0]?.doctor_id);
    } catch (err) {
      container.innerHTML = `<div class="error-msg">${err.message}</div>`;
    }
  },

  async createSlot() {
    hideError("slot-error");
    const doctorId = +document.getElementById("slot-doctor").value;
    const start = document.getElementById("slot-start").value;
    const end = document.getElementById("slot-end").value;
    try {
      await Api.createSlot({
        doctor_id: doctorId,
        start_time: start,
        end_time: end,
      });
      showSuccess("slot-success", "Слот создан");
      this.loadSlots(doctorId);
    } catch (err) {
      showError("slot-error", err.message);
    }
  },

  async loadSlots(doctorId) {
    const tbody = document.getElementById("slots-table-body");
    tbody.innerHTML = "<tr><td colspan='4'>Загрузка...</td></tr>";
    try {
      const slots = await Api.getAdminSchedule(doctorId);
      if (!slots.length) {
        tbody.innerHTML = "<tr><td colspan='4'>Слотов нет</td></tr>";
        return;
      }
      tbody.innerHTML = slots.map((s) => `
        <tr>
          <td>${fmtDate(s.start_time)}</td>
          <td>${fmtDate(s.end_time)}</td>
          <td><span class="status ${statusClass(s.status)}">${statusLabel(s.status)}</span></td>
          <td>
            ${s.status === "free" ? `<button class="btn btn-danger btn-sm" data-block="${s.slot_id}">Заблокировать</button>` : ""}
            ${s.status === "blocked" ? `<button class="btn btn-outline btn-sm" data-free="${s.slot_id}">Разблокировать</button>` : ""}
          </td>
        </tr>
      `).join("");
      tbody.querySelectorAll("[data-block]").forEach((btn) => {
        btn.addEventListener("click", async (e) => {
          e.stopPropagation();
          await Api.updateSlotStatus(+btn.dataset.block, "blocked");
          this.loadSlots(doctorId);
        });
      });
      tbody.querySelectorAll("[data-free]").forEach((btn) => {
        btn.addEventListener("click", async (e) => {
          e.stopPropagation();
          await Api.updateSlotStatus(+btn.dataset.free, "free");
          this.loadSlots(doctorId);
        });
      });
    } catch (err) {
      tbody.innerHTML = `<tr><td colspan='4' class='error-msg'>${err.message}</td></tr>`;
    }
  },

  async loadReports() {
    const container = document.getElementById("reports-section");
    container.innerHTML = "<p>Загрузка...</p>";
    try {
      const reports = await Api.getReports();
      container.innerHTML = reports.map((r) => `
        <div class="card">
          <h3>Период: ${r.period}</h3>
          <p><strong>Завершённых консультаций:</strong> ${r.consultation_count}</p>
          <p><strong>Отмен:</strong> ${r.no_show_count}</p>
          ${r.doctor_load ? `<p><strong>Загрузка врачей:</strong> ${r.doctor_load}</p>` : ""}
        </div>
      `).join("");
    } catch (err) {
      container.innerHTML = `<div class="error-msg">${err.message}</div>`;
    }
  },
};

document.addEventListener("DOMContentLoaded", () => App.init());
