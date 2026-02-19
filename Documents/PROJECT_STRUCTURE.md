# Project Structure

## Overview

This is a complete restaurant website builder engine that allows users to create customizable restaurant websites with configurable logos, names, colors, and branding.

## Directory Structure

```
RestaurantEngin/
├── backend/                    # Node.js/Express backend
│   ├── db/
│   │   ├── init.js             # MySQL connection pool and initialization
│   │   └── schema.sql          # Database schema (creates restaurant_websites table)
│   ├── routes/
│   │   └── websites.js         # API routes for CRUD operations
│   ├── uploads/                # Directory for uploaded logo files (auto-created)
│   ├── server.js               # Express server entry point
│   ├── package.json            # Backend dependencies
│   └── .env.example            # Environment variables template
│
├── frontend/                   # Vue.js frontend
│   ├── src/
│   │   ├── components/
│   │   │   ├── WebsiteBuilder.vue   # Main builder interface
│   │   │   └── WebsiteViewer.vue    # Website display component
│   │   ├── services/
│   │   │   └── api.js          # API client functions
│   │   ├── App.vue             # Root Vue component with router
│   │   ├── main.js             # Vue app entry point
│   │   └── style.css           # Global styles with Tailwind
│   ├── index.html              # HTML entry point
│   ├── vite.config.js          # Vite configuration
│   ├── tailwind.config.js      # Tailwind CSS configuration
│   ├── postcss.config.js       # PostCSS configuration
│   └── package.json            # Frontend dependencies
│
├── README.md                   # Main documentation
├── SETUP.md                    # Quick setup guide
└── .gitignore                  # Git ignore rules

```

## Key Features

### Backend (Express + MySQL)

1. **Database Layer** (`backend/db/`)
   - MySQL connection pool
   - Automatic schema initialization
   - Database creation if it doesn't exist

2. **API Routes** (`backend/routes/websites.js`)
   - `GET /api/websites` - List all websites
   - `GET /api/websites/:id` - Get specific website
   - `POST /api/websites` - Create new website
   - `PUT /api/websites/:id` - Update website
   - `DELETE /api/websites/:id` - Delete website
   - `POST /api/websites/:id/logo` - Upload logo

3. **File Upload**
   - Multer middleware for logo uploads
   - Stores files in `backend/uploads/`
   - Supports PNG, JPG, GIF, SVG, WebP
   - 5MB file size limit

### Frontend (Vue.js + Tailwind)

1. **WebsiteBuilder Component**
   - Form for creating/editing websites
   - Logo upload with preview
   - Color picker for branding
   - List of existing websites
   - Edit/Delete/View actions

2. **WebsiteViewer Component**
   - Displays generated website
   - Responsive design
   - Shows logo, name, description
   - Contact information section
   - Menu items display (if provided)
   - Social links (if provided)

3. **Routing**
   - `/` - Builder interface
   - `/website/:id` - View generated website

## Database Schema

### restaurant_websites Table

| Column | Type | Description |
|--------|------|-------------|
| id | INT | Primary key (auto-increment) |
| restaurant_name | VARCHAR(255) | Restaurant name (required) |
| logo_url | TEXT | URL to logo image |
| logo_file_path | VARCHAR(500) | Local file path |
| description | TEXT | Restaurant description |
| address | TEXT | Physical address |
| phone | VARCHAR(50) | Phone number |
| email | VARCHAR(255) | Email address |
| website_url | VARCHAR(500) | External website URL |
| primary_color | VARCHAR(7) | Primary brand color (hex) |
| secondary_color | VARCHAR(7) | Secondary brand color (hex) |
| font_family | VARCHAR(100) | Font family name |
| custom_css | TEXT | Custom CSS (future feature) |
| menu_items | JSON | Menu items array |
| social_links | JSON | Social media links object |
| is_published | BOOLEAN | Published status |
| created_at | TIMESTAMP | Creation timestamp |
| updated_at | TIMESTAMP | Last update timestamp |

## API Data Flow

1. **Create Website**
   ```
   Frontend → POST /api/websites → MySQL → Response with website ID
   ```

2. **Upload Logo**
   ```
   Frontend → POST /api/websites/:id/logo (multipart/form-data) 
   → Multer saves file → MySQL updates logo_url → Response
   ```

3. **View Website**
   ```
   Frontend → GET /api/websites/:id → MySQL → Response with website data
   → WebsiteViewer component renders
   ```

## Technology Stack

- **Backend**: Node.js, Express, MySQL2, Multer
- **Frontend**: Vue.js 3, Vite, Tailwind CSS, Vue Router
- **Database**: MySQL 8.0+
- **File Storage**: Local filesystem (uploads directory)

## Next Steps for Enhancement

1. **Menu Management UI**: Add interface for managing menu items
2. **Social Links UI**: Add interface for social media links
3. **Custom CSS Editor**: Add code editor for custom CSS
4. **Multiple Templates**: Add different website templates
5. **Image Gallery**: Add photo gallery section
6. **Online Ordering**: Integrate ordering system
7. **Reservations**: Add reservation booking
8. **Analytics**: Track website views and interactions

