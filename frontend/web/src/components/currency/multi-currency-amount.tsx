'use client';

import { cn } from '@/lib/utils';
import { useCurrencies } from '@/lib/hooks/useCurrencies';

interface MultiCurrencyAmountProps {
  originalAmount: number;
  originalCurrency: string;
  convertedAmount?: number;
  groupCurrency?: string;
  showConversion?: boolean;
  size?: 'sm' | 'md' | 'lg';
  className?: string;
}

export function MultiCurrencyAmount({
  originalAmount,
  originalCurrency,
  convertedAmount,
  groupCurrency,
  showConversion = true,
  size = 'md',
  className,
}: MultiCurrencyAmountProps) {
  const { data: currencies } = useCurrencies();
  
  const formatCurrency = (amount: number, currencyCode: string) => {
    const currency = currencies?.find(c => c.code === currencyCode);
    
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: currencyCode,
      minimumFractionDigits: currency?.decimalPlaces ?? 2,
      maximumFractionDigits: currency?.decimalPlaces ?? 2,
    }).format(amount);
  };

  const needsConversion = groupCurrency && 
    originalCurrency !== groupCurrency && 
    convertedAmount !== undefined;

  const sizeClasses = {
    sm: 'text-sm',
    md: 'text-base',
    lg: 'text-lg',
  };

  return (
    <div className={cn("flex flex-col", className)}>
      <span className={cn("font-semibold", sizeClasses[size])}>
        {formatCurrency(originalAmount, originalCurrency)}
      </span>
      {showConversion && needsConversion && (
        <span className="text-sm text-muted-foreground">
          ≈ {formatCurrency(convertedAmount!, groupCurrency)}
        </span>
      )}
    </div>
  );
}

interface CurrencyAmountProps {
  amount: number;
  currency: string;
  showSign?: boolean;
  signBehavior?: 'always' | 'positive' | 'negative';
  colorize?: boolean;
  className?: string;
}

export function CurrencyAmount({
  amount,
  currency,
  showSign = false,
  signBehavior = 'always',
  colorize = false,
  className,
}: CurrencyAmountProps) {
  const { data: currencies } = useCurrencies();
  const currencyInfo = currencies?.find(c => c.code === currency);
  
  const formattedAmount = new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: currency,
    minimumFractionDigits: currencyInfo?.decimalPlaces ?? 2,
    maximumFractionDigits: currencyInfo?.decimalPlaces ?? 2,
  }).format(Math.abs(amount));

  let sign = '';
  if (showSign) {
    if (signBehavior === 'always' || 
        (signBehavior === 'positive' && amount > 0) ||
        (signBehavior === 'negative' && amount < 0)) {
      sign = amount >= 0 ? '+' : '-';
    }
  }

  const colorClass = colorize
    ? amount > 0
      ? 'text-green-600 dark:text-green-400'
      : amount < 0
      ? 'text-red-600 dark:text-red-400'
      : ''
    : '';

  return (
    <span className={cn(colorClass, className)}>
      {sign}{formattedAmount}
    </span>
  );
}

interface CurrencyExchangeInfoProps {
  fromCurrency: string;
  toCurrency: string;
  exchangeRate: number;
  rateDate?: string;
  className?: string;
}

export function CurrencyExchangeInfo({
  fromCurrency,
  toCurrency,
  exchangeRate,
  rateDate,
  className,
}: CurrencyExchangeInfoProps) {
  return (
    <div className={cn("text-xs text-muted-foreground", className)}>
      <span>
        1 {fromCurrency} = {exchangeRate.toFixed(4)} {toCurrency}
      </span>
      {rateDate && (
        <span className="ml-2">
          (as of {new Date(rateDate).toLocaleDateString()})
        </span>
      )}
    </div>
  );
}
