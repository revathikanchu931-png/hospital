import { describe, expect, it, vi } from 'vitest';
import api from './api';

describe('API client CSRF preparation', () => {
  it('fetches a CSRF token before an unauthenticated registration POST', async () => {
    const getToken = vi.spyOn(api, 'get').mockResolvedValue({ data: { token: 'csrf-token' } });
    const interceptor = api.interceptors.request.handlers.find(Boolean).fulfilled;

    const config = await interceptor({ method: 'post', url: '/auth/register/doctor' });

    expect(getToken).toHaveBeenCalledWith('/auth/csrf');
    expect(config.url).toBe('/auth/register/doctor');
    getToken.mockRestore();
  });

  it('does not fetch a CSRF token for safe GET requests', async () => {
    const getToken = vi.spyOn(api, 'get').mockResolvedValue({ data: { token: 'csrf-token' } });
    const interceptor = api.interceptors.request.handlers.find(Boolean).fulfilled;

    await interceptor({ method: 'get', url: '/doctors' });

    expect(getToken).not.toHaveBeenCalled();
    getToken.mockRestore();
  });
});
