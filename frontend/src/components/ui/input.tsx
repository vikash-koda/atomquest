import * as React from 'react';
import { cn } from '@/lib/utils';

export const Input = React.forwardRef<HTMLInputElement, React.InputHTMLAttributes<HTMLInputElement>>(
  ({ className, ...props }, ref) => (
    <input
      ref={ref}
      {...props}
      className={cn('h-10 w-full rounded-md border border-border bg-white/80 px-3 text-sm outline-none transition focus:border-primary dark:bg-white/5', className)}
    />
  ),
);
Input.displayName = 'Input';

export const Textarea = React.forwardRef<HTMLTextAreaElement, React.TextareaHTMLAttributes<HTMLTextAreaElement>>(
  ({ className, ...props }, ref) => (
    <textarea
      ref={ref}
      {...props}
      className={cn('min-h-24 w-full rounded-md border border-border bg-white/80 px-3 py-2 text-sm outline-none transition focus:border-primary dark:bg-white/5', className)}
    />
  ),
);
Textarea.displayName = 'Textarea';

export const Select = React.forwardRef<HTMLSelectElement, React.SelectHTMLAttributes<HTMLSelectElement>>(
  ({ className, ...props }, ref) => (
    <select
      ref={ref}
      {...props}
      className={cn('h-10 w-full rounded-md border border-border bg-white/80 px-3 text-sm outline-none transition focus:border-primary dark:bg-white/5', className)}
    />
  ),
);
Select.displayName = 'Select';
