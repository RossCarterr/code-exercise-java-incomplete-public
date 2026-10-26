import { useUrlShortener } from './hooks/useUrlShortener';
import { ShortenForm } from './components/ShortenForm';
import { ResultBanner } from './components/ResultBanner';
import { UrlTable } from './components/UrlTable';

export default function App() {
  const {
    urls,
    loading,
    error,
    lastCreated,
    page,
    pageSize,
    hasNextPage,
    shorten,
    deleteUrl,
    clearLastCreated,
    previousPage,
    nextPage,
  } = useUrlShortener();

  return (
    <div className="app">
      <header className="app-header">
        <div className="app-header__inner">
          <div className="logo">
            <span className="logo__mark" aria-hidden="true">⌁</span>
            <span className="logo__name">Snip</span>
          </div>
          <p className="tagline">Long URLs, made short.</p>
        </div>
      </header>

      <main className="app-main">
        <section className="card shorten-card" aria-labelledby="shorten-heading">
          <h2 id="shorten-heading" className="card__title">
            Shorten a URL
          </h2>
          <ShortenForm onSubmit={shorten} />
        </section>

        {lastCreated && (
          <ResultBanner result={lastCreated} onDismiss={clearLastCreated} />
        )}

        {error && (
          <div className="banner banner--error" role="alert">
            <strong>Error:</strong> {error}
          </div>
        )}

        <section className="card" aria-labelledby="list-heading">
          <h2 id="list-heading" className="card__title">
            All shortened URLs
            {urls.length > 0 && (
              <span className="badge">{urls.length}</span>
            )}
          </h2>

          {loading ? (
            <div className="loading" aria-live="polite" aria-busy="true">
              Loading…
            </div>
          ) : (
            <UrlTable
              urls={urls}
              page={page}
              pageSize={pageSize}
              hasNextPage={hasNextPage}
              isLoading={loading}
              onDelete={deleteUrl}
              onPreviousPage={previousPage}
              onNextPage={nextPage}
            />
          )}
        </section>
      </main>

      <footer className="app-footer">
        <p>Snip URL Shortener</p>
      </footer>
    </div>
  );
}
