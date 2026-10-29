import type { ReactNode } from 'react'
import { cn } from '@/lib/utils'

interface CardProps {
  children: ReactNode
  className?: string
}

export function Card({ children, className }: CardProps) {
  return (
    <div className={cn('rounded-md border border-border bg-white p-6', className)}>{children}</div>
  )
}

export function CardHeader({ children, className }: CardProps) {
  return <div className={cn('mb-4', className)}>{children}</div>
}

export function CardTitle({ children, className }: CardProps) {
  return (
    <h3 className={cn('text-lg font-semibold font-display text-text', className)}>{children}</h3>
  )
}

export function CardContent({ children, className }: CardProps) {
  return <div className={cn(className)}>{children}</div>
}
