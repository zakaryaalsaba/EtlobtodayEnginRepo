import dotenv from 'dotenv';
import fs from 'fs';

dotenv.config();

// Cache for OpenAI instance
let openai = null;
let openaiAvailable = false;
let openaiCheckDone = false;

/**
 * Check if OpenAI is available and initialize it
 */
async function checkOpenAIAvailability() {
  if (openaiCheckDone) {
    return openaiAvailable;
  }
  
  openaiCheckDone = true;
  
  try {
    if (process.env.OPENAI_API_KEY) {
      const { default: OpenAI } = await import('openai');
      openai = new OpenAI({
        apiKey: process.env.OPENAI_API_KEY,
      });
      openaiAvailable = true;
      console.log('OpenAI initialized for menu processing');
      return true;
    }
  } catch (error) {
    console.warn('OpenAI package not installed or not configured. Menu image processing will be limited.');
    console.warn('To enable AI menu extraction, install: npm install openai');
    openaiAvailable = false;
    return false;
  }
  
  return false;
}

/**
 * Process menu image and extract menu items using OpenAI Vision API
 * @param {string} imagePath - Path to the uploaded menu image
 * @param {string} restaurantName - Name of the restaurant
 * @returns {Promise<Array>} Array of menu items with name, description, and price
 */
export async function processMenuImage(imagePath, restaurantName) {
  try {
    // Check if OpenAI is available
    const isAvailable = await checkOpenAIAvailability();
    
    // If OpenAI is not available, return empty array (user can add items manually)
    if (!isAvailable || !openai || !process.env.OPENAI_API_KEY) {
      console.log('OpenAI not configured. Menu image saved but items not extracted. User can add items manually.');
      return [];
    }

    // Read the image file and convert to base64
    const imageBuffer = fs.readFileSync(imagePath);
    const base64Image = imageBuffer.toString('base64');
    
    // Determine image MIME type
    const ext = imagePath.split('.').pop().toLowerCase();
    const mimeType = ext === 'png' ? 'image/png' : ext === 'jpg' || ext === 'jpeg' ? 'image/jpeg' : 'image/webp';

    const prompt = `Analyze this restaurant menu image. Extract ALL menu items and restaurant information.

IMPORTANT: This menu may be in Arabic, English, or both languages. Extract text exactly as shown.

First, identify the restaurant name from the menu header/logo/title.

Then, extract ALL menu items organized by their categories/sections.

For each menu item, provide:
- name: The dish/item name exactly as shown (preserve Arabic text if present)
- name_ar: The Arabic name if different from name, otherwise same as name
- description: Brief description if available
- price: The numeric price value only (extract number, e.g., if "1.50" or "$1.50" or "1.50 JOD", return "1.50")
- category: The category/section name (e.g., "Sandwiches", "Meals", "Appetizers", "Drinks", "ساندويش", "وجبات", "مقبلات", "مشروبات")

Also extract:
- restaurant_name: The restaurant name from the menu header/logo
- restaurant_name_ar: Arabic name if visible

Return ONLY a valid JSON object in this format:
{
  "restaurant_name": "Restaurant Name",
  "restaurant_name_ar": "اسم المطعم",
  "products": [
    {
      "name": "Item Name",
      "name_ar": "اسم العنصر",
      "description": "Description if available",
      "price": "1.50",
      "category": "Category Name",
      "category_ar": "اسم الفئة"
    }
  ]
}

Extract ALL visible menu items. If prices are shown, include them. If you cannot read the menu clearly, return an empty products array but still try to extract restaurant name.`;

    const response = await openai.chat.completions.create({
      model: 'gpt-4o', // Using vision-capable model
      messages: [
        {
          role: 'user',
          content: [
            {
              type: 'text',
              text: prompt
            },
            {
              type: 'image_url',
              image_url: {
                url: `data:${mimeType};base64,${base64Image}`
              }
            }
          ]
        }
      ],
      max_tokens: 2000,
      temperature: 0.1, // Low temperature for accurate extraction
    });

    const content = response.choices[0].message.content;
    console.log('OpenAI Response (first 500 chars):', content.substring(0, 500));
    
    // Try to extract JSON from the response
    let result = {
      restaurant_name: null,
      restaurant_name_ar: null,
      products: []
    };
    
    try {
      // Remove markdown code blocks if present
      let jsonStr = content;
      const codeBlockMatch = content.match(/```(?:json)?\s*(\{[\s\S]*\})\s*```/);
      if (codeBlockMatch) {
        jsonStr = codeBlockMatch[1];
      } else {
        // Try to find JSON object
        const jsonObjectMatch = content.match(/\{[\s\S]*\}/);
        if (jsonObjectMatch) {
          jsonStr = jsonObjectMatch[0];
        }
      }
      
      const parsed = JSON.parse(jsonStr);
      
      // Handle both old format (array) and new format (object)
      if (Array.isArray(parsed)) {
        // Old format - array of items
        result.products = parsed;
      } else if (parsed.products) {
        // New format - object with restaurant info and products
        result.restaurant_name = parsed.restaurant_name || null;
        result.restaurant_name_ar = parsed.restaurant_name_ar || null;
        result.products = parsed.products || [];
      } else {
        result.products = [];
      }
      
    } catch (parseError) {
      console.error('Failed to parse menu items JSON:', parseError);
      console.error('Raw response:', content);
      // Try to extract items manually or return empty
      throw new Error('Failed to parse menu items from AI response');
    }

    // Validate and clean menu items
    result.products = result.products
      .filter(item => item && item.name) // Only keep items with names
      .map(item => ({
        name: item.name || '',
        name_ar: item.name_ar || item.name || '',
        description: item.description || '',
        description_ar: item.description_ar || item.description || '',
        price: item.price || '',
        category: item.category || 'General',
        category_ar: item.category_ar || item.category || 'عام'
      }));

    console.log(`Extracted ${result.products.length} products, restaurant: ${result.restaurant_name || 'not found'}`);
    return result;

  } catch (error) {
    console.error('Error processing menu image:', error);
    // Don't throw error, just return empty array so menu image can still be saved
    console.log('Returning empty menu items array. User can add items manually.');
    return [];
  }
}
