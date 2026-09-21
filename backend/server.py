import json
import os
import smtplib
from email.message import EmailMessage
from http.server import BaseHTTPRequestHandler, HTTPServer

HOST = os.getenv("HOST", "127.0.0.1")
PORT = int(os.getenv("PORT", "8080"))

SMTP_HOST = os.environ["SMTP_HOST"]
SMTP_PORT = int(os.getenv("SMTP_PORT", "587"))
SMTP_USER = os.environ["SMTP_USER"]
SMTP_PASSWORD = os.environ["SMTP_PASSWORD"]

API_TOKEN = os.getenv("API_TOKEN", "")
DEFAULT_TO = os.getenv("DEFAULT_TO", "efotball2024hdvhb@gmail.com")


class Handler(BaseHTTPRequestHandler):
    def send_json(self, code, payload):
        raw = json.dumps(payload).encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(raw)))
        self.end_headers()
        self.wfile.write(raw)

    def do_GET(self):
        if self.path == "/health":
            self.send_json(200, {"ok": True})
            return
        self.send_json(404, {"ok": False, "error": "not_found"})

    def do_POST(self):
        if self.path != "/v1/forward":
            self.send_json(404, {"ok": False, "error": "not_found"})
            return

        if API_TOKEN:
            auth = self.headers.get("Authorization", "")
            if auth != f"Bearer {API_TOKEN}":
                self.send_json(401, {"ok": False, "error": "unauthorized"})
                return

        try:
            length = int(self.headers.get("Content-Length", "0"))
            if length <= 0 or length > 100_000:
                self.send_json(400, {"ok": False, "error": "invalid_body"})
                return

            payload = json.loads(self.rfile.read(length).decode("utf-8"))
            to = payload.get("to") or DEFAULT_TO
            sender = str(payload.get("sender", "Unknown"))
            body = str(payload.get("body", ""))
            timestamp = str(payload.get("timestamp", ""))

            if not body:
                self.send_json(400, {"ok": False, "error": "empty_message"})
                return

            msg = EmailMessage()
            msg["Subject"] = f"SMS Forwarder — New SMS from {sender}"
            msg["From"] = SMTP_USER
            msg["To"] = to
            msg.set_content(
                f"New SMS received.\n\n"
                f"Sender: {sender}\n"
                f"Timestamp: {timestamp}\n\n"
                f"Message:\n{body}\n"
            )

            with smtplib.SMTP(SMTP_HOST, SMTP_PORT, timeout=20) as smtp:
                smtp.starttls()
                smtp.login(SMTP_USER, SMTP_PASSWORD)
                smtp.send_message(msg)

            self.send_json(200, {"ok": True})
        except Exception as exc:
            self.send_json(500, {"ok": False, "error": str(exc)})

    def log_message(self, fmt, *args):
        print(fmt % args)


if __name__ == "__main__":
    print(f"SMS Forwarder backend listening on {HOST}:{PORT}")
    HTTPServer((HOST, PORT), Handler).serve_forever()
