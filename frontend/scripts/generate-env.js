const { writeFileSync, mkdirSync } = require('fs');
const { resolve } = require('path');

// In production the API is reached through the Vercel rewrite (/api/* -> backend), so the default is a
// relative URL. This keeps the session cookie first-party. Override it with the API_BASE_URL variable.
const apiBaseUrl = process.env.API_BASE_URL || '/api/v1';

const environmentContent = `export const environment = {
  production: true,
  apiBaseUrl: '${apiBaseUrl}'
};
`;

const envDir = resolve(__dirname, '../src/environments');
mkdirSync(envDir, { recursive: true });
writeFileSync(resolve(envDir, 'environment.ts'), environmentContent);

console.log('✓ environment.ts generado exitosamente con API_BASE_URL:', apiBaseUrl);
console.log('Archivo guardado en:', resolve(envDir, 'environment.ts'));
