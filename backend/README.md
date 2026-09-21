# SMS Forwarder Backend

A small Python standard-library HTTPS-front-end boundary for email delivery.

## Environment

Set these variables on the server:

- `API_TOKEN` — shared bearer token if you add authentication at your reverse proxy/app boundary.
- `SMTP_HOST`
- `SMTP_PORT` (usually 587)
- `SMTP_USER`
- `SMTP_PASSWORD`
- `SMTP_FROM`
- `HOST`
- `PORT`

Do not commit real SMTP passwords or API tokens to GitHub.

## Endpoint

`POST /v1/forward`

JSON:
```json
{
  "to": "destination@example.com",
  "messages": [
    {
      "sender": "+123456789",
      "body": "Example SMS",
      "timestamp": 1760000000000
    }
  ]
}
```

For production, put the service behind HTTPS (for example, a reverse proxy with a valid TLS certificate) and add strong authentication/rate limiting. The Android app should never contain SMTP credentials.
