import nodemailer from 'nodemailer';
import { pool } from '../db/init.js';

/**
 * Notification Service
 * Handles sending notifications via Email, SMS, Push, and WhatsApp
 */

// Email transporter configuration
let emailTransporter = null;

function initEmailTransporter() {
  if (emailTransporter) return emailTransporter;

  // Use environment variables for email configuration
  // For development, you can use Gmail or other SMTP services
  // For production, use a service like SendGrid, AWS SES, etc.
  const emailConfig = {
    host: process.env.SMTP_HOST || 'smtp.gmail.com',
    port: parseInt(process.env.SMTP_PORT) || 587,
    secure: process.env.SMTP_SECURE === 'true', // true for 465, false for other ports
    auth: {
      user: process.env.SMTP_USER,
      pass: process.env.SMTP_PASSWORD,
    },
  };

  // Only create transporter if credentials are provided
  if (emailConfig.auth.user && emailConfig.auth.pass) {
    emailTransporter = nodemailer.createTransport(emailConfig);
  } else {
    console.warn('Email credentials not configured. Email notifications will be disabled.');
  }

  return emailTransporter;
}

/**
 * Get notification settings for a restaurant
 */
export async function getNotificationSettings(websiteId) {
  try {
    const [rows] = await pool.execute(
      `SELECT 
        notifications_enabled,
        notification_email_enabled,
        notification_sms_enabled,
        notification_push_enabled,
        notification_whatsapp_enabled,
        notification_email,
        email as restaurant_email
      FROM restaurant_websites 
      WHERE id = ?`,
      [websiteId]
    );

    if (rows.length === 0) {
      return null;
    }

    const settings = rows[0];
    return {
      notificationsEnabled: settings.notifications_enabled || false,
      emailEnabled: settings.notification_email_enabled || false,
      smsEnabled: settings.notification_sms_enabled || false,
      pushEnabled: settings.notification_push_enabled || false,
      whatsappEnabled: settings.notification_whatsapp_enabled || false,
      notificationEmail: settings.notification_email || settings.restaurant_email || null,
    };
  } catch (error) {
    console.error('Error getting notification settings:', error);
    return null;
  }
}

/**
 * Send email notification for new order
 */
