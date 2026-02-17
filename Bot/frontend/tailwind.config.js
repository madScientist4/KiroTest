/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        // FNB Brand Colors
        fnb: {
          primary: '#005EB8',    // FNB Blue
          secondary: '#00A3E0',  // Light Blue
          accent: '#FFB81C',     // Gold/Yellow
          dark: '#002855',       // Dark Blue
          light: '#E6F2FF',      // Very Light Blue
          success: '#00A651',    // Green
          error: '#E31837',      // Red
          warning: '#FF6900',    // Orange
          gray: {
            50: '#F9FAFB',
            100: '#F3F4F6',
            200: '#E5E7EB',
            300: '#D1D5DB',
            400: '#9CA3AF',
            500: '#6B7280',
            600: '#4B5563',
            700: '#374151',
            800: '#1F2937',
            900: '#111827',
          }
        }
      }
    },
  },
  plugins: [],
}
