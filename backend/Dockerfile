# RestaurantEngin Backend - production Docker image
# Use with DigitalOcean App Platform (Dockerfile deploy) or any container host

FROM node:20-alpine

WORKDIR /app

# Copy package files
COPY package*.json ./

# Install production dependencies only
RUN npm ci --only=production 2>/dev/null || npm install --omit=dev

# Copy application code
COPY . .

# Create uploads dir (backend serves /uploads and writes here)
RUN mkdir -p uploads uploads/menu-extractor uploads/delivery-zones uploads/delivery-companies

# App Platform sets PORT=8080 at runtime; default 8080 for production
ENV PORT=8080
EXPOSE 8080

# Listen on all interfaces (required in containers)
CMD ["node", "server.js"]
