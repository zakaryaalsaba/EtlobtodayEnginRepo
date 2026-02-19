# Restaurant Website Builder Engine

A complete restaurant website builder engine built with Vue.js and MySQL. Create beautiful, customizable restaurant websites with configurable logos, names, colors, and branding.

## Features

- 🎨 **Customizable Branding**: Configure restaurant name, logo, colors, and fonts
- 📱 **Responsive Design**: Beautiful, mobile-friendly website templates
- 🖼️ **Logo Upload**: Upload and manage restaurant logos
- 🎨 **Color Customization**: Choose primary and secondary colors
- 📝 **Contact Information**: Add address, phone, email, and website URL
- 📋 **Menu Management**: Add menu items (JSON format)
- 🔗 **Social Links**: Add social media links
- 👁️ **Live Preview**: Preview your website before publishing
- 💾 **MySQL Database**: Persistent storage with MySQL

## Tech Stack

- **Frontend**: Vue.js 3 + Vite + Tailwind CSS
- **Backend**: Node.js + Express
- **Database**: MySQL
- **File Upload**: Multer

## Project Structure

```
RestaurantEngin/
├── backend/
│   ├── db/
│   │   ├── init.js          # MySQL connection and initialization
│   │   └── schema.sql       # Database schema
│   ├── routes/
│   │   └── websites.js      # API routes for website CRUD
│   ├── uploads/             # Uploaded logo files
│   ├── server.js            # Express server
│   └── package.json
├── frontend/
│   ├── src/
│   │   ├── components/
│   │   │   ├── WebsiteBuilder.vue  # Builder interface
│   │   │   └── WebsiteViewer.vue   # Website display
│   │   ├── services/
│   │   │   └── api.js       # API client
│   │   ├── App.vue
│   │   └── main.js
│   └── package.json
└── README.md
```

## Setup Instructions

### Prerequisites

- Node.js 18+ and npm
- MySQL 8.0+
- OpenAI API key (for menu image processing)

### 1. Database Setup

```bash
# Connect to MySQL
mysql -u root -p

# Create database
CREATE DATABASE restaurant_websites;

# Exit MySQL
exit;
```

### 2. Backend Setup

```bash
cd backend

# Install dependencies
npm install

# Create .env file
cp .env.example .env

# Edit .env with your MySQL credentials and OpenAI API key:
# MYSQL_HOST=localhost
# MYSQL_PORT=3306
# MYSQL_DB=restaurant_websites
# MYSQL_USER=root
# MYSQL_PASSWORD=your_password
# PORT=3000
# OPENAI_API_KEY=your_openai_api_key_here

# Start server (database schema will be created automatically)
npm start

# Or for development with auto-reload:
npm run dev
```

The backend will automatically create the database schema on first run.

### 3. Frontend Setup

```bash
cd frontend

# Install dependencies
npm install

# Start development server
npm run dev
```

The frontend will be available at `http://localhost:5173`

## API Endpoints

### GET `/api/websites`
Get all restaurant websites

### GET `/api/websites/:id`
Get a specific restaurant website

### POST `/api/websites`
Create a new restaurant website

**Request Body:**
```json
{
  "restaurant_name": "My Restaurant",
  "description": "A great restaurant",
  "address": "123 Main St",
  "phone": "(555) 123-4567",
  "email": "contact@restaurant.com",
  "primary_color": "#4F46E5",
  "secondary_color": "#7C3AED",
  "is_published": false
}
```

### PUT `/api/websites/:id`
Update a restaurant website

### DELETE `/api/websites/:id`
Delete a restaurant website

### POST `/api/websites/:id/logo`
Upload logo for a restaurant website

**Form Data:**
- `logo`: Image file (PNG, JPG, GIF, SVG, WebP up to 5MB)

### POST `/api/websites/:id/menu-image`
Upload menu image and extract menu items using AI

**Form Data:**
- `menu_image`: Image file (PNG, JPG, WebP up to 10MB)

**Response:**
```json
{
  "website": {...},
  "menuItems": [...],
  "itemsExtracted": 15
}
```

## Usage

1. **Create a Website**: Fill out the form with restaurant details, upload a logo, and customize colors
2. **Preview**: Click "Preview Website" to see how it looks
3. **Publish**: Toggle "Publish Website" to make it live
4. **View**: Click "View" on any website card to see the generated website
5. **Edit**: Click "Edit" to modify an existing website
6. **Delete**: Click "Delete" to remove a website

## Database Schema

The `restaurant_websites` table stores:
- Basic info: name, description, logo
- Contact: address, phone, email, website URL
- Branding: primary color, secondary color, font family
- Content: menu items (JSON), social links (JSON)
- Status: published/draft flag
- Timestamps: created_at, updated_at

## Environment Variables

### Backend (.env)
- `MYSQL_HOST` - MySQL host (default: localhost)
- `MYSQL_PORT` - MySQL port (default: 3306)
- `MYSQL_DB` - Database name (default: restaurant_websites)
- `MYSQL_USER` - MySQL user (default: root)
- `MYSQL_PASSWORD` - MySQL password
- `PORT` - Server port (default: 3000)
- `OPENAI_API_KEY` - OpenAI API key (required for menu image processing)

## Development

### Backend Development
```bash
cd backend
npm run dev  # Auto-reload on file changes
```

### Frontend Development
```bash
cd frontend
npm run dev  # Vite dev server with hot reload
```

### Build for Production
```bash
cd frontend
npm run build  # Creates dist/ folder
```

## Troubleshooting

### Database Connection Issues
- Ensure MySQL is running: `mysql -u root -p`
- Check credentials in `backend/.env`
- Verify database exists: `SHOW DATABASES;`

### Port Already in Use
- Backend: Change `PORT` in `.env` (default: 3000)
- Frontend: Change port in `vite.config.js` (default: 5173)

### File Upload Issues
- Ensure `backend/uploads/` directory exists
- Check file size (max 5MB)
- Verify file type (images only)

## License

ISC

