import type { UrlListItem } from '../types/api';

interface UrlTableProps {
  urls: UrlListItem[];
  page: number;
  pageSize: number;
  hasNextPage: boolean;
  isLoading?: boolean;
  onDelete: (alias: string) => void;
  onPreviousPage: () => void;
  onNextPage: () => void;
}

export function UrlTable({
                           urls,
                           page,
                           pageSize,
                           hasNextPage,
                           isLoading = false,
                           onDelete,
                           onPreviousPage,
                           onNextPage,
                         }: UrlTableProps) {
  if (!isLoading && urls.length === 0 && page === 0) {
    return (
        <div className="empty-state">
          <p>No shortened URLs yet.</p>
          <span>Create your first short URL using the form above.</span>
        </div>
    );
  }

  return (
      <div>
        {urls.length === 0 ? (
            <div className="empty-state">
              <p>No URLs found on this page.</p>
            </div>
        ) : (
            <div className="url-table-wrapper">
              <table className="url-table">
                <thead>
                <tr>
                  <th scope="col">Alias</th>
                  <th scope="col">Destination</th>
                  <th scope="col">Short URL</th>
                  <th scope="col">Actions</th>
                </tr>
                </thead>

                <tbody>
                {urls.map((url) => (
                    <tr key={url.alias}>
                      <td>
                        <code>{url.alias}</code>
                      </td>

                      <td>
                        <a
                            href={url.fullUrl}>
                            {url.fullUrl}
                        </a>
                      </td>

                      <td>
                        <a
                            href={url.shortUrl}>
                            {url.shortUrl}
                        </a>
                      </td>

                      <td>
                        <div>
                          <button
                              type="button"
                              className="btn-danger"
                              onClick={() => onDelete(url.alias)}
                              aria-label={`Delete short URL ${url.alias}`}>
                            Delete
                          </button>
                        </div>
                      </td>
                    </tr>
                ))}
                </tbody>
              </table>
            </div>
        )}

        <div>
          <button
              type="button"
              className="btn-secondary"
              onClick={onPreviousPage}
              disabled={page === 0 || isLoading}>
            Previous
          </button>

          <span>
            Page {page + 1} · {pageSize} per page
          </span>

          <button
              type="button"
              className="btn-secondary"
              onClick={onNextPage}
              disabled={!hasNextPage || isLoading}>
            Next
          </button>
        </div>
      </div>
  );
}
