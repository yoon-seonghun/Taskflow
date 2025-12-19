/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        primary: {
          50: '#f0f9ff',
          100: '#e0f2fe',
          200: '#bae6fd',
          300: '#7dd3fc',
          400: '#38bdf8',
          500: '#0ea5e9',
          600: '#0284c7',
          700: '#0369a1',
          800: '#075985',
          900: '#0c4a6e',
        },
        // 상태 색상
        status: {
          'not-started': '#6b7280',
          'in-progress': '#3b82f6',
          'completed': '#22c55e',
          'deleted': '#ef4444',
        },
        // 우선순위 색상
        priority: {
          'urgent': '#ef4444',
          'high': '#f97316',
          'normal': '#6b7280',
          'low': '#9ca3af',
        },
      },
      fontSize: {
        'compact': '13px',
        'table': '14px',
      },
      spacing: {
        'row': '36px',
        'sidebar': '240px',
        'panel': '400px',
      },
      minHeight: {
        'row': '36px',
      },
      maxWidth: {
        'sidebar': '240px',
        'panel': '400px',
      },
      width: {
        'sidebar': '240px',
        'panel': '400px',
      },
      zIndex: {
        'sidebar': '40',
        'panel': '50',
        'modal': '60',
        'toast': '70',
        'tooltip': '80',
      },
      animation: {
        'slide-in-right': 'slideInRight 0.3s ease-out',
        'slide-out-right': 'slideOutRight 0.3s ease-out',
        'fade-in': 'fadeIn 0.2s ease-out',
        'fade-out': 'fadeOut 0.2s ease-out',
      },
      keyframes: {
        slideInRight: {
          '0%': { transform: 'translateX(100%)', opacity: '0' },
          '100%': { transform: 'translateX(0)', opacity: '1' },
        },
        slideOutRight: {
          '0%': { transform: 'translateX(0)', opacity: '1' },
          '100%': { transform: 'translateX(100%)', opacity: '0' },
        },
        fadeIn: {
          '0%': { opacity: '0' },
          '100%': { opacity: '1' },
        },
        fadeOut: {
          '0%': { opacity: '1' },
          '100%': { opacity: '0' },
        },
      },
    },
  },
  plugins: [],
}
