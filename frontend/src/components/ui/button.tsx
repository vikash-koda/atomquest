import * as React from 'react';
import { cn } from '@/lib/utils';

type Props = React.ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: 'primary' | 'secondary' | 'ghost' | 'danger';
};

export function Button({ className, variant = 'primary', ...props }: Props) {
  return (
    <button
      className={cn(
        'inline-flex h-10 items-center justify-center gap-2 rounded-md px-4 text-sm font-semibold transition focus:outline-none focus:ring-2 focus:ring-primary disabled:opacity-50',
        variant === 'primary' && 'bg-primary text-white shadow-soft hover:brightness-95',
        variant === 'secondary' && 'border border-border bg-white/70 text-foreground hover:bg-muted dark:bg-white/5',
        variant === 'ghost' && 'text-foreground hover:bg-muted',
        variant === 'danger' && 'bg-danger text-white hover:brightness-95',
        className,
      )}
      {...props}
    />
  );
}
