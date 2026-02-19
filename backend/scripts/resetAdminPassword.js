import mysql from 'mysql2/promise';
import bcrypt from 'bcryptjs';
import dotenv from 'dotenv';

dotenv.config();

/**
 * Reset admin password script
 * Usage: node scripts/resetAdminPassword.js <email> <newPassword>
 */

async function resetAdminPassword(email, newPassword) {
  try {
    // Create MySQL connection
    const connection = await mysql.createConnection({
      host: process.env.MYSQL_HOST || 'localhost',
      port: parseInt(process.env.MYSQL_PORT) || 3306,
      database: process.env.MYSQL_DB || 'restaurant_websites',
      user: process.env.MYSQL_USER || 'root',
      password: process.env.MYSQL_PASSWORD || '',
    });

    // Check if admin exists
    const [admins] = await connection.execute(
      'SELECT id, email, name FROM admins WHERE email = ?',
      [email]
    );

    if (admins.length === 0) {
      console.error(`Admin with email ${email} not found.`);
      await connection.end();
      process.exit(1);
    }

    const admin = admins[0];
    console.log(`Found admin: ${admin.name} (${admin.email})`);

    // Hash new password
    const passwordHash = await bcrypt.hash(newPassword, 10);

    // Update password
    await connection.execute(
      'UPDATE admins SET password_hash = ? WHERE email = ?',
      [passwordHash, email]
    );

    console.log(`✅ Password reset successfully for ${email}`);
    console.log(`New password: ${newPassword}`);
    console.log(`\nYou can now login with:`);
    console.log(`Email: ${email}`);
    console.log(`Password: ${newPassword}`);

    await connection.end();
    process.exit(0);
  } catch (error) {
    console.error('Error resetting password:', error);
    process.exit(1);
  }
}

// Get command line arguments
const email = process.argv[2];
const newPassword = process.argv[3];

if (!email || !newPassword) {
  console.log('Usage: node scripts/resetAdminPassword.js <email> <newPassword>');
  console.log('Example: node scripts/resetAdminPassword.js admin@admin.com newpassword123');
  process.exit(1);
}

resetAdminPassword(email, newPassword);

