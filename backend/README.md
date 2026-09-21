# SMS Forwarder backend

The Android app intentionally does not contain SMTP credentials.

Set these environment variables on the server:

```bash
export SMTP_HOST="smtp.gmail.com"
export SMTP_PORT="587"
export SMTP_USER="your-sender@gmail.com"
export SMTP_PASSWORD="your-app-password"
export DEFAULT_TO="efotball2024hdvhb@gmail.com"

# Optional:
export API_TOKEN="change-this-to-a-long-random-token"
```

Run:

```bash
python3 backend/server.py
```

The server exposes:

- `GET /health`
- `POST /v1/forward`

For production, put the Python server behind HTTPS using a reverse proxy. Do not expose SMTP passwords in the Android APK.

If `API_TOKEN` is enabled, the Android client must also be updated to send:
`Authorization: Bearer <API_TOKEN>`.
