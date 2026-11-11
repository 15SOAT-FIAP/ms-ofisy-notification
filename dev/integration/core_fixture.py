"""Minimal Core contract for the standalone notification Compose environment."""
import json
import re
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer


class CoreFixture(BaseHTTPRequestHandler):
    def do_GET(self):
        if not re.fullmatch(r"/api/v1/service-orders/[0-9a-f-]{36}/customer", self.path):
            self.send_error(404)
            return
        if self.headers.get("Authorization") != "Bearer local-fixture-only":
            self.send_error(401)
            return
        body = json.dumps({"customerId": "00000000-0000-4000-8000-000000000001"}).encode()
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)


ThreadingHTTPServer(("0.0.0.0", 8080), CoreFixture).serve_forever()
