"""Faux serveur API pour tester le Programme 3 (Uploader).
Accepte POST /api/tracks/upload et repond 200 OK.
Gere le corps en Transfer-Encoding: chunked (envoye par le client multipart).
"""
from http.server import BaseHTTPRequestHandler, HTTPServer


class Handler(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"

    def _drain_body(self) -> int:
        te = self.headers.get("Transfer-Encoding", "").lower()
        if "chunked" in te:
            total = 0
            while True:
                line = self.rfile.readline().strip()
                if not line:
                    continue
                size = int(line.split(b";")[0], 16)
                if size == 0:
                    self.rfile.readline()  # CRLF final
                    break
                self.rfile.read(size)
                self.rfile.readline()  # CRLF apres le chunk
                total += size
            return total
        length = int(self.headers.get("Content-Length", 0))
        self.rfile.read(length)
        return length

    def do_POST(self):
        received = self._drain_body()
        print(f"[mock-api] POST {self.path} ({received} octets) -> 200 OK", flush=True)
        self.send_response(200)
        self.send_header("Content-Type", "text/plain")
        self.send_header("Content-Length", "2")
        self.send_header("Connection", "close")
        self.end_headers()
        self.wfile.write(b"OK")

    def log_message(self, *args):
        pass


if __name__ == "__main__":
    HTTPServer(("0.0.0.0", 8080), Handler).serve_forever()
