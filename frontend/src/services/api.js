import axios from 'axios';
const api = axios.create({ baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080/api', withCredentials: true, withXSRFToken: true, xsrfCookieName: 'XSRF-TOKEN', xsrfHeaderName: 'X-XSRF-TOKEN', headers: { 'Content-Type': 'application/json' } });
// Public registration pages can be submitted before AuthProvider's initial CSRF request finishes.
// Fetch a token before every state-changing request; safe methods do not need a CSRF token.
api.interceptors.request.use(async config => {
  const method = (config.method || 'get').toLowerCase();
  if (!['get', 'head', 'options'].includes(method) && !config.url?.endsWith('/auth/csrf')) {
    await api.get('/auth/csrf');
  }
  return config;
});
api.interceptors.response.use(response=>response,error=>{if(error.response?.status===401&&!['/auth/login','/auth/register','/auth/verify-email','/auth/resend-verification','/auth/forgot-password','/auth/reset-password'].some(path=>error.config?.url?.includes(path))){window.dispatchEvent(new Event('carevia:session-expired'));}return Promise.reject(error);});
export default api;
