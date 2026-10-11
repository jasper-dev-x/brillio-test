/**
 * Jest runs unit and integration tests for the React app in a jsdom browser environment.
 * TypeScript is compiled by ts-jest; types are checked separately by `npm run typecheck`.
 * @type {import('jest').Config}
 */
export default {
  testEnvironment: 'jsdom',
  roots: ['<rootDir>/src'],
  testMatch: ['**/*.test.ts', '**/*.test.tsx'],
  setupFilesAfterEnv: ['<rootDir>/src/test/setupTests.ts'],
  moduleNameMapper: {
    '\\.css$': '<rootDir>/src/test/styleMock.ts',
    // Source files import with explicit .ts/.tsx extensions (Vite style); strip for Jest resolution.
    '^(\\.{1,2}/.*)\\.tsx?$': '$1',
  },
  transform: {
    '^.+\\.tsx?$': [
      'ts-jest',
      {
        tsconfig: {
          jsx: 'react-jsx',
          module: 'commonjs',
          moduleResolution: 'node10',
          esModuleInterop: true,
          verbatimModuleSyntax: false,
          isolatedModules: true,
          ignoreDeprecations: '6.0',
        },
        diagnostics: false,
      },
    ],
  },
  collectCoverageFrom: ['src/**/*.{ts,tsx}', '!src/main.tsx', '!src/test/**', '!src/**/*.d.ts'],
}
