import type { NextConfig } from "next";
import { PHASE_DEVELOPMENT_SERVER } from "next/constants";

const turbopack: NextConfig["turbopack"] = {
  rules: {
    "*.css": {
      loaders: ["@tailwindcss/turbopack"],
      as: "*.css",
    },
  },
};

// O site é estático (output: 'export'), servido pelo Caddy, que também leva /api até a API.
// Export estático não aceita rewrites, nem no `next dev`; por isso o proxy de /api para a API
// local só existe na fase de desenvolvimento, e o export só nas outras fases.
export default function nextConfig(phase: string): NextConfig {
  if (phase === PHASE_DEVELOPMENT_SERVER) {
    return {
      turbopack,
      async rewrites() {
        return [{ source: "/api/:path*", destination: "http://localhost:8081/:path*" }];
      },
    };
  }
  return { turbopack, output: "export" };
}
