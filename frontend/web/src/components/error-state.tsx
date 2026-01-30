'use client';

import { RefreshCw, WifiOff, ServerOff, AlertCircle, FileX } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { cn } from '@/lib/utils/cn';

interface ErrorStateProps {
  title?: string;
  description?: string;
  error?: Error | string | null;
  onRetry?: () => void;
  retryLabel?: string;
  variant?: 'default' | 'network' | 'server' | 'not-found' | 'inline';
  className?: string;
}

export function ErrorState({
  title,
  description,
  error,
  onRetry,
  retryLabel = 'Try Again',
  variant = 'default',
  className,
}: ErrorStateProps) {
  const errorMessage = typeof error === 'string' ? error : error?.message;

  const getIcon = () => {
    switch (variant) {
      case 'network':
        return <WifiOff className="h-8 w-8 text-muted-foreground" />;
      case 'server':
        return <ServerOff className="h-8 w-8 text-destructive" />;
      case 'not-found':
        return <FileX className="h-8 w-8 text-muted-foreground" />;
      default:
        return <AlertCircle className="h-8 w-8 text-destructive" />;
    }
  };

  const getDefaultTitle = () => {
    switch (variant) {
      case 'network':
        return 'Connection Error';
      case 'server':
        return 'Server Error';
      case 'not-found':
        return 'Not Found';
      default:
        return 'Something went wrong';
    }
  };

  const getDefaultDescription = () => {
    switch (variant) {
      case 'network':
        return 'Please check your internet connection and try again.';
      case 'server':
        return 'Our servers are experiencing issues. Please try again later.';
      case 'not-found':
        return 'The requested resource could not be found.';
      default:
        return 'An unexpected error occurred. Please try again.';
    }
  };

  if (variant === 'inline') {
    return (
      <div className={cn('flex items-center gap-3 p-4 rounded-lg border border-destructive/20 bg-destructive/5', className)}>
        <AlertCircle className="h-5 w-5 text-destructive shrink-0" />
        <div className="flex-1 min-w-0">
          <p className="text-sm font-medium text-destructive">
            {title || getDefaultTitle()}
          </p>
          {(description || errorMessage) && (
            <p className="text-sm text-muted-foreground truncate">
              {description || errorMessage || getDefaultDescription()}
            </p>
          )}
        </div>
        {onRetry && (
          <Button size="sm" variant="outline" onClick={onRetry}>
            <RefreshCw className="h-4 w-4 mr-1" />
            Retry
          </Button>
        )}
      </div>
    );
  }

  return (
    <Card className={cn('max-w-md mx-auto', className)}>
      <CardHeader className="text-center">
        <div className="flex justify-center mb-4">
          <div className="p-4 rounded-full bg-muted">
            {getIcon()}
          </div>
        </div>
        <CardTitle>{title || getDefaultTitle()}</CardTitle>
        <CardDescription>
          {description || getDefaultDescription()}
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        {errorMessage && process.env.NODE_ENV === 'development' && (
          <div className="p-3 rounded-md bg-muted text-sm font-mono text-muted-foreground overflow-auto">
            {errorMessage}
          </div>
        )}
        
        {onRetry && (
          <Button onClick={onRetry} className="w-full">
            <RefreshCw className="mr-2 h-4 w-4" />
            {retryLabel}
          </Button>
        )}
      </CardContent>
    </Card>
  );
}

// Specific error state variants for common use cases
export function NetworkError({ onRetry }: { onRetry?: () => void }) {
  return (
    <ErrorState
      variant="network"
      onRetry={onRetry}
    />
  );
}

export function ServerError({ onRetry }: { onRetry?: () => void }) {
  return (
    <ErrorState
      variant="server"
      onRetry={onRetry}
    />
  );
}

export function NotFoundError({
  title = 'Not Found',
  description = 'The page or resource you are looking for does not exist.',
}: {
  title?: string;
  description?: string;
}) {
  return (
    <ErrorState
      variant="not-found"
      title={title}
      description={description}
    />
  );
}
