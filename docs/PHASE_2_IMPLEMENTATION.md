# Phase 2: Integration & Frontend Implementation

## Overview

Phase 2 builds on the core microservices established in Phase 1, focusing on API Gateway integration, authentication infrastructure, and frontend development. This phase delivers a fully functional MVP that end-users can interact with.

**Duration:** Week 9-10  
**Dependencies:** All Phase 1 services must be complete and tested

---

## Goals

1. **API Gateway Integration** - Unified entry point for all microservices
2. **Authentication Infrastructure** - Complete auth flow with JWT tokens
3. **Frontend Application** - Next.js web application with full functionality
4. **Real-time Updates** - WebSocket support for live notifications
5. **End-to-End Testing** - Verify all user flows work correctly

---

## Sprint 2.1: API Gateway Integration (Week 9)

### 2.1.1 Route Configuration

**Objective:** Configure API Gateway to route requests to all backend services

**Components:**
- Route definitions for all microservices
- Path-based routing predicates
- Load balancing configuration
- Request/response transformation

**Route Configuration:**
```yaml
# application.yml
spring:
  cloud:
    gateway:
      routes:
        # User Service Routes
        - id: user-service
          uri: lb://user-service
          predicates:
            - Path=/api/v1/users/**, /api/v1/auth/**
          filters:
            - RewritePath=/api/v1/(?<segment>.*), /$\{segment}
            
        # Group Service Routes
        - id: group-service
          uri: lb://group-service
          predicates:
            - Path=/api/v1/groups/**, /api/v1/invitations/**
          filters:
            - RewritePath=/api/v1/(?<segment>.*), /$\{segment}
            
        # Expense Service Routes
        - id: expense-service
          uri: lb://expense-service
          predicates:
            - Path=/api/v1/expenses/**
          filters:
            - RewritePath=/api/v1/(?<segment>.*), /$\{segment}
            
        # Balance Service Routes
        - id: balance-service
          uri: lb://balance-service
          predicates:
            - Path=/api/v1/balances/**
          filters:
            - RewritePath=/api/v1/(?<segment>.*), /$\{segment}
            
        # Settlement Service Routes
        - id: settlement-service
          uri: lb://settlement-service
          predicates:
            - Path=/api/v1/settlements/**
          filters:
            - RewritePath=/api/v1/(?<segment>.*), /$\{segment}
            
        # Notification Service Routes
        - id: notification-service
          uri: lb://notification-service
          predicates:
            - Path=/api/v1/notifications/**
          filters:
            - RewritePath=/api/v1/(?<segment>.*), /$\{segment}
```

**Files to Create/Update:**
```
services/api-gateway/
├── src/main/java/com/splitter/gateway/
│   ├── config/
│   │   ├── RouteConfig.java
│   │   ├── CorsConfig.java
│   │   ├── RateLimitConfig.java
│   │   └── WebSocketConfig.java
│   ├── filter/
│   │   ├── AuthenticationFilter.java
│   │   ├── LoggingFilter.java
│   │   ├── RateLimitFilter.java
│   │   └── CorrelationIdFilter.java
│   └── exception/
│       └── GatewayExceptionHandler.java
└── src/main/resources/
    └── application.yml
```

**Acceptance Criteria:**
- [ ] All service endpoints accessible through gateway on port 8080
- [ ] Request routing works correctly for all paths
- [ ] CORS properly configured for frontend origins
- [ ] Rate limiting prevents abuse (100 req/min per user)

---

### 2.1.2 Authentication Filter

**Objective:** Implement JWT validation at the gateway level

**Components:**

**AuthenticationFilter.java:**
```java
@Component
public class AuthenticationFilter implements GlobalFilter, Ordered {
    
    private final JwtUtils jwtUtils;
    
    private static final List<String> PUBLIC_PATHS = List.of(
        "/api/v1/auth/login",
        "/api/v1/auth/register",
        "/api/v1/auth/refresh",
        "/api/v1/auth/forgot-password",
        "/actuator/health"
    );
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        
        // Skip auth for public endpoints
        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }
        
        // Extract and validate JWT
        String authHeader = exchange.getRequest()
            .getHeaders()
            .getFirst(HttpHeaders.AUTHORIZATION);
            
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return onError(exchange, HttpStatus.UNAUTHORIZED);
        }
        
        String token = authHeader.substring(7);
        
        return jwtUtils.validateToken(token)
            .flatMap(claims -> {
                // Add user info to headers for downstream services
                ServerHttpRequest mutatedRequest = exchange.getRequest()
                    .mutate()
                    .header("X-User-Id", claims.getSubject())
                    .header("X-User-Email", claims.get("email", String.class))
                    .header("X-User-Roles", String.join(",", claims.get("roles", List.class)))
                    .build();
                    
                return chain.filter(exchange.mutate().request(mutatedRequest).build());
            })
            .onErrorResume(e -> onError(exchange, HttpStatus.UNAUTHORIZED));
    }
    
    @Override
    public int getOrder() {
        return -100; // Run before other filters
    }
}
```

**Acceptance Criteria:**
- [ ] Valid JWT tokens pass through to services
- [ ] Invalid/expired tokens return 401 Unauthorized
- [ ] User info propagated in headers to downstream services
- [ ] Public endpoints accessible without authentication

---

### 2.1.3 CORS Configuration

**Objective:** Enable cross-origin requests from frontend applications

**CorsConfig.java:**
```java
@Configuration
public class CorsConfig {
    
    @Bean
    public CorsWebFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOrigin("http://localhost:3000"); // Dev frontend
        config.addAllowedOrigin("https://splitter.app");  // Production
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.addExposedHeader("Authorization");
        config.addExposedHeader("X-Correlation-Id");
        config.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        
        return new CorsWebFilter(source);
    }
}
```

