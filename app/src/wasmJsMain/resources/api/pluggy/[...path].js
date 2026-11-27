export default async function handler(req, res) {
  const pathSegments = req.query.path || [];
  const path = Array.isArray(pathSegments)
    ? pathSegments.join("/")
    : pathSegments;

  // Preserva query params (ex: ?itemId=...&from=...&to=...)
  const queryIndex = req.url.indexOf("?");
  const queryString = queryIndex !== -1 ? req.url.slice(queryIndex) : "";
  const targetUrl = `https://api.pluggy.ai/${path}${queryString}`;

  const headers = {
    Accept: "application/json",
  };
  if (req.headers["content-type"])
    headers["Content-Type"] = req.headers["content-type"];
  if (req.headers["x-api-key"]) headers["X-API-KEY"] = req.headers["x-api-key"];

  const init = {
    method: req.method,
    headers,
  };

  if (req.method !== "GET" && req.method !== "HEAD" && req.body) {
    init.body =
      typeof req.body === "string" ? req.body : JSON.stringify(req.body);
  }

  try {
    const upstream = await fetch(targetUrl, init);
    const text = await upstream.text();
    res.status(upstream.status);
    res.setHeader(
      "Content-Type",
      upstream.headers.get("content-type") || "application/json",
    );
    return res.send(text);
  } catch (err) {
    return res.status(502).json({ error: String(err) });
  }
}
