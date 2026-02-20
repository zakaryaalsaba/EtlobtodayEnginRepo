import express from 'express';
import cors from 'cors';
import dotenv from 'dotenv';
import rateLimit from 'express-rate-limit';
import path from 'path';
import { fileURLToPath } from 'url';
import websiteRoutes from './routes/websites.js';
import productsRoutes from './routes/products.js';
import ordersRoutes from './routes/orders.js';
import adminRoutes from './routes/admin.js';
import customersRoutes from './routes/customers.js';
import addressesRoutes from './routes/addresses.js';
import restaurantRoutes from './routes/restaurant.js';
import applicationsRoutes from './routes/applications.js';
import registerRoutes from './routes/register.js';
import superAdminRoutes from './routes/superAdmin.js';
import superAdminDriversRoutes from './routes/superAdminDrivers.js';
import paymentsRoutes from './routes/payments.js';
import couponsRoutes from './routes/coupons.js';
import authRoutes from './routes/auth.js';
import testNotificationRoutes from './routes/test-notification.js';
import driversRoutes from './routes/drivers.js';
import driverOrdersRoutes from './routes/driverOrders.js';
import settingsRoutes from './routes/settings.js';
import deliveryCompaniesRoutes from './routes/deliveryCompanies.js';
import deliveryZonesRoutes from './routes/deliveryZones.js';
import deliveryCompanyDashboardRoutes from './routes/deliveryCompanyDashboard.js';
import menuExtractorRoutes from './routes/menuExtractor.js';
import { initDatabase, testConnection } from './db/init.js';
import { domainRouter } from './middleware/domainRouter.js';
import { connectRedis } from './services/redis.js';

dotenv.config();

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const app = express();
// Default to 8080 in production (DigitalOcean App Platform); 3000 for local dev
const PORT = process.env.PORT || (process.env.NODE_ENV === 'production' ? 8080 : 3000);

// Middleware
app.use(cors());
app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ extended: true }));

// Serve uploaded files
app.use('/uploads', express.static(path.join(__dirname, 'uploads')));

// Domain-based routing middleware (must be before other routes)
app.use(domainRouter);

// Rate limiting (more lenient for development)
const limiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: process.env.NODE_ENV === 'production' ? 100 : 1000, // Higher limit for development
  message: 'Too many requests from this IP, please try again later.',
  standardHeaders: true,
  legacyHeaders: false,
});
app.use('/api/', limiter);

// Routes
// IMPORTANT: Driver-specific routes must come BEFORE general orders routes
// so that authenticated driver requests are handled by driver-specific handlers
app.use('/api/websites', websiteRoutes);
app.use('/api/products', productsRoutes);
app.use('/api/orders', driverOrdersRoutes); // Driver routes first (requires authentication)
app.use('/api/orders', ordersRoutes); // General orders routes (fallback)
app.use('/api/admin', adminRoutes);
app.use('/api/customers/:customerId/addresses', addressesRoutes);
app.use('/api/customers', customersRoutes);
app.use('/api/restaurant', restaurantRoutes);
app.use('/api/applications', applicationsRoutes);
app.use('/api/register', registerRoutes);
app.use('/api/super-admin', superAdminRoutes);
app.use('/api/super-admin', superAdminDriversRoutes);
app.use('/api/payments', paymentsRoutes);
app.use('/api/coupons', couponsRoutes);
app.use('/api/auth', authRoutes);
app.use('/api/test-notification', testNotificationRoutes);
app.use('/api/drivers', driversRoutes);
app.use('/api/settings', settingsRoutes);
app.use('/api/delivery-companies', deliveryCompaniesRoutes);
app.use('/api/delivery-companies', deliveryZonesRoutes); // Routes like /api/delivery-companies/:companyId/zones
app.use('/api/delivery-zones', deliveryZonesRoutes); // Routes like /api/delivery-zones/:id
app.use('/api/delivery-company', deliveryCompanyDashboardRoutes); // Delivery company admin dashboard
app.use('/api/menu-extractor', menuExtractorRoutes); // Menu extractor tool

// Health check
app.get('/health', (req, res) => {
  res.json({ status: 'ok', timestamp: new Date().toISOString() });
});

// Initialize database and start server
initDatabase()
  .then(() => {
    console.log('✅ Database schema initialized');
    return testConnection();
  })
  .then(() => {
    console.log('✅ Database connection test passed');
    // Redis is optional - don't fail startup if it's unavailable
    return connectRedis().catch((err) => {
      console.warn('⚠️  Redis connection failed (continuing without cache):', err.message);
      return Promise.resolve(); // Continue even if Redis fails
    });
  })
  .then(() => {
    const apiBaseUrl = process.env.API_BASE_URL || `http://localhost:${PORT}`;
    const androidEmulatorUrl = process.env.ANDROID_EMULATOR_URL || `http://10.0.2.2:${PORT}`;
    
    app.listen(PORT, '0.0.0.0', () => {
      console.log(`✅ Server running on port ${PORT}`);
      console.log(`Health check: ${apiBaseUrl}/health`);
      console.log(`API Base URL: ${apiBaseUrl}`);
      if (androidEmulatorUrl !== apiBaseUrl) {
        console.log(`Server accessible from emulator at: ${androidEmulatorUrl}`);
      }
    });
  })
  .catch((error) => {
    console.error('❌ Failed to start server:', error);
    console.error('Error details:', {
      message: error.message,
      stack: error.stack,
      code: error.code,
      errno: error.errno,
      sqlState: error.sqlState
    });
    console.error('Environment check:', {
      MYSQL_HOST: process.env.MYSQL_HOST ? '✅ Set' : '❌ Missing',
      MYSQL_PORT: process.env.MYSQL_PORT || 'Using default 3306',
      MYSQL_DB: process.env.MYSQL_DB || 'Using default restaurant_websites',
      MYSQL_USER: process.env.MYSQL_USER ? '✅ Set' : '❌ Missing',
      MYSQL_PASSWORD: process.env.MYSQL_PASSWORD ? '✅ Set' : '❌ Missing',
      MYSQL_SSL_MODE: process.env.MYSQL_SSL_MODE || 'Not set',
      PORT: process.env.PORT || PORT
    });
    process.exit(1);
  });

