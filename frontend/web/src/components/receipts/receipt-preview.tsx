"use client";

import { Receipt, Calendar, Store, DollarSign, Clock, CheckCircle2, XCircle, Loader2 } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Separator } from "@/components/ui/separator";
import { cn } from "@/lib/utils";
import { formatCurrency, formatDate } from "@/lib/utils";

interface LineItem {
  description: string;
  quantity: number;
  unitPrice: number;
  totalPrice: number;
}

interface ParsedReceipt {
  merchantName?: string;
  merchantAddress?: string;
  date?: string;
  time?: string;
  subtotal?: number;
  tax?: number;
  tip?: number;
  total?: number;
  currency?: string;
  items?: LineItem[];
  paymentMethod?: string;
  confidence?: number;
}

interface ReceiptPreviewProps {
  receipt: {
    id: string;
    status: "UPLOADED" | "PROCESSING" | "PARSED" | "FAILED";
    originalFilename: string;
    uploadedAt: string;
    processedAt?: string;
    parsedData?: ParsedReceipt;
    errorMessage?: string;
  };
  imageUrl?: string;
  className?: string;
}

export function ReceiptPreview({ receipt, imageUrl, className }: ReceiptPreviewProps) {
  const { status, parsedData, errorMessage } = receipt;

  return (
    <Card className={cn("w-full", className)}>
      <CardHeader className="pb-2">
        <div className="flex items-center justify-between">
          <CardTitle className="text-lg flex items-center gap-2">
            <Receipt className="h-5 w-5" />
            Receipt
          </CardTitle>
          <StatusBadge status={status} />
        </div>
      </CardHeader>
      <CardContent className="space-y-4">
        {/* Image preview */}
        {imageUrl && (
          <div className="relative aspect-[3/4] max-h-48 bg-gray-100 rounded-lg overflow-hidden">
            <img
              src={imageUrl}
              alt="Receipt"
              className="w-full h-full object-contain"
            />
          </div>
        )}

        {/* Processing status */}
        {status === "PROCESSING" && (
          <div className="flex items-center justify-center gap-2 py-4 text-muted-foreground">
            <Loader2 className="h-5 w-5 animate-spin" />
            <span>Processing receipt...</span>
          </div>
        )}

        {/* Error message */}
        {status === "FAILED" && errorMessage && (
          <div className="bg-destructive/10 text-destructive rounded-lg p-3 text-sm">
            {errorMessage}
          </div>
        )}

        {/* Parsed data */}
        {status === "PARSED" && parsedData && (
          <ParsedReceiptView data={parsedData} />
        )}

        {/* Metadata */}
        <div className="text-xs text-muted-foreground">
          <p>Uploaded: {formatDate(receipt.uploadedAt)}</p>
          {receipt.processedAt && (
            <p>Processed: {formatDate(receipt.processedAt)}</p>
          )}
        </div>
      </CardContent>
    </Card>
  );
}

function StatusBadge({ status }: { status: string }) {
  const config = {
    UPLOADED: { label: "Uploaded", variant: "secondary" as const, icon: Clock },
    PROCESSING: { label: "Processing", variant: "secondary" as const, icon: Loader2 },
    PARSED: { label: "Parsed", variant: "default" as const, icon: CheckCircle2 },
    FAILED: { label: "Failed", variant: "destructive" as const, icon: XCircle },
  };

  const { label, variant, icon: Icon } = config[status as keyof typeof config] || config.UPLOADED;

  return (
    <Badge variant={variant} className="gap-1">
      <Icon className={cn("h-3 w-3", status === "PROCESSING" && "animate-spin")} />
      {label}
    </Badge>
  );
}

function ParsedReceiptView({ data }: { data: ParsedReceipt }) {
  const currency = data.currency || "USD";

  return (
    <div className="space-y-4">
      {/* Merchant */}
      {data.merchantName && (
        <div className="flex items-start gap-2">
          <Store className="h-4 w-4 mt-0.5 text-muted-foreground" />
          <div>
            <p className="font-medium">{data.merchantName}</p>
            {data.merchantAddress && (
              <p className="text-sm text-muted-foreground">{data.merchantAddress}</p>
            )}
          </div>
        </div>
      )}

      {/* Date */}
      {data.date && (
        <div className="flex items-center gap-2">
          <Calendar className="h-4 w-4 text-muted-foreground" />
          <span>
            {data.date}
            {data.time && ` at ${data.time}`}
          </span>
        </div>
      )}

      {/* Line items */}
      {data.items && data.items.length > 0 && (
        <>
          <Separator />
          <div className="space-y-2">
            <p className="font-medium text-sm">Items</p>
            {data.items.map((item, index) => (
              <div key={index} className="flex justify-between text-sm">
                <span className="flex-1">
                  {item.quantity > 1 && `${item.quantity}x `}
                  {item.description}
                </span>
                <span className="text-muted-foreground ml-2">
                  {formatCurrency(item.totalPrice, currency)}
                </span>
              </div>
            ))}
          </div>
        </>
      )}

      {/* Totals */}
      <Separator />
      <div className="space-y-1">
        {data.subtotal !== undefined && (
          <div className="flex justify-between text-sm">
            <span className="text-muted-foreground">Subtotal</span>
            <span>{formatCurrency(data.subtotal, currency)}</span>
          </div>
        )}
        {data.tax !== undefined && (
          <div className="flex justify-between text-sm">
            <span className="text-muted-foreground">Tax</span>
            <span>{formatCurrency(data.tax, currency)}</span>
          </div>
        )}
        {data.tip !== undefined && (
          <div className="flex justify-between text-sm">
            <span className="text-muted-foreground">Tip</span>
            <span>{formatCurrency(data.tip, currency)}</span>
          </div>
        )}
        {data.total !== undefined && (
          <div className="flex justify-between font-medium">
            <span>Total</span>
            <span>{formatCurrency(data.total, currency)}</span>
          </div>
        )}
      </div>

      {/* Confidence score */}
      {data.confidence !== undefined && (
        <div className="text-xs text-muted-foreground text-right">
          Confidence: {Math.round(data.confidence * 100)}%
        </div>
      )}
    </div>
  );
}