---

### 2.1.4 Rate Limiting

**Objective:** Protect backend services from excessive requests

**RateLimitConfig.java:**
```java
@Configuration
public class RateLimitConfig {
    
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest()
                .getHeaders()
                .getFirst("X-User-Id");
            return Mono.just(userId != null ? userId : 
                exchange.getRequest().getRemoteAddress().getHostString());
        };
    }
    
    @Bean
    public RedisRateLimiter redisRateLimiter() {
        return new RedisRateLimiter(100, 200); // 100 requests/sec, burst of 200
    }
}
```

---

### 2.1.5 User Service - Authentication Endpoints

**Objective:** Add authentication endpoints to User Service

**New Endpoints:**
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/auth/register` | Register new user |
| POST | `/api/v1/auth/login` | Login and get tokens |
| POST | `/api/v1/auth/refresh` | Refresh access token |
| POST | `/api/v1/auth/logout` | Invalidate refresh token |
| POST | `/api/v1/auth/forgot-password` | Request password reset |
| POST | `/api/v1/auth/reset-password` | Reset password with token |

**Files to Create:**
```
services/user-service/src/main/java/com/splitter/user/
├── controller/
│   └── AuthController.java
├── service/
│   ├── AuthService.java
│   └── TokenService.java
├── dto/
│   ├── LoginRequest.java
│   ├── LoginResponse.java
│   ├── RegisterRequest.java
│   ├── RefreshTokenRequest.java
│   └── PasswordResetRequest.java
└── security/
    ├── JwtTokenProvider.java
    └── PasswordEncoder.java
```

**AuthController.java:**
```java
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthService authService;
    
    @PostMapping("/register")
    public Mono<ResponseEntity<UserDto>> register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request)
            .map(user -> ResponseEntity.status(HttpStatus.CREATED).body(user));
    }
    
    @PostMapping("/login")
    public Mono<ResponseEntity<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request)
            .map(ResponseEntity::ok);
    }
    
    @PostMapping("/refresh")
    public Mono<ResponseEntity<LoginResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refreshToken(request.getRefreshToken())
            .map(ResponseEntity::ok);
    }
    
    @PostMapping("/logout")
    public Mono<ResponseEntity<Void>> logout(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        return authService.logout(token)
            .then(Mono.just(ResponseEntity.ok().<Void>build()));
    }
}
```

**LoginResponse.java:**
```java
@Data
@Builder
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;
    private UserDto user;
}
```

**Database Migration - Refresh Tokens:**
```sql
-- V3__create_refresh_tokens_table.sql
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(500) UNIQUE NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    revoked_at TIMESTAMP
);

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);
```

**Acceptance Criteria:**
- [ ] Users can register with email/password
- [ ] Login returns JWT access + refresh tokens
- [ ] Access tokens expire in 15 minutes
- [ ] Refresh tokens expire in 7 days
- [ ] Password reset flow works via email

---

## Sprint 2.2: Frontend Development (Week 9-10)

### 2.2.1 Project Setup

**Objective:** Set up Next.js 14 project with all required dependencies

**Commands:**
```bash
# Create Next.js project
npx create-next-app@latest frontend/web --typescript --tailwind --eslint --app --src-dir

# Install dependencies
cd frontend/web
npm install @tanstack/react-query axios zustand zod react-hook-form @hookform/resolvers
npm install @radix-ui/react-dialog @radix-ui/react-dropdown-menu @radix-ui/react-tabs
npm install lucide-react class-variance-authority clsx tailwind-merge
npm install date-fns recharts
npm install -D @types/node