async function sendEmailNotification(websiteId, order, restaurantInfo) {
  try {
    const settings = await getNotificationSettings(websiteId);
    
    if (!settings || !settings.notificationsEnabled || !settings.emailEnabled) {
      console.log('Email notifications disabled for website:', websiteId);
      return false;
    }

    const recipientEmail = settings.notificationEmail;
    if (!recipientEmail) {
      console.warn('No notification email configured for website:', websiteId);
      return false;
    }

    const transporter = initEmailTransporter();
    if (!transporter) {
      console.warn('Email transporter not configured');
      return false;
    }

    // Format order items
    const itemsList = order.items.map(item => 
      `  • ${item.product_name} x${item.quantity} - $${(parseFloat(item.product_price) * item.quantity).toFixed(2)}`
    ).join('\n');

    const emailContent = {
      from: process.env.SMTP_FROM || process.env.SMTP_USER || 'noreply@restaurantaai.com',
      to: recipientEmail,
      subject: `New Order #${order.order_number} - ${restaurantInfo.restaurant_name}`,
      html: `
        <!DOCTYPE html>
        <html>
        <head>
          <style>
            body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
            .container { max-width: 600px; margin: 0 auto; padding: 20px; }
            .header { background: linear-gradient(135deg, #4F46E5, #7C3AED); color: white; padding: 20px; border-radius: 8px 8px 0 0; }
            .content { background: #f9f9f9; padding: 20px; border-radius: 0 0 8px 8px; }
            .order-info { background: white; padding: 15px; margin: 15px 0; border-radius: 5px; border-left: 4px solid #4F46E5; }
            .items { background: white; padding: 15px; margin: 15px 0; border-radius: 5px; }
            .total { font-size: 18px; font-weight: bold; color: #4F46E5; margin-top: 10px; }
            .status { display: inline-block; padding: 5px 15px; border-radius: 20px; background: #FEF3C7; color: #92400E; font-weight: bold; }
          </style>
        </head>
        <body>
          <div class="container">
            <div class="header">
              <h1>New Order Received!</h1>
              <p>Order #${order.order_number}</p>
            </div>
            <div class="content">
              <div class="order-info">
                <h2>Customer Information</h2>
                <p><strong>Name:</strong> ${order.customer_name}</p>
                <p><strong>Phone:</strong> ${order.customer_phone}</p>
                ${order.customer_email ? `<p><strong>Email:</strong> ${order.customer_email}</p>` : ''}
                ${order.customer_address ? `<p><strong>Address:</strong> ${order.customer_address}</p>` : ''}
              </div>
              
              <div class="items">
                <h2>Order Items</h2>
                <pre style="font-family: Arial, sans-serif; white-space: pre-wrap;">${itemsList}</pre>
                <div class="total">Total: $${parseFloat(order.total_amount).toFixed(2)}</div>
              </div>
              
              ${order.notes ? `
              <div class="order-info">
                <h3>Special Notes:</h3>
                <p>${order.notes}</p>
              </div>
              ` : ''}
              
              <div style="margin-top: 20px; text-align: center;">
                <span class="status">Status: ${order.status.toUpperCase()}</span>
              </div>
              
              <p style="margin-top: 20px; font-size: 12px; color: #666;">
                This is an automated notification from ${restaurantInfo.restaurant_name}
              </p>
            </div>
          </div>
        </body>
        </html>
      `,
      text: `
New Order #${order.order_number}

Customer Information:
Name: ${order.customer_name}
Phone: ${order.customer_phone}
${order.customer_email ? `Email: ${order.customer_email}` : ''}
${order.customer_address ? `Address: ${order.customer_address}` : ''}

Order Items:
${itemsList}

Total: $${parseFloat(order.total_amount).toFixed(2)}

${order.notes ? `Special Notes: ${order.notes}` : ''}

Status: ${order.status.toUpperCase()}
      `,
    };

    const info = await transporter.sendMail(emailContent);
    console.log('Email notification sent:', info.messageId);
    return true;
  } catch (error) {
    console.error('Error sending email notification:', error);
    return false;
  }
}

/**
 * Send SMS notification (placeholder - requires SMS service integration)
 */
async function sendSMSNotification(websiteId, order, restaurantInfo) {
  try {
    const settings = await getNotificationSettings(websiteId);
    
    if (!settings || !settings.notificationsEnabled || !settings.smsEnabled) {
      return false;
    }

    // TODO: Integrate with SMS service (Twilio, AWS SNS, etc.)
    console.log('SMS notification not implemented yet');
    return false;
  } catch (error) {
    console.error('Error sending SMS notification:', error);
    return false;
  }
}

/**
 * Send Push notification to restaurant admin
 */
