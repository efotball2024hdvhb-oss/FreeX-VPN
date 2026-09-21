import json
import os
import smtplib
from email.message import EmailMessage
from http.server import BaseHTTPRequestHandler, HTTPServer

HOST = os.getenv("HOST", "127.0.0.1")
PORT = int(os.getenv("PORT", "8080"))
API_TOKEN = os.getenv("API_TOKEN", "")
SMTP_HOST = os.getenv("SMTP_HOST", "")
SMTP_PORT = int(os.getenv("SMTP_PORT", "587"))
SMTP_USER = os.getenv("SMTP_USER", "")
SMTP_PASSWORD = os.getenv("SMTP_PASSWORD", "")
SMTP_FROM = os.getenv("SMTP_FROM", SMTP_USER)

class Handler(BaseHTTPRequestHandler):
    def send_json(self, status, payload):
        raw = json.dumps(payload).encode()
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(raw)))
        self.end_headers()
        self.wfile.write(raw)

    def do_POST(self):
        if self.path != "/v1/forward":
            self.send_json(404, {"error": "not_found"})
            return

        if API_TOKEN and self.headers.get("Authorization") != f"Bearer {API_TOKEN}":
            self.send_json(401, {"error": "unauthorized"})
            return

        length = int(self.headers.get("Content-Length", "0"))
        try:
            data = json.loads(self.rfile.read(length))
            destination = str(data["to"]).strip()
            messages = data["messages"]
            if not destination or not isinstance(messages, list) or not messages:
                raise ValueError("invalid_payload")
        except Exception:
            self.send_json(400, {"error": "invalid_payload"})
            return

        body = "\n\n".join(
            f"From: {m.get('sender', 'Unknown')}\n{m.get('body', '')}"
            for m in messages
        )

        try:
            if not all([SMTP_HOST, SMTP_USER, SMTP_PASSWORD, SMTP_FROM]):
                raise RuntimeError("SMTP is not configured")

            msg = EmailMessage()
            msg["Subject"] = f"SMS Forwarder — {len(messages)} new message(s)"
            msg["From"] = SMTP_FROM
            msg["To"] = destination
            msg.set_content(body)

            with smtplib.SMTP(SMTP_HOST, SMTP_PORT, timeout=15) as smtp:
                smtp.starttls()
                smtp.login(SMTP_USER, SMTP_PASSWORD)
                smtp.send_message(msg)

            self.send_json(200, {"ok": True})
        except Exception as exc:
            print(f"mail error: {exc}")
            self.send_json(502, {"error": "mail_delivery_failed"})

    def log_message(self, *_):
        pass

if __name__ == "__main__":
    if not API_TOKEN:
        print("WARNING: API_TOKEN is empty; configure it before exposing this server.")
    HTTPServer((HOST, PORT), Handler).serve_forever()