# Initialize shadcn/ui
npx shadcn-ui@latest init
npx shadcn-ui@latest add button input card dialog dropdown-menu tabs avatar badge toast
```

**Project Structure:**
```
frontend/web/
├── src/
│   ├── app/
│   │   ├── (auth)/
│   │   │   ├── login/
│   │   │   │   └── page.tsx
│   │   │   ├── register/
│   │   │   │   └── page.tsx
│   │   │   ├── forgot-password/
│   │   │   │   └── page.tsx
│   │   │   └── layout.tsx
│   │   ├── (dashboard)/
│   │   │   ├── page.tsx                 # Dashboard home
│   │   │   ├── groups/
│   │   │   │   ├── page.tsx             # Groups list
│   │   │   │   ├── [id]/
│   │   │   │   │   └── page.tsx         # Group detail
│   │   │   │   └── new/
│   │   │   │       └── page.tsx         # Create group
│   │   │   ├── expenses/
│   │   │   │   ├── page.tsx             # All expenses
│   │   │   │   └── new/
│   │   │   │       └── page.tsx         # Add expense
│   │   │   ├── activity/
│   │   │   │   └── page.tsx             # Activity feed
│   │   │   ├── friends/
│   │   │   │   └── page.tsx             # Friends list
│   │   │   ├── settings/
│   │   │   │   └── page.tsx             # User settings
│   │   │   └── layout.tsx               # Dashboard layout with nav
│   │   ├── layout.tsx                   # Root layout
│   │   ├── providers.tsx                # React Query, etc.
│   │   └── globals.css
│   ├── components/
│   │   ├── ui/                          # shadcn components
│   │   ├── layout/
│   │   │   ├── Header.tsx
│   │   │   ├── Sidebar.tsx
│   │   │   ├── MobileNav.tsx
│   │   │   └── Footer.tsx
│   │   ├── auth/
│   │   │   ├── LoginForm.tsx
│   │   │   ├── RegisterForm.tsx
│   │   │   └── ProtectedRoute.tsx
│   │   ├── groups/
│   │   │   ├── GroupCard.tsx
│   │   │   ├── GroupList.tsx
│   │   │   ├── GroupForm.tsx
│   │   │   ├── GroupMembers.tsx
│   │   │   └── InviteModal.tsx
│   │   ├── expenses/
│   │   │   ├── ExpenseCard.tsx
│   │   │   ├── ExpenseList.tsx
│   │   │   ├── ExpenseForm.tsx
│   │   │   ├── SplitTypeSelector.tsx
│   │   │   ├── ParticipantSelector.tsx
│   │   │   └── CategoryPicker.tsx
│   │   ├── balance/
│   │   │   ├── BalanceSummary.tsx
│   │   │   ├── BalanceCard.tsx
│   │   │   ├── DebtList.tsx
│   │   │   └── SettleUpModal.tsx
│   │   ├── notifications/
│   │   │   ├── NotificationBell.tsx
│   │   │   ├── NotificationList.tsx
│   │   │   └── NotificationItem.tsx
│   │   └── common/
│   │       ├── LoadingSpinner.tsx
│   │       ├── ErrorBoundary.tsx
│   │       ├── EmptyState.tsx
│   │       └── Avatar.tsx
│   ├── lib/
│   │   ├── api/
│   │   │   ├── client.ts                # Axios instance
│   │   │   ├── auth.ts                  # Auth API calls
│   │   │   ├── groups.ts                # Group API calls
│   │   │   ├── expenses.ts              # Expense API calls
│   │   │   ├── balances.ts              # Balance API calls
│   │   │   ├── settlements.ts           # Settlement API calls
│   │   │   └── notifications.ts         # Notification API calls
│   │   ├── utils/
│   │   │   ├── cn.ts                    # Class name utility
│   │   │   ├── formatters.ts            # Date, currency formatters
│   │   │   └── validators.ts            # Zod schemas
│   │   └── constants.ts
│   ├── hooks/
│   │   ├── useAuth.ts
│   │   ├── useGroups.ts
│   │   ├── useExpenses.ts
│   │   ├── useBalances.ts
│   │   ├── useNotifications.ts
│   │   └── useWebSocket.ts
│   ├── stores/
│   │   ├── authStore.ts                 # Zustand auth store
│   │   └── uiStore.ts                   # UI state
│   └── types/
│       ├── auth.ts
│       ├── group.ts
│       ├── expense.ts
│       ├── balance.ts
│       ├── settlement.ts
│       └── notification.ts
├── public/
│   ├── icons/
│   └── images/
├── tailwind.config.js
├── next.config.js
├── tsconfig.json
└── package.json
```

---

### 2.2.2 API Client Setup

**Objective:** Configure Axios with interceptors for auth

**lib/api/client.ts:**
```typescript
import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';
import { useAuthStore } from '@/stores/authStore';

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor - add auth token
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = useAuthStore.getState().accessToken;
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor - handle token refresh
apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean };
    
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;
      
      try {
        const refreshToken = useAuthStore.getState().refreshToken;
        const response = await axios.post(`${API_BASE_URL}/api/v1/auth/refresh`, {
          refreshToken,
        });
        
        const { accessToken, refreshToken: newRefreshToken } = response.data;
        useAuthStore.getState().setTokens(accessToken, newRefreshToken);
        
        originalRequest.headers.Authorization = `Bearer ${accessToken}`;
        return apiClient(originalRequest);
      } catch (refreshError) {
        useAuthStore.getState().logout();
        window.location.href = '/login';
        return Promise.reject(refreshError);
      }
    }
    
    return Promise.reject(error);
  }
);
```

**stores/authStore.ts:**
```typescript
import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import { User } from '@/types/auth';

interface AuthState {
  user: User | null;
  accessToken: string | null;
  refreshToken: string | null;
  isAuthenticated: boolean;
  
  setUser: (user: User) => void;
  setTokens: (accessToken: string, refreshToken: string) => void;
  login: (user: User, accessToken: string, refreshToken: string) => void;
  logout: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      user: null,
      accessToken: null,
      refreshToken: null,
      isAuthenticated: false,
      
      setUser: (user) => set({ user }),
      
      setTokens: (accessToken, refreshToken) => 
        set({ accessToken, refreshToken }),
      
      login: (user, accessToken, refreshToken) =>
        set({ user, accessToken, refreshToken, isAuthenticated: true }),
      
      logout: () =>
        set({ user: null, accessToken: null, refreshToken: null, isAuthenticated: false }),
    }),
    {
      name: 'auth-storage',
      partialize: (state) => ({
        accessToken: state.accessToken,
        refreshToken: state.refreshToken,
      }),
    }
  )
);
```

---

### 2.2.3 Authentication UI

**Objective:** Implement login, register, and password reset pages

**components/auth/LoginForm.tsx:**
```typescript
'use client';

import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { useToast } from '@/components/ui/use-toast';
import { authApi } from '@/lib/api/auth';
import { useAuthStore } from '@/stores/authStore';
import { Loader2 } from 'lucide-react';

const loginSchema = z.object({
  email: z.string().email('Invalid email address'),
  password: z.string().min(8, 'Password must be at least 8 characters'),
});

type LoginFormData = z.infer<typeof loginSchema>;

