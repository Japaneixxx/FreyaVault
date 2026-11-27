// Proxy server-side da API do Pluggy (Vercel Function).
// O navegador chama /api/pluggy/<rota> e este arquivo repassa para https://api.pluggy.ai/<rota>,
// evitando CORS. Nada é armazenado: as credenciais só passam em trânsito.
const UPSTREAM = process.env.PLUGGY_API_URL || "https://api.pluggy.ai";
const PREFIX = "/api/pluggy/";

async function readRawBody(req) {
  if (req.body !== undefined && req.body !== null) {
    if (typeof req.body === "string") return req.body;
    if (Buffer.isBuffer(req.body)) return req.body.toString("utf8");
    return JSON.stringify(req.body);
  }
  const chunks = [];
  for await (const chunk of req) chunks.push(chunk);
  return chunks.length ? Buffer.concat(chunks).toString("utf8") : "";
}

export default async function handler(req, res) {
  // Sempre derivamos rota e query da URL original. A Vercel pode anexar "?path=..." (parâmetro
  // da rota dinâmica) à URL; se isso fosse repassado ao Pluggy, a chamada seria recusada.
  const incoming = new URL(req.url, "http://localhost");
  let path = incoming.pathname.startsWith(PREFIX)
    ? incoming.pathname.slice(PREFIX.length)
    : "";
  if (!path) {
    const q = req.query && req.query.path;
    path = Array.isArray(q) ? q.join("/") : q || "";
  }
  path = path.replace(/^\/+/, "");
  if (!path || path.includes("..")) {
    return res.status(400).json({ message: "Rota inválida", source: "proxy" });
  }
  incoming.searchParams.delete("path");
  const qs = incoming.searchParams.toString();
  const targetUrl = `${UPSTREAM}/${path}${qs ? `?${qs}` : ""}`;

  const headers = {
    Accept: "application/json",
    "User-Agent": "FreyaVault-Web/1.0 (+https://github.com/Japaneixxx/FreyaVault)",
  };
  if (req.headers["x-api-key"]) headers["X-API-KEY"] = req.headers["x-api-key"];

  const init = { method: req.method, headers };
  if (req.method !== "GET" && req.method !== "HEAD") {
    const body = await readRawBody(req);
    if (body) {
      init.body = body;
      headers["Content-Type"] = "application/json";
    }
  }

  res.setHeader("Cache-Control", "no-store");
  res.setHeader("X-Freya-Proxy", "1");
  try {
    const upstream = await fetch(targetUrl, init);
    const text = await upstream.text();
    res.status(upstream.status);
    res.setHeader("Content-Type", upstream.headers.get("content-type") || "application/json");
    return res.send(text);
  } catch (err) {
    return res.status(502).json({ message: `Proxy não alcançou o Pluggy: ${String(err)}`, source: "proxy" });
  }
}
