import { defineConfig } from 'cypress'

/**
 * Cypress component testing: mounts real React components (styled with Tailwind/daisyUI)
 * in a real browser via Vite. Full-stack end-to-end tests live in Playwright (./e2e).
 */
export default defineConfig({
  component: {
    devServer: {
      framework: 'react',
      bundler: 'vite',
    },
    specPattern: 'cypress/component/**/*.cy.{ts,tsx}',
    supportFile: 'cypress/support/component.tsx',
    indexHtmlFile: 'cypress/support/component-index.html',
    viewportWidth: 1280,
    viewportHeight: 800,
  },
  video: false,
  screenshotOnRunFailure: true,
})