export function LoginForm() {
  const [isLoading, setIsLoading] = useState(false);
  const router = useRouter();
  const { toast } = useToast();
  const login = useAuthStore((state) => state.login);
  
  const { register, handleSubmit, formState: { errors } } = useForm<LoginFormData>({
    resolver: zodResolver(loginSchema),
  });
  
  const onSubmit = async (data: LoginFormData) => {
    setIsLoading(true);
    try {
      const response = await authApi.login(data);
      login(response.user, response.accessToken, response.refreshToken);
      toast({ title: 'Welcome back!', description: 'Successfully logged in.' });
      router.push('/');
    } catch (error: any) {
      toast({
        title: 'Login failed',
        description: error.response?.data?.message || 'Invalid credentials',
        variant: 'destructive',
      });
    } finally {
      setIsLoading(false);
    }
  };
  
  return (
    <Card className="w-full max-w-md">
      <CardHeader className="space-y-1">
        <CardTitle className="text-2xl font-bold">Sign in</CardTitle>
        <CardDescription>
          Enter your email and password to access your account
        </CardDescription>
      </CardHeader>
      <CardContent>
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="email">Email</Label>
            <Input
              id="email"
              type="email"
              placeholder="you@example.com"
              {...register('email')}
            />
            {errors.email && (
              <p className="text-sm text-red-500">{errors.email.message}</p>
            )}
          </div>
          
          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <Label htmlFor="password">Password</Label>
              <Link href="/forgot-password" className="text-sm text-primary hover:underline">
                Forgot password?
              </Link>
            </div>
            <Input
              id="password"
              type="password"
              {...register('password')}
            />
            {errors.password && (
              <p className="text-sm text-red-500">{errors.password.message}</p>
            )}
          </div>
          
          <Button type="submit" className="w-full" disabled={isLoading}>
            {isLoading ? (
              <>
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                Signing in...
              </>
            ) : (
              'Sign in'
            )}
          </Button>
        </form>
        
        <div className="mt-4 text-center text-sm">
          Don't have an account?{' '}
          <Link href="/register" className="text-primary hover:underline">
            Sign up
          </Link>
        </div>
      </CardContent>
    </Card>
  );
}
```

---

### 2.2.4 Dashboard Layout

**Objective:** Create main dashboard layout with navigation

**app/(dashboard)/layout.tsx:**
```typescript
import { Sidebar } from '@/components/layout/Sidebar';
import { Header } from '@/components/layout/Header';
import { ProtectedRoute } from '@/components/auth/ProtectedRoute';

export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <ProtectedRoute>
      <div className="flex h-screen bg-gray-50">
        <Sidebar />
        <div className="flex flex-1 flex-col overflow-hidden">
          <Header />
          <main className="flex-1 overflow-y-auto p-6">
            {children}
          </main>
        </div>
      </div>
    </ProtectedRoute>
  );
}
```

**components/layout/Sidebar.tsx:**
```typescript
'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { cn } from '@/lib/utils/cn';
import {
  Home,
  Users,
  Receipt,
  Wallet,
  Bell,
  Settings,
  PieChart,
} from 'lucide-react';

const navigation = [
  { name: 'Dashboard', href: '/', icon: Home },
  { name: 'Groups', href: '/groups', icon: Users },
  { name: 'Expenses', href: '/expenses', icon: Receipt },
  { name: 'Activity', href: '/activity', icon: Bell },
  { name: 'Friends', href: '/friends', icon: Users },
  { name: 'Settings', href: '/settings', icon: Settings },
];

export function Sidebar() {
  const pathname = usePathname();
  
  return (
    <aside className="hidden w-64 border-r bg-white lg:block">
      <div className="flex h-16 items-center border-b px-6">
        <Link href="/" className="flex items-center gap-2">
          <Wallet className="h-8 w-8 text-primary" />
          <span className="text-xl font-bold">Splitter</span>
        </Link>
      </div>
      
      <nav className="space-y-1 p-4">
        {navigation.map((item) => {
          const isActive = pathname === item.href;
          return (
            <Link
              key={item.name}
              href={item.href}
              className={cn(
                'flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors',
                isActive
                  ? 'bg-primary text-primary-foreground'
                  : 'text-gray-700 hover:bg-gray-100'
              )}
            >
              <item.icon className="h-5 w-5" />
              {item.name}
            </Link>
          );
        })}
      </nav>
    </aside>
  );
}
```

---

### 2.2.5 Groups UI

**Objective:** Implement group listing, creation, and detail views

**components/groups/GroupCard.tsx:**
```typescript
import Link from 'next/link';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar';
import { Users } from 'lucide-react';
import { Group } from '@/types/group';
import { formatCurrency } from '@/lib/utils/formatters';

interface GroupCardProps {
  group: Group;
  balance?: number;
}

export function GroupCard({ group, balance }: GroupCardProps) {
  const balanceColor = balance && balance > 0 ? 'text-green-600' : 
                       balance && balance < 0 ? 'text-red-600' : 'text-gray-600';
  
  return (
    <Link href={`/groups/${group.id}`}>
      <Card className="transition-shadow hover:shadow-md">
        <CardHeader className="flex flex-row items-center gap-4 pb-2">
          <Avatar className="h-12 w-12">
            <AvatarImage src={group.coverImageUrl} />
            <AvatarFallback>{group.name.substring(0, 2).toUpperCase()}</AvatarFallback>
          </Avatar>
          <div className="flex-1">
            <CardTitle className="text-lg">{group.name}</CardTitle>
            <Badge variant="secondary" className="mt-1">
              {group.groupType}
            </Badge>
          </div>
        </CardHeader>
        <CardContent>
          <div className="flex items-center justify-between text-sm">
            <div className="flex items-center gap-1 text-gray-500">
              <Users className="h-4 w-4" />
              <span>{group.memberCount} members</span>
            </div>
            {balance !== undefined && (
              <span className={`font-medium ${balanceColor}`}>
                {balance > 0 ? '+' : ''}{formatCurrency(balance, group.defaultCurrency)}
              </span>
            )}
          </div>
        </CardContent>
      </Card>
    </Link>
  );
}
```

**hooks/useGroups.ts:**
```typescript
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { groupsApi } from '@/lib/api/groups';
import { CreateGroupRequest, UpdateGroupRequest } from '@/types/group';

