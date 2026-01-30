# Splitter Frontend

This directory contains the frontend web application for the Splitter expense-sharing platform.

## Structure

```
frontend/
└── web/           # Next.js web application
```

## Quick Start

1. Navigate to the web directory:
   ```bash
   cd web
   ```

2. Install dependencies:
   ```bash
   npm install
   ```

3. Copy the environment file:
   ```bash
   cp .env.example .env.local
   ```

4. Start the development server:
   ```bash
   npm run dev
   ```

5. Open [http://localhost:3000](http://localhost:3000)

## Requirements

- Node.js 18+
- npm or yarn
- Backend services running (API Gateway on port 8080)

See [web/README.md](web/README.md) for detailed documentation.
