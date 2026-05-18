import type { Config } from 'tailwindcss';

const config: Config = {
  darkMode: ['class'],
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        border: 'hsl(var(--border))',
        background: 'hsl(var(--background))',
        foreground: 'hsl(var(--foreground))',
        primary: 'hsl(var(--primary))',
        muted: 'hsl(var(--muted))',
        accent: 'hsl(var(--accent))',
        success: '#16a34a',
        warning: '#d97706',
        danger: '#dc2626',
      },
      boxShadow: {
        soft: '0 18px 60px rgba(15, 23, 42, 0.10)',
      },
    },
  },
  plugins: [require('tailwindcss-animate')],
};

export default config;
