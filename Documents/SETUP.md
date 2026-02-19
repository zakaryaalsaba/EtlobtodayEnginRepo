# Quick Setup Guide

## Step-by-Step Setup

### 1. Install MySQL

**macOS:**
```bash
brew install mysql
brew services start mysql
```

**Linux (Ubuntu/Debian):**
```bash
sudo apt-get install mysql-server
sudo systemctl start mysql
```

**Windows:**
Download and install from https://dev.mysql.com/downloads/mysql/

### 2. Create Database

```bash
# Connect to MySQL
mysql -u root -p

# Create database
CREATE DATABASE restaurant_websites;

# Exit MySQL
exit;
```

### 3. Backend Setup

```bash
cd backend

# Install dependencies
npm install

# Create .env file
cp .env.example .env

# Edit .env with your MySQL credentials:
# MYSQL_HOST=localhost
# MYSQL_PORT=3306
# MYSQL_DB=restaurant_websites
# MYSQL_USER=root
# MYSQL_PASSWORD=your_mysql_password
# PORT=3000

# Start backend server
npm start
```

The server will automatically create all database tables on first run.

### 4. Frontend Setup

```bash
# In a new terminal
cd frontend

# Install dependencies
npm install

# Start development server
npm run dev
```

### 5. Access Application

Open your browser to: `http://localhost:5173`

## Testing

1. **Create a Website**: Fill out the form with restaurant name, upload a logo, and customize colors
2. **Preview**: Click "Preview Website" to see the generated website
3. **View**: Click "View" on any website card to see the full website

## Troubleshooting

### MySQL Connection Error
- Ensure MySQL is running: `mysql -u root -p`
- Check credentials in `backend/.env`
- Verify database exists: `SHOW DATABASES;`

### Port Already in Use
- Backend: Change `PORT` in `.env` (default: 3000)
- Frontend: Change port in `vite.config.js` (default: 5173)

### Module Not Found Errors
- Run `npm install` in both `backend/` and `frontend/` directories
- Delete `node_modules` and `package-lock.json`, then reinstall

### File Upload Issues
- Ensure `backend/uploads/` directory exists (will be created automatically)
- Check file size (max 5MB)
- Verify file type (images only: PNG, JPG, GIF, SVG, WebP)

## Environment Variables Reference

### Required
- `MYSQL_PASSWORD` - MySQL root password (required)

### Optional
- `MYSQL_HOST` - Database host (default: localhost)
- `MYSQL_PORT` - Database port (default: 3306)
- `MYSQL_DB` - Database name (default: restaurant_websites)
- `MYSQL_USER` - Database user (default: root)
- `PORT` - Backend port (default: 3000)

