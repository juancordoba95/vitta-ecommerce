import type { NextConfig } from "next";

type RemotePattern = NonNullable<NonNullable<NextConfig["images"]>["remotePatterns"]>[number];

/**
 * Product images: Cloudinary in production. The backend's local storage (served from
 * BACKEND_URL under /uploads) is only whitelisted in development.
 */
function localBackendPattern(): RemotePattern[] {
  if (process.env.NODE_ENV === "production" || !process.env.BACKEND_URL) return [];
  try {
    const { protocol, hostname, port } = new URL(process.env.BACKEND_URL);
    return [{ protocol: protocol === "https:" ? "https" : "http", hostname, port, pathname: "/uploads/**" }];
  } catch {
    return []; // malformed URL: the app reports it at request time
  }
}

const nextConfig: NextConfig = {
  images: {
    remotePatterns: [
      { protocol: "https", hostname: "res.cloudinary.com", pathname: "/**" },
      ...localBackendPattern(),
    ],
  },
  cacheComponents: true,
  partialPrefetching: true,
  turbopack: {
    rules: {
      "*.css": {
        loaders: ["@tailwindcss/turbopack"],
        as: "*.css",
      },
    },
  },
};

export default nextConfig;
