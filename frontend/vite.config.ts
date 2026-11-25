import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import electron from 'vite-plugin-electron'
import renderer from 'vite-plugin-electron-renderer'
import { resolve } from 'path'

// vite-plugin-electron 1.x adds Rolldown's `platform` option even when running
// against Vite 6/Rollup. Rollup does not recognize it, so remove only that
// compatibility-only option before the Electron bundles are built.
const stripUnsupportedRollupPlatform = () => ({
  name: 'strip-unsupported-rollup-platform',
  configResolved(config: any) {
    if (config.build?.rollupOptions) {
      delete config.build.rollupOptions.platform
    }
  }
})

export default defineConfig({
  plugins: [
    vue(),
    electron([
      {
        entry: 'electron/main.ts',
        vite: {
          plugins: [stripUnsupportedRollupPlatform()],
          build: {
            outDir: 'dist-electron',
            rollupOptions: {
              external: ['electron']
            }
          }
        }
      },
      {
        entry: 'electron/preload.ts',
        onstart(args) {
          args.reload()
        },
        vite: {
          plugins: [stripUnsupportedRollupPlatform()],
          build: {
            outDir: 'dist-electron',
            rollupOptions: {
              external: ['electron']
            }
          }
        }
      }
    ]),
    renderer()
  ],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  build: {
    outDir: 'dist',
    emptyOutDir: true
  }
})
