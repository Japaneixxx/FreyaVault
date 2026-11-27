/* Freya PWA — cache offline dos arquivos estáticos do build Wasm. */
const CACHE = "freya-web-v2";
/* Os chunks e o .wasm têm nomes com hash: são guardados pela estratégia network-first abaixo. */
const CORE = ["./", "./index.html", "./manifest.json", "./icon.svg", "./icon-maskable.svg", "./freya.js"];

self.addEventListener("install", (event) => {
    event.waitUntil(
        caches.open(CACHE)
            .then((cache) => Promise.allSettled(CORE.map((url) => cache.add(url))))
            .then(() => self.skipWaiting())
    );
});

self.addEventListener("activate", (event) => {
    event.waitUntil(
        caches.keys()
            .then((keys) => Promise.all(keys.filter((key) => key !== CACHE).map((key) => caches.delete(key))))
            .then(() => self.clients.claim())
    );
});

self.addEventListener("fetch", (event) => {
    const request = event.request;
    const url = new URL(request.url);
    if (request.method !== "GET" || url.origin !== self.location.origin) return;
    /* A API (proxy do Pluggy) nunca passa pelo cache: dados financeiros devem vir sempre da rede. */
    if (url.pathname.startsWith("/api/")) return;

    event.respondWith(
        caches.match(request).then((cached) => {
            const network = fetch(request)
                .then((response) => {
                    if (response && response.ok) {
                        const copy = response.clone();
                        caches.open(CACHE).then((cache) => cache.put(request, copy));
                    }
                    return response;
                })
                .catch(() => cached || caches.match("./index.html"));
            return cached || network;
        })
    );
});
