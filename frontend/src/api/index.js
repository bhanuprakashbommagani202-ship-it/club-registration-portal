import api from "./client";

const data = (res) => res.data;

/** Drops empty values so they aren't sent as ?category=&q= */
const clean = (params = {}) =>
  Object.fromEntries(Object.entries(params).filter(([, v]) => v !== "" && v !== null && v !== undefined && v !== false));

export const authApi = {
  login: (body) => api.post("/auth/login", body).then(data),
  register: (body) => api.post("/auth/register", body).then(data),
  me: () => api.get("/auth/me").then(data),
};

export const clubApi = {
  list: (params) => api.get("/clubs", { params: clean(params) }).then(data),
  get: (id) => api.get(`/clubs/${id}`).then(data),
  create: (body) => api.post("/clubs", body).then(data),
  update: (id, body) => api.put(`/clubs/${id}`, body).then(data),
  apply: (id, motivation) => api.post(`/clubs/${id}/apply`, { motivation }).then(data),
  members: (id, status) => api.get(`/clubs/${id}/members`, { params: clean({ status }) }).then(data),
};

export const membershipApi = {
  mine: () => api.get("/memberships/my").then(data),
  pending: () => api.get("/memberships/pending").then(data),
  decide: (id, status, remark) => api.patch(`/memberships/${id}/status`, { status, remark }).then(data),
  leave: (id) => api.post(`/memberships/${id}/leave`).then(data),
};

export const eventApi = {
  list: (params) => api.get("/events", { params: clean(params) }).then(data),
  create: (body) => api.post("/events", body).then(data),
  cancel: (id) => api.post(`/events/${id}/cancel`).then(data),
  register: (id) => api.post(`/events/${id}/register`).then(data),
  unregister: (id) => api.delete(`/events/${id}/register`).then(data),
  attendees: (id) => api.get(`/events/${id}/attendees`).then(data),
};

export const userApi = {
  list: (role) => api.get("/users", { params: clean({ role }) }).then(data),
  create: (body) => api.post("/users", body).then(data),
  setActive: (id, active) => api.patch(`/users/${id}/status`, { active }).then(data),
};

export const dashboardApi = {
  get: () => api.get("/dashboard").then(data),
};
