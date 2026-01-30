import { useQuery } from '@tanstack/react-query';
import { api } from '@/lib/api';

export interface Currency {
  code: string;
  name: string;
  symbol: string;
  decimalPlaces: number;
}

export interface ConversionResult {
  originalAmount: number;
  convertedAmount: number;
  exchangeRate: number;
  fromCurrency: string;
  toCurrency: string;
  rateDate: string;
  provider: string;
}

export function useCurrencies() {
  return useQuery<Currency[]>({
    queryKey: ['currencies'],
    queryFn: async () => {
      const response = await api.get('/api/v1/currencies');
      return response.data;
    },
    staleTime: 24 * 60 * 60 * 1000, // 24 hours - currencies rarely change
  });
}

export function useCurrency(code: string) {
  return useQuery<Currency>({
    queryKey: ['currencies', code],
    queryFn: async () => {
      const response = await api.get(`/api/v1/currencies/${code}`);
      return response.data;
    },
    enabled: !!code,
    staleTime: 24 * 60 * 60 * 1000,
  });
}

export function useConvertCurrency(
  amount: number | undefined,
  fromCurrency: string | undefined,
  toCurrency: string | undefined,
  date?: string
) {
  return useQuery<ConversionResult>({
    queryKey: ['currency-convert', amount, fromCurrency, toCurrency, date],
    queryFn: async () => {
      const params = new URLSearchParams({
        amount: amount!.toString(),
        from: fromCurrency!,
        to: toCurrency!,
      });
      if (date) {
        params.append('date', date);
      }
      const response = await api.get(`/api/v1/currencies/convert?${params}`);
      return response.data;
    },
    enabled: !!amount && !!fromCurrency && !!toCurrency && fromCurrency !== toCurrency,
    staleTime: 60 * 60 * 1000, // 1 hour
  });
}

export function useExchangeRates(baseCurrency: string, date?: string) {
  return useQuery({
    queryKey: ['exchange-rates', baseCurrency, date],
    queryFn: async () => {
      const params = new URLSearchParams({ base: baseCurrency });
      if (date) {
        params.append('date', date);
      }
      const response = await api.get(`/api/v1/currencies/rates?${params}`);
      return response.data;
    },
    enabled: !!baseCurrency,
    staleTime: 60 * 60 * 1000,
  });
}