async function sendPushNotification(websiteId, order, restaurantInfo) {
  try {
    console.log(`[RESTAURANT NOTIFICATION] [PUSH] Checking push notification for website_id: ${websiteId}, order: ${order.order_number}`);
    
    try {
      const { sendPushNotification } = await import('./pushNotificationService.js');
      
      // Get restaurant admin device token (also get admin id for logging)
      const [admins] = await pool.execute(
        'SELECT id, device_token FROM admins WHERE website_id = ?',
        [websiteId]
      );

      console.log(`[RESTAURANT NOTIFICATION] [PUSH] Found ${admins.length} admin(s) for website_id: ${websiteId}`);
      admins.forEach((a, i) => {
        const hasToken = a.device_token && String(a.device_token).trim() !== '';
        console.log(`[RESTAURANT NOTIFICATION] [PUSH]   Admin ${i + 1}: id=${a.id}, has_device_token=${hasToken}${hasToken ? `, token_length=${String(a.device_token).length}` : ''}`);
      });

      const adminWithToken = admins.find(a => a.device_token && String(a.device_token).trim() !== '');
      if (adminWithToken) {
        const title = `New Order: ${order.order_number}`;
        const body = `New order received: ${order.customer_name} - ${order.total_amount || 'N/A'}`;
        
        const data = {
          type: 'new_order',
          order_id: String(order.id),
          order_number: order.order_number,
          website_id: String(websiteId)
        };

        console.log(`[RESTAURANT NOTIFICATION] [PUSH] Sending FCM to admin id=${adminWithToken.id}, title="${title}"`);
        const success = await sendPushNotification(
          adminWithToken.device_token,
          title,
          body,
          data
        );
        
        if (success) {
          console.log(`[RESTAURANT NOTIFICATION] [PUSH] ✅ Push notification sent to restaurant admin for order ${order.order_number}`);
        } else {
          console.warn(`[RESTAURANT NOTIFICATION] [PUSH] ❌ FCM returned failure for order ${order.order_number}`);
        }
        
        return success;
      } else {
        console.log(`[RESTAURANT NOTIFICATION] [PUSH] No device token for restaurant (website_id: ${websiteId}). Admin must open Order Manage app and stay logged in so the app can register the FCM token with the server.`);
        return false;
      }
    } catch (error) {
      console.error('[RESTAURANT NOTIFICATION] [PUSH] Error sending push to restaurant admin:', error);
      return false;
    }
  } catch (error) {
    console.error('[RESTAURANT NOTIFICATION] [PUSH] Error:', error);
    return false;
  }
}

/**
 * Send WhatsApp notification (placeholder - requires WhatsApp Business API)
 */
async function sendWhatsAppNotification(websiteId, order, restaurantInfo) {
  try {
    const settings = await getNotificationSettings(websiteId);
    
    if (!settings || !settings.notificationsEnabled || !settings.whatsappEnabled) {
      return false;
    }

    // TODO: Integrate with WhatsApp Business API
    console.log('WhatsApp notification not implemented yet');
    return false;
  } catch (error) {
    console.error('Error sending WhatsApp notification:', error);
    return false;
  }
}

/**
 * Send order notification to restaurant owner
 * Sends via all enabled channels
 */
export async function sendOrderNotification(websiteId, order) {
  try {
    // Get restaurant information
    const [restaurants] = await pool.execute(
      'SELECT restaurant_name, email FROM restaurant_websites WHERE id = ?',
      [websiteId]
    );

    if (restaurants.length === 0) {
      console.error('[RESTAURANT NOTIFICATION] Restaurant not found for website_id:', websiteId);
      return false;
    }

    const restaurantInfo = restaurants[0];
    console.log(`[RESTAURANT NOTIFICATION] Sending to restaurant "${restaurantInfo.restaurant_name}" (website_id: ${websiteId}), order: ${order.order_number}`);

    // Send notifications via all enabled channels
    const results = await Promise.allSettled([
      sendEmailNotification(websiteId, order, restaurantInfo),
      sendSMSNotification(websiteId, order, restaurantInfo),
      sendPushNotification(websiteId, order, restaurantInfo),
      sendWhatsAppNotification(websiteId, order, restaurantInfo),
    ]);

    const successCount = results.filter(r => r.status === 'fulfilled' && r.value === true).length;
    console.log(`[RESTAURANT NOTIFICATION] Order notification sent via ${successCount} channel(s) (email, sms, push, whatsapp)`);
    results.forEach((r, i) => {
      const names = ['email', 'sms', 'push', 'whatsapp'];
      const ok = r.status === 'fulfilled' && r.value === true;
      console.log(`[RESTAURANT NOTIFICATION]   - ${names[i]}: ${ok ? 'OK' : 'skipped/failed'}`);
    });
    
    return successCount > 0;
  } catch (error) {
    console.error('[RESTAURANT NOTIFICATION] Error sending order notification:', error);
    return false;
  }
}

