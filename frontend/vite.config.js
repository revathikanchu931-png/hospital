import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
const githubPages = process.env.GITHUB_PAGES === 'true';

export default defineConfig({
  base: githubPages ? '/hospital/' : '/',
  plugins: [react()],
  server: { port: 5173 },
  test: { environment: 'jsdom', setupFiles: './src/testSetup.js' },
});
