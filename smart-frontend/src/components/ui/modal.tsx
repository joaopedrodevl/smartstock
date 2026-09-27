import { X } from 'lucide-react'
import type { ReactNode } from 'react'
import { cn } from '@/lib/utils'

interface ModalProps {
  open: boolean
  onClose: () => void
  title: string
  children: ReactNode
  className?: string
}

export function Modal({ open, onClose, title, children, className }: ModalProps) {
  if (!open) return null

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      <div className="fixed inset-0 bg-black/40" onClick={onClose} />
      <div
        className={cn(
          'relative z-10 w-full max-w-lg rounded-md border border-border bg-white p-6 shadow-lg',
          className,
        )}
      >
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-lg font-semibold font-display">{title}</h2>
          <button
            onClick={onClose}
            className="rounded-md p-1 text-text-secondary hover:bg-surface hover:text-text"
          >
            <X size={18} />
          </button>
        </div>
        {children}
      </div>
    </div>
  )
}
