# Splitter Frontend Web Application

A modern, responsive web application for the Splitter expense-sharing platform built with Next.js 14, TypeScript, and TailwindCSS.

## Features

- 🔐 **Authentication** - Secure login and registration with JWT tokens
- 👥 **Group Management** - Create and manage expense groups
- 💰 **Expense Tracking** - Add, edit, and split expenses
- 📊 **Balance Overview** - Real-time balance calculations
- 🔔 **Notifications** - Real-time notifications via WebSocket
- 📱 **Responsive Design** - Works seamlessly on desktop and mobile

## Tech Stack

- **Framework**: Next.js 14 (App Router)
- **Language**: TypeScript
- **Styling**: TailwindCSS + shadcn/ui components
- **State Management**: Zustand
- **Data Fetching**: TanStack React Query
- **Form Handling**: React Hook Form + Zod validation
- **Real-time**: WebSocket integration

## Getting Started

### Prerequisites

- Node.js 18+ 
- npm or yarn
- Running backend services (API Gateway on port 8080)

### Installation

1. Install dependencies:
   ```bash
   npm install
   ```

2. Set up environment variables:
   ```bash
   cp .env.local.example .env.local
   ```

3. Start the development server:
   ```bash
   npm run dev
   ```

4. Open [http://localhost:3000](http://localhost:3000) in your browser.

## Project Structure

```
src/
├── app/                    # Next.js App Router pages
│   ├── auth/              # Authentication pages
│   ├── dashboard/         # Main dashboard
│   ├── groups/            # Group management
│   ├── expenses/          # Expense management
│   ├── balances/          # Balance overview
│   ├── activity/          # Activity/notifications
│   └── settings/          # User settings
├── components/            # React components
│   ├── ui/               # Base UI components (shadcn/ui style)
│   ├── auth/             # Authentication components
│   ├── layout/           # Layout components
│   ├── groups/           # Group-related components
│   ├── expenses/         # Expense-related components
│   ├── balances/         # Balance-related components
│   └── notifications/    # Notification components
├── lib/                  # Utilities and configurations
│   ├── api/             # API client and endpoints
│   ├── hooks/           # Custom React hooks
│   ├── stores/          # Zustand stores
│   ├── types/           # TypeScript types
│   └── utils/           # Utility functions
└── styles/              # Global styles
```

## Available Scripts

- `npm run dev` - Start development server
- `npm run build` - Build for production
- `npm run start` - Start production server
- `npm run lint` - Run ESLint

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `NEXT_PUBLIC_API_URL` | Backend API base URL | `http://localhost:8080/api/v1` |
| `NEXT_PUBLIC_WS_URL` | WebSocket URL | `ws://localhost:8080` |

## API Integration

The frontend connects to the following backend services through the API Gateway:

- **User Service** (8081) - Authentication and user management
- **Group Service** (8082) - Group CRUD operations
- **Expense Service** (8083) - Expense management
- **Balance Service** (8084) - Balance calculations
- **Settlement Service** (8085) - Payment settlements
- **Notification Service** (8086) - Notifications

## Authentication Flow

1. User logs in via `/auth/login`
2. Backend returns JWT access token and refresh token
3. Tokens are stored in Zustand store (with localStorage persistence)
4. Access token is attached to all API requests via Axios interceptor
5. When access token expires, refresh token is used to get new tokens
6. Invalid tokens redirect to login page

## Real-time Updates

The application uses WebSocket to receive real-time updates:

- New expenses added
- Expense modifications
- Settlement notifications
- Group member changes
- Balance updates

## License

MIT
