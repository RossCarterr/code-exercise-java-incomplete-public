import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { UrlTable } from '../UrlTable';
import type { UrlListItem } from '../../types/api';

const mockUrls: UrlListItem[] = [
  { alias: 'abc', fullUrl: 'https://example.com', shortUrl: 'http://localhost:8080/abc' },
  { alias: 'def', fullUrl: 'https://another.com/path', shortUrl: 'http://localhost:8080/def' },
];

const renderUrlTable = ({
                          urls = mockUrls,
                          page = 0,
                          pageSize = 5,
                          hasNextPage = false,
                          isLoading = false,
                          onDelete = vi.fn(),
                          onPreviousPage = vi.fn(),
                          onNextPage = vi.fn(),
                        } = {}) => render(
    <UrlTable
        urls={urls}
        page={page}
        pageSize={pageSize}
        hasNextPage={hasNextPage}
        isLoading={isLoading}
        onDelete={onDelete}
        onPreviousPage={onPreviousPage}
        onNextPage={onNextPage}
    />,
);

describe('UrlTable', () => {
  it('shows empty state when there are no URLs', () => {
    renderUrlTable({ urls: [] });
    expect(screen.getByText(/no shortened urls yet/i)).toBeInTheDocument();
  });

  it('renders a row for each URL', () => {
    renderUrlTable();
    expect(screen.getByText('http://localhost:8080/abc')).toBeInTheDocument();
    expect(screen.getByText('http://localhost:8080/def')).toBeInTheDocument();
  });

  it('calls onDelete with the alias when delete is clicked', () => {
    const onDelete = vi.fn();
    renderUrlTable({ onDelete });

    fireEvent.click(screen.getAllByRole('button', { name: /delete/i })[0]);

    expect(onDelete).toHaveBeenCalledWith('abc');
  });

  it('calls onPreviousPage when previous is clicked', () => {
    const onPreviousPage = vi.fn();
    renderUrlTable({ page: 1, onPreviousPage });

    fireEvent.click(screen.getByRole('button', { name: /previous/i }));

    expect(onPreviousPage).toHaveBeenCalled();
  });

  it('calls onNextPage when next is clicked', () => {
    const onNextPage = vi.fn();
    renderUrlTable({ hasNextPage: true, onNextPage });

    fireEvent.click(screen.getByRole('button', { name: /next/i }));

    expect(onNextPage).toHaveBeenCalled();
  });

  it('disables previous on the first page', () => {
    renderUrlTable({ page: 0 });

    expect(screen.getByRole('button', { name: /previous/i })).toBeDisabled();
  });

  it('disables next when there is no next page', () => {
    renderUrlTable({ hasNextPage: false });

    expect(screen.getByRole('button', { name: /next/i })).toBeDisabled();
  });
});
