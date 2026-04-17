module.exports = {
  preset: 'jest-preset-angular',
  setupFilesAfterEnv: ['<rootDir>/setup-jest.ts'],
  testEnvironment: 'jsdom',
  transform: { '^.+\\.(ts|js|mjs|html)$': ['jest-preset-angular', { tsconfig: '<rootDir>/tsconfig.spec.json' }] },
  testMatch: ['**/*.spec.ts'],
  collectCoverageFrom: ['src/**/*.ts', '!src/**/*.spec.ts', '!src/main.ts']
};
