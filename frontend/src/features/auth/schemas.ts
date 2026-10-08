import { z } from 'zod'

// Mirrors the backend rules: names up to 100 characters, passwords 8–72 characters.
const email = z.string().trim().min(1, 'Email is required.').email('Enter a valid email address.').max(254)
const newPassword = z
  .string()
  .min(8, 'Password must be at least 8 characters.')
  .refine((value) => new TextEncoder().encode(value).length <= 72, 'Password must be at most 72 characters.')

export const loginSchema = z.object({
  email,
  password: z.string().min(1, 'Password is required.'),
})

export const registerSchema = z
  .object({
    name: z.string().trim().min(1, 'Full name is required.').max(100, 'Full name must be at most 100 characters.'),
    email,
    password: newPassword,
    confirmPassword: z.string().min(1, 'Please confirm your password.'),
  })
  .refine((values) => values.password === values.confirmPassword, {
    path: ['confirmPassword'],
    message: 'Passwords do not match.',
  })

export const forgotPasswordSchema = z.object({ email })

export const resetPasswordSchema = z
  .object({
    password: newPassword,
    confirmPassword: z.string().min(1, 'Please confirm your password.'),
  })
  .refine((values) => values.password === values.confirmPassword, {
    path: ['confirmPassword'],
    message: 'Passwords do not match.',
  })

export const changePasswordSchema = z
  .object({
    currentPassword: z.string().min(1, 'Current password is required.'),
    newPassword,
    confirmPassword: z.string().min(1, 'Please confirm your new password.'),
  })
  .refine((values) => values.newPassword === values.confirmPassword, {
    path: ['confirmPassword'],
    message: 'Passwords do not match.',
  })

export type LoginValues = z.infer<typeof loginSchema>
export type RegisterValues = z.infer<typeof registerSchema>
export type ForgotPasswordValues = z.infer<typeof forgotPasswordSchema>
export type ResetPasswordValues = z.infer<typeof resetPasswordSchema>
export type ChangePasswordValues = z.infer<typeof changePasswordSchema>
