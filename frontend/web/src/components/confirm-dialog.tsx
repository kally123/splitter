'use client';

import { useState } from 'react';
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Loader2, AlertTriangle } from 'lucide-react';

interface ConfirmDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  title: string;
  description: string;
  confirmLabel?: string;
  cancelLabel?: string;
  onConfirm: () => void | Promise<void>;
  variant?: 'default' | 'destructive';
  isLoading?: boolean;
}

export function ConfirmDialog({
  open,
  onOpenChange,
  title,
  description,
  confirmLabel = 'Confirm',
  cancelLabel = 'Cancel',
  onConfirm,
  variant = 'default',
  isLoading = false,
}: ConfirmDialogProps) {
  const handleConfirm = async () => {
    await onConfirm();
    onOpenChange(false);
  };

  return (
    <AlertDialog open={open} onOpenChange={onOpenChange}>
      <AlertDialogContent>
        <AlertDialogHeader>
          {variant === 'destructive' && (
            <div className="flex items-center gap-2 text-destructive mb-2">
              <AlertTriangle className="h-5 w-5" />
            </div>
          )}
          <AlertDialogTitle>{title}</AlertDialogTitle>
          <AlertDialogDescription>{description}</AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel disabled={isLoading}>{cancelLabel}</AlertDialogCancel>
          <AlertDialogAction
            onClick={handleConfirm}
            disabled={isLoading}
            className={variant === 'destructive' ? 'bg-destructive text-destructive-foreground hover:bg-destructive/90' : ''}
          >
            {isLoading && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
            {confirmLabel}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}

interface DeleteConfirmDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  itemName: string;
  itemType?: string;
  onConfirm: () => void | Promise<void>;
  isLoading?: boolean;
  requireConfirmation?: boolean;
  confirmationText?: string;
}

export function DeleteConfirmDialog({
  open,
  onOpenChange,
  itemName,
  itemType = 'item',
  onConfirm,
  isLoading = false,
  requireConfirmation = false,
  confirmationText,
}: DeleteConfirmDialogProps) {
  const [inputValue, setInputValue] = useState('');
  const textToMatch = confirmationText || itemName;
  const isConfirmed = !requireConfirmation || inputValue === textToMatch;

  const handleConfirm = async () => {
    if (!isConfirmed) return;
    await onConfirm();
    setInputValue('');
    onOpenChange(false);
  };

  const handleOpenChange = (newOpen: boolean) => {
    if (!newOpen) {
      setInputValue('');
    }
    onOpenChange(newOpen);
  };

  return (
    <AlertDialog open={open} onOpenChange={handleOpenChange}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <div className="flex items-center gap-2 text-destructive mb-2">
            <div className="p-2 rounded-full bg-destructive/10">
              <AlertTriangle className="h-5 w-5" />
            </div>
          </div>
          <AlertDialogTitle>Delete {itemType}?</AlertDialogTitle>
          <AlertDialogDescription>
            Are you sure you want to delete <strong>"{itemName}"</strong>? This action cannot be undone.
          </AlertDialogDescription>
        </AlertDialogHeader>

        {requireConfirmation && (
          <div className="space-y-2 py-2">
            <Label htmlFor="confirmation">
              Type <strong>{textToMatch}</strong> to confirm:
            </Label>
            <Input
              id="confirmation"
              value={inputValue}
              onChange={(e) => setInputValue(e.target.value)}
              placeholder={textToMatch}
              autoComplete="off"
            />
          </div>
        )}

        <AlertDialogFooter>
          <AlertDialogCancel disabled={isLoading}>Cancel</AlertDialogCancel>
          <Button
            variant="destructive"
            onClick={handleConfirm}
            disabled={isLoading || !isConfirmed}
          >
            {isLoading && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
            Delete
          </Button>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}

// Hook for easy confirmation dialog usage
export function useConfirmDialog() {
  const [dialogState, setDialogState] = useState<{
    open: boolean;
    title: string;
    description: string;
    onConfirm: () => void | Promise<void>;
    variant?: 'default' | 'destructive';
  }>({
    open: false,
    title: '',
    description: '',
    onConfirm: () => {},
  });

  const [isLoading, setIsLoading] = useState(false);

  const confirm = (options: {
    title: string;
    description: string;
    onConfirm: () => void | Promise<void>;
    variant?: 'default' | 'destructive';
  }) => {
    setDialogState({ ...options, open: true });
  };

  const handleConfirm = async () => {
    setIsLoading(true);
    try {
      await dialogState.onConfirm();
    } finally {
      setIsLoading(false);
      setDialogState((prev) => ({ ...prev, open: false }));
    }
  };

  const dialog = (
    <ConfirmDialog
      open={dialogState.open}
      onOpenChange={(open) => setDialogState((prev) => ({ ...prev, open }))}
      title={dialogState.title}
      description={dialogState.description}
      onConfirm={handleConfirm}
      variant={dialogState.variant}
      isLoading={isLoading}
    />
  );

  return { confirm, dialog };
}