export function useGroups() {
  return useQuery({
    queryKey: ['groups'],
    queryFn: groupsApi.getMyGroups,
  });
}

export function useGroup(id: string) {
  return useQuery({
    queryKey: ['groups', id],
    queryFn: () => groupsApi.getById(id),
    enabled: !!id,
  });
}

export function useCreateGroup() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: (data: CreateGroupRequest) => groupsApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['groups'] });
    },
  });
}

export function useUpdateGroup(id: string) {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: (data: UpdateGroupRequest) => groupsApi.update(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['groups', id] });
      queryClient.invalidateQueries({ queryKey: ['groups'] });
    },
  });
}

export function useDeleteGroup() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: (id: string) => groupsApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['groups'] });
    },
  });
}
```

---

### 2.2.6 Expense UI

**Objective:** Implement expense creation form with all split types

**components/expenses/ExpenseForm.tsx:**
```typescript
'use client';

import { useState } from 'react';
import { useForm, useFieldArray } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Calendar } from '@/components/ui/calendar';
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover';
import { CalendarIcon, Loader2 } from 'lucide-react';
import { format } from 'date-fns';
import { cn } from '@/lib/utils/cn';
import { SplitTypeSelector } from './SplitTypeSelector';
import { ParticipantSelector } from './ParticipantSelector';
import { CategoryPicker } from './CategoryPicker';
import { useCreateExpense } from '@/hooks/useExpenses';
import { useToast } from '@/components/ui/use-toast';

const expenseSchema = z.object({
  description: z.string().min(1, 'Description is required').max(255),
  amount: z.number().positive('Amount must be greater than 0'),
  currency: z.string().default('USD'),
  groupId: z.string().uuid(),
  paidBy: z.string().uuid(),
  splitType: z.enum(['EQUAL', 'EXACT', 'PERCENTAGE', 'SHARES']),
  participants: z.array(z.object({
    userId: z.string().uuid(),
    amount: z.number().optional(),
    percentage: z.number().optional(),
    shares: z.number().optional(),
  })).min(1, 'At least one participant is required'),
  category: z.string(),
  expenseDate: z.date(),
  notes: z.string().optional(),
});

type ExpenseFormData = z.infer<typeof expenseSchema>;

interface ExpenseFormProps {
  groupId: string;
  members: { id: string; displayName: string }[];
  onSuccess?: () => void;
}

