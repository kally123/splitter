import { z } from "zod";

export const emailSchema = z
  .string()
  .min(1, "Email is required")
  .email("Invalid email address");

export const passwordSchema = z
  .string()
  .min(8, "Password must be at least 8 characters")
  .regex(
    /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)/,
    "Password must contain at least one uppercase letter, one lowercase letter, and one number"
  );

export const displayNameSchema = z
  .string()
  .min(2, "Name must be at least 2 characters")
  .max(100, "Name must be less than 100 characters");

export const loginSchema = z.object({
  email: emailSchema,
  password: z.string().min(1, "Password is required"),
});

export const registerSchema = z
  .object({
    email: emailSchema,
    password: passwordSchema,
    confirmPassword: z.string().min(1, "Please confirm your password"),
    displayName: displayNameSchema,
  })
  .refine((data) => data.password === data.confirmPassword, {
    message: "Passwords don't match",
    path: ["confirmPassword"],
  });

export const createGroupSchema = z.object({
  name: z
    .string()
    .min(1, "Group name is required")
    .max(100, "Group name must be less than 100 characters"),
  description: z.string().max(500, "Description must be less than 500 characters").optional(),
  groupType: z.enum(["HOME", "TRIP", "COUPLE", "FRIENDS", "FAMILY", "WORK", "OTHER"]),
  defaultCurrency: z.string().default("USD"),
  simplifyDebts: z.boolean().default(true),
});

export const createExpenseSchema = z.object({
  description: z
    .string()
    .min(1, "Description is required")
    .max(255, "Description must be less than 255 characters"),
  amount: z.number().positive("Amount must be greater than 0"),
  currency: z.string().default("USD"),
  groupId: z.string().uuid().optional(),
  paidBy: z.string().uuid("Please select who paid"),
  splitType: z.enum(["EQUAL", "EXACT", "PERCENTAGE", "SHARES"]),
  participants: z
    .array(
      z.object({
        userId: z.string().uuid(),
        amount: z.number().optional(),
        percentage: z.number().optional(),
        shares: z.number().optional(),
      })
    )
    .min(1, "At least one participant is required"),
  category: z.string(),
  expenseDate: z.date(),
  notes: z.string().max(1000, "Notes must be less than 1000 characters").optional(),
});

export const createSettlementSchema = z.object({
  toUserId: z.string().uuid("Please select a recipient"),
  amount: z.number().positive("Amount must be greater than 0"),
  paymentMethod: z.enum([
    "CASH",
    "BANK_TRANSFER",
    "VENMO",
    "PAYPAL",
    "ZELLE",
    "CREDIT_CARD",
    "CHECK",
    "OTHER",
  ]),
  notes: z.string().max(500, "Notes must be less than 500 characters").optional(),
});

export type LoginFormData = z.infer<typeof loginSchema>;
export type RegisterFormData = z.infer<typeof registerSchema>;
export type CreateGroupFormData = z.infer<typeof createGroupSchema>;
export type CreateExpenseFormData = z.infer<typeof createExpenseSchema>;
export type CreateSettlementFormData = z.infer<typeof createSettlementSchema>;
