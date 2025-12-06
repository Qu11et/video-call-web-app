import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { nodePolyfills } from 'vite-plugin-node-polyfills'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    react(),
    // Thêm plugin này để simple-peer hoạt động được trên Vite
    nodePolyfills({
      // SỬA LỖI: Cấu trúc đúng là đặt các biến vào trong object 'globals'
      globals: {
        Buffer: true,
        global: true,
        process: true,
      },
      protocolImports: true,
    }),
  ],
  define: {
    // Đảm bảo biến global tồn tại (fallback)
    'global': 'window',
  }
})
