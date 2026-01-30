'use client';

import { useState, useEffect } from 'react';
import { WifiOff, RefreshCw } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils/cn';

interface OfflineIndicatorProps {
  className?: string;
}

export function OfflineIndicator({ className }: OfflineIndicatorProps) {
  const [isOnline, setIsOnline] = useState(true);
  const [showBanner, setShowBanner] = useState(false);

  useEffect(() => {
    // Check initial status
    setIsOnline(navigator.onLine);
    
    const handleOnline = () => {
      setIsOnline(true);
      // Show "back online" message briefly
      setShowBanner(true);
      setTimeout(() => setShowBanner(false), 3000);
    };

    const handleOffline = () => {
      setIsOnline(false);
      setShowBanner(true);
    };

    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);

    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
    };
  }, []);

  if (isOnline && !showBanner) {
    return null;
  }

  return (
    <div
      className={cn(
        'fixed bottom-4 left-4 right-4 md:left-auto md:right-4 md:w-80 z-50',
        'animate-in slide-in-from-bottom-4 fade-in duration-300',
        className
      )}
    >
      <div
        className={cn(
          'flex items-center gap-3 rounded-lg border p-4 shadow-lg',
          isOnline
            ? 'bg-green-50 border-green-200 dark:bg-green-950 dark:border-green-800'
            : 'bg-yellow-50 border-yellow-200 dark:bg-yellow-950 dark:border-yellow-800'
        )}
      >
        {isOnline ? (
          <>
            <div className="p-2 rounded-full bg-green-100 dark:bg-green-900">
              <RefreshCw className="h-4 w-4 text-green-600 dark:text-green-400" />
            </div>
            <div className="flex-1">
              <p className="text-sm font-medium text-green-800 dark:text-green-200">
                Back online
              </p>
              <p className="text-xs text-green-600 dark:text-green-400">
                Your connection has been restored
              </p>
            </div>
          </>
        ) : (
          <>
            <div className="p-2 rounded-full bg-yellow-100 dark:bg-yellow-900">
              <WifiOff className="h-4 w-4 text-yellow-600 dark:text-yellow-400" />
            </div>
            <div className="flex-1">
              <p className="text-sm font-medium text-yellow-800 dark:text-yellow-200">
                You're offline
              </p>
              <p className="text-xs text-yellow-600 dark:text-yellow-400">
                Some features may be unavailable
              </p>
            </div>
            <Button
              size="sm"
              variant="outline"
              onClick={() => window.location.reload()}
              className="shrink-0"
            >
              Retry
            </Button>
          </>
        )}
      </div>
    </div>
  );
}

// Hook to check online status
export function useOnlineStatus() {
  const [isOnline, setIsOnline] = useState(true);

  useEffect(() => {
    setIsOnline(navigator.onLine);

    const handleOnline = () => setIsOnline(true);
    const handleOffline = () => setIsOnline(false);

    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);

    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
    };
  }, []);

  return isOnline;
}
