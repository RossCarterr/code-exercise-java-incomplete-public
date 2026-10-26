import { useState, useCallback, useEffect } from 'react';
import { api } from '../services/api';
import type { ShortenUrlResponse, UrlListItem } from '../types/api';

interface UseUrlShortenerReturn {
  urls: UrlListItem[];
  loading: boolean;
  error: string | null;
  lastCreated: ShortenUrlResponse | null;
  page: number;
  pageSize: number;
  hasNextPage: boolean;
  shorten: (fullUrl: string, customAlias?: string) => Promise<void>;
  deleteUrl: (alias: string) => Promise<void>;
  clearLastCreated: () => void;
  previousPage: () => void;
  nextPage: () => void;
}

const DEFAULT_PAGE_SIZE = 5;

export function useUrlShortener(): UseUrlShortenerReturn {
  const [urls, setUrls] = useState<UrlListItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [lastCreated, setLastCreated] = useState<ShortenUrlResponse | null>(null);
  const [page, setPage] = useState(0);
  const [hasNextPage, setHasNextPage] = useState(false);

  const fetchAll = useCallback(async (pageToLoad = page) => {
    setLoading(true);
    setError(null);
    try {
      const data = await api.listAll(pageToLoad, DEFAULT_PAGE_SIZE + 1);
      setUrls(data.slice(0, DEFAULT_PAGE_SIZE));
      setHasNextPage(data.length > DEFAULT_PAGE_SIZE);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to load URLs.');
    } finally {
      setLoading(false);
    }
  }, [page]);

  useEffect(() => {
    fetchAll(page);
  }, [fetchAll, page]);

  const shorten = useCallback(async (fullUrl: string, customAlias?: string) => {
    setError(null);
    setLastCreated(null);
    try {
      const result = await api.shorten({ fullUrl, customAlias: customAlias || undefined });
      setLastCreated(result);
      // Refresh the list
      setPage(0); // reset to page 0 when adding to the database
      await fetchAll();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to shorten URL.');
    }
  }, [fetchAll]);

  const deleteUrl = useCallback(async (alias: string) => {
    setError(null);
    try {
      await api.delete(alias);
      await fetchAll(page); // update page when deleting URL
      setUrls((prev) => prev.filter((u) => u.alias !== alias));
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to delete URL.');
    }
  }, [fetchAll, page]);

  const clearLastCreated = useCallback(() => setLastCreated(null), []);

  const previousPage = useCallback(() => {
    setPage((currentPage) => Math.max(currentPage - 1, 0));
  }, []);

  const nextPage = useCallback(() => {
    if (hasNextPage) {
      setPage((currentPage) => currentPage + 1);
    }
  }, [hasNextPage]);

  return {
    urls,
    loading,
    error,
    lastCreated,
    page,
    pageSize: DEFAULT_PAGE_SIZE,
    hasNextPage,
    shorten,
    deleteUrl,
    clearLastCreated,
    previousPage,
    nextPage,
  };
}
