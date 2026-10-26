import { describe, it, expect, vi, beforeEach } from 'vitest';
import { api } from '../api';

describe('api service', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('shorten sends POST to /shorten with correct body', async () => {
    const mockResponse = { alias: 'abc', fullUrl: 'https://ex.com', shortUrl: 'http://host/abc' };
    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 201,
      json: async () => mockResponse,
    } as Response);

    const result = await api.shorten({ fullUrl: 'https://ex.com', customAlias: 'abc' });

    expect(fetch).toHaveBeenCalledWith('/shorten', expect.objectContaining({
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ fullUrl: 'https://ex.com', customAlias: 'abc' }),
    }));
    expect(result).toEqual(mockResponse);
  });

  it('shorten throws with error message from API on failure', async () => {
    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: false,
      status: 400,
      json: async () => ({ error: 'Alias already taken.' }),
    } as Response);

    await expect(api.shorten({ fullUrl: 'https://ex.com', customAlias: 'taken' }))
      .rejects.toThrow('Alias already taken.');
  });

  it('listAll sends GET to /urls', async () => {
    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => [],
    } as Response);

    await api.listAll(0, 6);

    expect(fetch).toHaveBeenCalledWith('/urls?page=0&size=6');
  });

  it('delete sends DELETE to /{alias}', async () => {
    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 204,
      json: async () => undefined,
    } as Response);

    await api.delete('my-alias');

    expect(fetch).toHaveBeenCalledWith('/my-alias', expect.objectContaining({ method: 'DELETE' }));
  });

  it('delete encodes aliases in the URL', async () => {
    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 204,
      json: async () => undefined,
    } as Response);

    await api.delete('my alias');

    expect(fetch).toHaveBeenCalledWith('/my%20alias', expect.objectContaining({ method: 'DELETE' }));
  });

  it('delete throws on 404', async () => {
    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: false,
      status: 404,
      json: async () => ({ error: "No URL found for alias 'ghost'." }),
    } as Response);

    await expect(api.delete('ghost'))
      .rejects.toThrow("No URL found for alias 'ghost'.");
  });
});