export function ExpenseForm({ groupId, members, onSuccess }: ExpenseFormProps) {
  const [splitType, setSplitType] = useState<'EQUAL' | 'EXACT' | 'PERCENTAGE' | 'SHARES'>('EQUAL');
  const { toast } = useToast();
  const createExpense = useCreateExpense();
  
  const { register, control, handleSubmit, watch, setValue, formState: { errors } } = useForm<ExpenseFormData>({
    resolver: zodResolver(expenseSchema),
    defaultValues: {
      groupId,
      splitType: 'EQUAL',
      expenseDate: new Date(),
      currency: 'USD',
      participants: members.map(m => ({ userId: m.id })),
    },
  });
  
  const amount = watch('amount');
  
  const onSubmit = async (data: ExpenseFormData) => {
    try {
      await createExpense.mutateAsync(data);
      toast({ title: 'Expense added', description: 'The expense has been recorded.' });
      onSuccess?.();
    } catch (error: any) {
      toast({
        title: 'Error',
        description: error.response?.data?.message || 'Failed to add expense',
        variant: 'destructive',
      });
    }
  };
  
  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
      {/* Description */}
      <div className="space-y-2">
        <Label htmlFor="description">Description</Label>
        <Input
          id="description"
          placeholder="What was this expense for?"
          {...register('description')}
        />
        {errors.description && (
          <p className="text-sm text-red-500">{errors.description.message}</p>
        )}
      </div>
      
      {/* Amount and Currency */}
      <div className="grid grid-cols-2 gap-4">
        <div className="space-y-2">
          <Label htmlFor="amount">Amount</Label>
          <Input
            id="amount"
            type="number"
            step="0.01"
            placeholder="0.00"
            {...register('amount', { valueAsNumber: true })}
          />
          {errors.amount && (
            <p className="text-sm text-red-500">{errors.amount.message}</p>
          )}
        </div>
        
        <div className="space-y-2">
          <Label htmlFor="currency">Currency</Label>
          <Select defaultValue="USD" onValueChange={(v) => setValue('currency', v)}>
            <SelectTrigger>
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="USD">USD ($)</SelectItem>
              <SelectItem value="EUR">EUR (€)</SelectItem>
              <SelectItem value="GBP">GBP (£)</SelectItem>
              <SelectItem value="INR">INR (₹)</SelectItem>
            </SelectContent>
          </Select>
        </div>
      </div>
      
      {/* Paid By */}
      <div className="space-y-2">
        <Label>Paid by</Label>
        <Select onValueChange={(v) => setValue('paidBy', v)}>
          <SelectTrigger>
            <SelectValue placeholder="Who paid?" />
          </SelectTrigger>
          <SelectContent>
            {members.map((member) => (
              <SelectItem key={member.id} value={member.id}>
                {member.displayName}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>
      
      {/* Split Type */}
      <div className="space-y-2">
        <Label>Split type</Label>
        <SplitTypeSelector
          value={splitType}
          onChange={(v) => {
            setSplitType(v);
            setValue('splitType', v);
          }}
        />
      </div>
      
      {/* Participants */}
      <ParticipantSelector
        members={members}
        splitType={splitType}
        totalAmount={amount || 0}
        control={control}
        errors={errors}
      />
      
      {/* Category */}
      <div className="space-y-2">
        <Label>Category</Label>
        <CategoryPicker onSelect={(c) => setValue('category', c)} />
      </div>
      
      {/* Date */}
      <div className="space-y-2">
        <Label>Date</Label>
        <Popover>
          <PopoverTrigger asChild>
            <Button variant="outline" className="w-full justify-start text-left font-normal">
              <CalendarIcon className="mr-2 h-4 w-4" />
              {watch('expenseDate') ? format(watch('expenseDate'), 'PPP') : 'Pick a date'}
            </Button>
          </PopoverTrigger>
          <PopoverContent className="w-auto p-0">
            <Calendar
              mode="single"
              selected={watch('expenseDate')}
              onSelect={(date) => date && setValue('expenseDate', date)}
            />
          </PopoverContent>
        </Popover>
      </div>
      
      {/* Notes */}
      <div className="space-y-2">
        <Label htmlFor="notes">Notes (optional)</Label>
        <Textarea
          id="notes"
          placeholder="Any additional details..."
          {...register('notes')}
        />
      </div>
      
      {/* Submit */}
      <Button type="submit" className="w-full" disabled={createExpense.isPending}>
        {createExpense.isPending ? (
          <>
            <Loader2 className="mr-2 h-4 w-4 animate-spin" />
            Adding expense...
          </>
        ) : (
          'Add Expense'
        )}
      </Button>
    </form>
  );
}
```

**components/expenses/SplitTypeSelector.tsx:**
```typescript
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group';
import { Equal, Hash, Percent, DollarSign } from 'lucide-react';

interface SplitTypeSelectorProps {
  value: 'EQUAL' | 'EXACT' | 'PERCENTAGE' | 'SHARES';
  onChange: (value: 'EQUAL' | 'EXACT' | 'PERCENTAGE' | 'SHARES') => void;
}

const splitTypes = [
  { value: 'EQUAL', label: 'Equal', icon: Equal, description: 'Split equally' },
  { value: 'EXACT', label: 'Exact', icon: DollarSign, description: 'Enter exact amounts' },
  { value: 'PERCENTAGE', label: 'Percentage', icon: Percent, description: 'Split by %' },
  { value: 'SHARES', label: 'Shares', icon: Hash, description: 'Split by shares' },
] as const;

export function SplitTypeSelector({ value, onChange }: SplitTypeSelectorProps) {
  return (
    <ToggleGroup type="single" value={value} onValueChange={(v) => v && onChange(v as any)}>
      {splitTypes.map((type) => (
        <ToggleGroupItem
          key={type.value}
          value={type.value}
          className="flex flex-col items-center gap-1 px-4 py-3"
        >
          <type.icon className="h-4 w-4" />
          <span className="text-xs">{type.label}</span>
        </ToggleGroupItem>
      ))}
    </ToggleGroup>
  );
}
```

---

### 2.2.7 Balance & Settlement UI

**Objective:** Display balances and implement settle up flow

**components/balance/BalanceSummary.tsx:**
```typescript
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { ArrowUpRight, ArrowDownRight, Minus } from 'lucide-react';
import { formatCurrency } from '@/lib/utils/formatters';

interface BalanceSummaryProps {
  youOwe: number;
  youAreOwed: number;
  currency?: string;
}

export function BalanceSummary({ youOwe, youAreOwed, currency = 'USD' }: BalanceSummaryProps) {
  const netBalance = youAreOwed - youOwe;
  
  return (
    <div className="grid gap-4 md:grid-cols-3">
      <Card>
        <CardHeader className="flex flex-row items-center justify-between pb-2">
          <CardTitle className="text-sm font-medium text-gray-500">
            You owe
          </CardTitle>
          <ArrowUpRight className="h-4 w-4 text-red-500" />
        </CardHeader>
        <CardContent>
          <div className="text-2xl font-bold text-red-600">
            {formatCurrency(youOwe, currency)}
          </div>
        </CardContent>
      </Card>
      
      <Card>
        <CardHeader className="flex flex-row items-center justify-between pb-2">
          <CardTitle className="text-sm font-medium text-gray-500">
            You are owed
          </CardTitle>
          <ArrowDownRight className="h-4 w-4 text-green-500" />
        </CardHeader>
        <CardContent>
          <div className="text-2xl font-bold text-green-600">
            {formatCurrency(youAreOwed, currency)}
          </div>
        </CardContent>
      </Card>
      
      <Card>
        <CardHeader className="flex flex-row items-center justify-between pb-2">
          <CardTitle className="text-sm font-medium text-gray-500">
            Net balance
          </CardTitle>
          <Minus className="h-4 w-4 text-gray-500" />
        </CardHeader>
        <CardContent>
          <div className={`text-2xl font-bold ${
            netBalance > 0 ? 'text-green-600' : netBalance < 0 ? 'text-red-600' : 'text-gray-600'
          }`}>
            {netBalance > 0 ? '+' : ''}{formatCurrency(netBalance, currency)}
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
```

**components/balance/SettleUpModal.tsx:**
```typescript
'use client';

import { useState } from 'react';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Loader2 } from 'lucide-react';
import { useCreateSettlement } from '@/hooks/useSettlements';
import { useToast } from '@/components/ui/use-toast';
import { formatCurrency } from '@/lib/utils/formatters';

interface SettleUpModalProps {
  isOpen: boolean;
  onClose: () => void;
  fromUserId: string;
  toUserId: string;
  toUserName: string;
  amount: number;
  currency: string;
  groupId?: string;
}

const paymentMethods = [
  { value: 'CASH', label: 'Cash' },
  { value: 'BANK_TRANSFER', label: 'Bank Transfer' },
  { value: 'VENMO', label: 'Venmo' },
  { value: 'PAYPAL', label: 'PayPal' },
  { value: 'ZELLE', label: 'Zelle' },
  { value: 'OTHER', label: 'Other' },
];

export function SettleUpModal({
  isOpen,
  onClose,
  fromUserId,
  toUserId,
  toUserName,
  amount,
  currency,
  groupId,
}: SettleUpModalProps) {
  const [settleAmount, setSettleAmount] = useState(amount);
  const [paymentMethod, setPaymentMethod] = useState('CASH');
  const [notes, setNotes] = useState('');
  
  const createSettlement = useCreateSettlement();
  const { toast } = useToast();
  
  const handleSubmit = async () => {
    try {
      await createSettlement.mutateAsync({
        fromUserId,
        toUserId,
        amount: settleAmount,
        currency,
        paymentMethod,
        notes,
        groupId,
      });
      
      toast({
        title: 'Settlement recorded',
        description: `Payment of ${formatCurrency(settleAmount, currency)} to ${toUserName} recorded.`,
      });
      onClose();
    } catch (error: any) {
      toast({
        title: 'Error',
        description: error.response?.data?.message || 'Failed to record settlement',
        variant: 'destructive',
      });
    }
  };
  
  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Settle up with {toUserName}</DialogTitle>
        </DialogHeader>
        
        <div className="space-y-4 py-4">
          <div className="space-y-2">
            <Label>Amount</Label>
            <Input
              type="number"
              step="0.01"
              value={settleAmount}
              onChange={(e) => setSettleAmount(parseFloat(e.target.value))}
            />
            <p className="text-sm text-gray-500">
              You owe {formatCurrency(amount, currency)} in total
            </p>
          </div>
          
          <div className="space-y-2">
            <Label>Payment method</Label>
            <Select value={paymentMethod} onValueChange={setPaymentMethod}>
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {paymentMethods.map((method) => (
                  <SelectItem key={method.value} value={method.value}>
                    {method.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          
          <div className="space-y-2">
            <Label>Notes (optional)</Label>
            <Input
              placeholder="Add a note..."
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
            />
          </div>
        </div>
        
        <DialogFooter>
          <Button variant="outline" onClick={onClose}>
            Cancel
          </Button>
          <Button onClick={handleSubmit} disabled={createSettlement.isPending}>
            {createSettlement.isPending ? (
              <>
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                Recording...
              </>
            ) : (
              'Record Payment'
            )}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
```

---

### 2.2.8 Notifications UI

**Objective:** Real-time notification display

**components/notifications/NotificationBell.tsx:**
```typescript
'use client';

import { useState } from 'react';
import { Bell } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover';
import { ScrollArea } from '@/components/ui/scroll-area';
import { Badge } from '@/components/ui/badge';
import { NotificationList } from './NotificationList';
import { useUnreadCount } from '@/hooks/useNotifications';

export function NotificationBell() {
  const [isOpen, setIsOpen] = useState(false);
  const { data: unreadCount = 0 } = useUnreadCount();
  
  return (
    <Popover open={isOpen} onOpenChange={setIsOpen}>
      <PopoverTrigger asChild>
        <Button variant="ghost" size="icon" className="relative">
          <Bell className="h-5 w-5" />
          {unreadCount > 0 && (
            <Badge 
              variant="destructive" 
              className="absolute -right-1 -top-1 h-5 w-5 rounded-full p-0 text-xs"
            >
              {unreadCount > 99 ? '99+' : unreadCount}
            </Badge>
          )}
        </Button>
      </PopoverTrigger>
      <PopoverContent className="w-80 p-0" align="end">
        <div className="flex items-center justify-between border-b px-4 py-3">
          <h3 className="font-semibold">Notifications</h3>
          <Button variant="ghost" size="sm">
            Mark all read
          </Button>
        </div>
        <ScrollArea className="h-[400px]">
          <NotificationList onClose={() => setIsOpen(false)} />
        </ScrollArea>
      </PopoverContent>
    </Popover>
  );
}
```

---

## Sprint 2.3: WebSocket Integration (Week 10)

### 2.3.1 WebSocket Gateway

**Objective:** Enable real-time updates for notifications and balances

**Files to Create:**
```
services/api-gateway/src/main/java/com/splitter/gateway/
├── websocket/
│   ├── WebSocketConfig.java
│   ├── WebSocketHandler.java
│   └── WebSocketSessionManager.java
```

**WebSocketConfig.java:**
```java
@Configuration
@EnableWebSocket
public class WebSocketConfig {
    
    @Bean
    public HandlerMapping webSocketHandlerMapping(WebSocketHandler handler) {
        Map<String, WebSocketHandler> map = new HashMap<>();
        map.put("/ws/**", handler);
        
        SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
        mapping.setUrlMap(map);
        mapping.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return mapping;
    }
    
    @Bean
    public WebSocketHandlerAdapter handlerAdapter() {
        return new WebSocketHandlerAdapter();
    }
}
```

**Frontend WebSocket Hook:**
```typescript
// hooks/useWebSocket.ts
import { useEffect, useRef, useCallback } from 'react';
import { useAuthStore } from '@/stores/authStore';
import { useQueryClient } from '@tanstack/react-query';

const WS_URL = process.env.NEXT_PUBLIC_WS_URL || 'ws://localhost:8080/ws';

export function useWebSocket() {
  const wsRef = useRef<WebSocket | null>(null);
  const accessToken = useAuthStore((state) => state.accessToken);
  const queryClient = useQueryClient();
  
  const connect = useCallback(() => {
    if (!accessToken || wsRef.current?.readyState === WebSocket.OPEN) return;
    
    const ws = new WebSocket(`${WS_URL}?token=${accessToken}`);
    
    ws.onopen = () => {
      console.log('WebSocket connected');
    };
    
    ws.onmessage = (event) => {
      const message = JSON.parse(event.data);
      
      switch (message.type) {
        case 'NOTIFICATION':
          queryClient.invalidateQueries({ queryKey: ['notifications'] });
          queryClient.invalidateQueries({ queryKey: ['notifications', 'unread'] });
          break;
        case 'BALANCE_UPDATE':
          queryClient.invalidateQueries({ queryKey: ['balances'] });
          break;
        case 'EXPENSE_CREATED':
        case 'EXPENSE_UPDATED':
        case 'EXPENSE_DELETED':
          queryClient.invalidateQueries({ queryKey: ['expenses'] });
          queryClient.invalidateQueries({ queryKey: ['balances'] });
          break;
      }
    };
    
    ws.onclose = () => {
      console.log('WebSocket disconnected');
      // Reconnect after delay
      setTimeout(connect, 5000);
    };
    
    wsRef.current = ws;
  }, [accessToken, queryClient]);
  
  useEffect(() => {
    connect();
    return () => {
      wsRef.current?.close();
    };
  }, [connect]);
  
  return wsRef.current;
}
```

---

## Testing Checklist

### API Gateway Tests
- [ ] All routes correctly forward to services
- [ ] JWT validation blocks invalid tokens
- [ ] CORS allows frontend origins
- [ ] Rate limiting triggers on excessive requests
- [ ] WebSocket connections authenticate properly

### Frontend Tests
- [ ] Login/Register forms validate input
- [ ] Protected routes redirect unauthenticated users
- [ ] Group CRUD operations work
- [ ] Expense creation with all split types works
- [ ] Balance calculations display correctly
- [ ] Settlement flow completes successfully
- [ ] Notifications appear in real-time

### Integration Tests
```bash
# Run all frontend tests
cd frontend/web && npm test

# Run E2E tests
cd frontend/web && npm run test:e2e

# Run API integration tests
cd services/api-gateway && mvn verify
```

---

## Deployment

### Frontend Deployment
```bash
# Build production bundle
cd frontend/web
npm run build

# Deploy to Vercel (recommended)
vercel --prod

# Or deploy to Docker
docker build -t splitter-web .
docker push splitter-web:latest
```

### API Gateway Deployment
```bash
# Build Docker image
cd services/api-gateway
docker build -t splitter-api-gateway .

# Deploy with Docker Compose
docker-compose -f docker-compose.prod.yml up -d api-gateway
```

---

## API Endpoints Summary (Phase 2)

### Authentication (User Service)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/auth/register` | Register new user |
| POST | `/api/v1/auth/login` | Login |
| POST | `/api/v1/auth/refresh` | Refresh token |
| POST | `/api/v1/auth/logout` | Logout |
| POST | `/api/v1/auth/forgot-password` | Request password reset |
| POST | `/api/v1/auth/reset-password` | Reset password |

### Gateway Features
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/ws` | WebSocket connection |
| GET | `/actuator/health` | Health check |
| GET | `/actuator/info` | Service info |

---

## Next Steps (Phase 3)

1. **End-to-End Testing**
   - Playwright/Cypress test suite
   - Critical user journey coverage
   - Performance baseline

2. **Bug Fixes & Polish**
   - Address all P0/P1 bugs
   - UI consistency review
   - Loading and empty states

3. **Production Deployment**
   - Kubernetes manifests
   - SSL/TLS configuration
   - Monitoring setup

---

## Files Created in Phase 2

### Backend
```
services/api-gateway/
├── src/main/java/com/splitter/gateway/
│   ├── config/
│   │   ├── RouteConfig.java
│   │   ├── CorsConfig.java
│   │   ├── RateLimitConfig.java
│   │   └── WebSocketConfig.java
│   ├── filter/
│   │   ├── AuthenticationFilter.java
│   │   ├── LoggingFilter.java
│   │   └── CorrelationIdFilter.java
│   ├── websocket/
│   │   ├── WebSocketHandler.java
│   │   └── WebSocketSessionManager.java
│   └── exception/
│       └── GatewayExceptionHandler.java

services/user-service/
├── src/main/java/com/splitter/user/
│   ├── controller/
│   │   └── AuthController.java
│   ├── service/
│   │   ├── AuthService.java
│   │   └── TokenService.java
│   ├── dto/
│   │   ├── LoginRequest.java
│   │   ├── LoginResponse.java
│   │   ├── RegisterRequest.java
│   │   └── RefreshTokenRequest.java
│   └── security/
│       ├── JwtTokenProvider.java
│       └── PasswordEncoder.java
```

### Frontend
```
frontend/web/
├── src/
│   ├── app/
│   │   ├── (auth)/
│   │   │   ├── login/page.tsx
│   │   │   ├── register/page.tsx
│   │   │   └── forgot-password/page.tsx
│   │   ├── (dashboard)/
│   │   │   ├── page.tsx
│   │   │   ├── groups/
│   │   │   ├── expenses/
│   │   │   ├── activity/
│   │   │   ├── friends/
│   │   │   └── settings/
│   ├── components/
│   │   ├── auth/
│   │   ├── groups/
│   │   ├── expenses/
│   │   ├── balance/
│   │   ├── notifications/
│   │   └── layout/
│   ├── lib/
│   │   ├── api/
│   │   └── utils/
│   ├── hooks/
│   ├── stores/
│   └── types/
```

---

*Phase 2 delivers a functional MVP with user-facing frontend and secure API access. All subsequent phases build upon this foundation.*
