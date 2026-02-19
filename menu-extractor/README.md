# Menu Extractor - Restaurant Website Builder

A Vue.js application that extracts menu items from uploaded images and automatically creates restaurant websites with products in the database.

## Features

- 📸 **Drag & Drop Image Upload** - Easy image upload interface
- 🤖 **AI-Powered Extraction** - Uses OpenAI Vision API to extract menu items
- ✏️ **Product Review & Edit** - Review and edit extracted products before creation
- 🏪 **Auto Restaurant Creation** - Automatically creates restaurant website and inserts products
- 🌐 **Multi-language Support** - Supports Arabic and English product names

## Setup

1. **Install dependencies:**
   ```bash
   npm install
   ```

2. **Configure API URL:**
   Create a `.env` file:
   ```env
   VITE_API_BASE_URL=http://localhost:3000/api
   ```

3. **Start development server:**
   ```bash
   npm run dev
   ```

4. **Make sure backend is running:**
   - Backend should be running on `http://localhost:3000`
   - OpenAI API key should be configured in backend `.env` for menu extraction

## Usage

1. Upload menu images (drag & drop or click to browse)
2. Wait for AI to extract products from images
3. Review and edit extracted products
4. Fill in restaurant information
5. Click "Create Restaurant" to generate the website

## Requirements

- Backend server running with OpenAI API key configured
- Node.js 20+ or 22+

## API Endpoints

The app uses these backend endpoints:
- `POST /api/menu-extractor/process` - Process menu images
- `POST /api/menu-extractor/create` - Create restaurant with products
