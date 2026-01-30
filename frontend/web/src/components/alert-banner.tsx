'use client';

import { AlertCircle, CheckCircle2, Info, AlertTriangle, X } from 'lucide-react';
import { cn } from '@/lib/utils/cn';
import { Button } from '@/components/ui/button';

interface AlertBannerProps {
  variant?: 'info' | 'success' | 'warning' | 'error';
  title?: string;
  message: string;
  dismissible?: boolean;
  onDismiss?: () => void;
  action?: {
    label: string;
    onClick: () => void;
  };
  className?: string;
}

const variantStyles = {
  info: {
    container: 'bg-blue-50 border-blue-200 dark:bg-blue-950/50 dark:border-blue-800',
    icon: 'text-blue-500',
    title: 'text-blue-800 dark:text-blue-200',
    message: 'text-blue-700 dark:text-blue-300',
    button: 'text-blue-700 hover:bg-blue-100 dark:text-blue-300 dark:hover:bg-blue-900',
  },
  success: {
    container: 'bg-green-50 border-green-200 dark:bg-green-950/50 dark:border-green-800',
    icon: 'text-green-500',
    title: 'text-green-800 dark:text-green-200',
    message: 'text-green-700 dark:text-green-300',
    button: 'text-green-700 hover:bg-green-100 dark:text-green-300 dark:hover:bg-green-900',
  },
  warning: {
    container: 'bg-yellow-50 border-yellow-200 dark:bg-yellow-950/50 dark:border-yellow-800',
    icon: 'text-yellow-500',
    title: 'text-yellow-800 dark:text-yellow-200',
    message: 'text-yellow-700 dark:text-yellow-300',
    button: 'text-yellow-700 hover:bg-yellow-100 dark:text-yellow-300 dark:hover:bg-yellow-900',
  },
  error: {
    container: 'bg-red-50 border-red-200 dark:bg-red-950/50 dark:border-red-800',
    icon: 'text-red-500',
    title: 'text-red-800 dark:text-red-200',
    message: 'text-red-700 dark:text-red-300',
    button: 'text-red-700 hover:bg-red-100 dark:text-red-300 dark:hover:bg-red-900',
  },
};

const icons = {
  info: Info,
  success: CheckCircle2,
  warning: AlertTriangle,
  error: AlertCircle,
};

export function AlertBanner({
  variant = 'info',
  title,
  message,
  dismissible = false,
  onDismiss,
  action,
  className,
}: AlertBannerProps) {
  const styles = variantStyles[variant];
  const Icon = icons[variant];

  return (
    <div
      className={cn(
        'relative flex items-start gap-3 rounded-lg border p-4',
        styles.container,
        className
      )}
      role="alert"
    >
      <Icon className={cn('h-5 w-5 shrink-0 mt-0.5', styles.icon)} />
      
      <div className="flex-1 min-w-0">
        {title && (
          <h4 className={cn('font-medium', styles.title)}>
            {title}
          </h4>
        )}
        <p className={cn('text-sm', !title && 'font-medium', styles.message)}>
          {message}
        </p>
        
        {action && (
          <Button
            variant="ghost"
            size="sm"
            className={cn('mt-2 h-auto p-0 font-medium', styles.button)}
            onClick={action.onClick}
          >
            {action.label}
          </Button>
        )}
      </div>
      
      {dismissible && onDismiss && (
        <Button
          variant="ghost"
          size="icon"
          className={cn('h-6 w-6 shrink-0', styles.button)}
          onClick={onDismiss}
        >
          <X className="h-4 w-4" />
          <span className="sr-only">Dismiss</span>
        </Button>
      )}
    </div>
  );
}

// Page-level banner that spans the full width
export function PageBanner({
  variant = 'info',
  message,
  action,
  dismissible,
  onDismiss,
}: Omit<AlertBannerProps, 'title' | 'className'>) {
  const styles = variantStyles[variant];
  const Icon = icons[variant];

  return (
    <div
      className={cn(
        'flex items-center justify-center gap-2 px-4 py-2 text-sm',
        styles.container
      )}
      role="alert"
    >
      <Icon className={cn('h-4 w-4', styles.icon)} />
      <span className={styles.message}>{message}</span>
      
      {action && (
        <Button
          variant="ghost"
          size="sm"
          className={cn('h-auto py-0.5 px-2 text-sm font-medium', styles.button)}
          onClick={action.onClick}
        >
          {action.label}
        </Button>
      )}
      
      {dismissible && onDismiss && (
        <Button
          variant="ghost"
          size="icon"
          className={cn('h-5 w-5 ml-2', styles.button)}
          onClick={onDismiss}
        >
          <X className="h-3 w-3" />
          <span className="sr-only">Dismiss</span>
        </Button>
      )}
    </div>
  );
}
