import { createClient } from 'redis';
import dotenv from 'dotenv';

dotenv.config();

const redisClient = createClient({
  url: process.env.REDIS_URL || 'redis://localhost:6379',
  socket: {
    reconnectStrategy: (retries) => {
      if (retries > 10) {
        console.error('Redis: Max reconnection attempts reached');
        return new Error('Redis connection failed');
      }
      return Math.min(retries * 100, 3000);
    }
  }
});

redisClient.on('error', (err) => {
  console.error('Redis Client Error:', err);
});

redisClient.on('connect', () => {
  console.log('✅ Redis Client Connected');
});

redisClient.on('ready', () => {
  console.log('✅ Redis Client Ready');
});

// Connect on module load (if not already connected)
let isConnected = false;

export async function connectRedis() {
  if (!process.env.REDIS_URL?.trim()) {
    return; // No Redis URL: skip connecting (avoids localhost:6379 in production)
  }
  if (!isConnected) {
    try {
      await redisClient.connect();
      isConnected = true;
    } catch (error) {
      console.warn('⚠️  Redis unavailable (continuing without cache):', error.message);
    }
  }
}

// Auto-connect only when REDIS_URL is set (avoid connecting to localhost in production)
if (process.env.REDIS_URL && process.env.REDIS_AUTO_CONNECT !== 'false') {
  connectRedis().catch(() => {});
}

export default redisClient;
