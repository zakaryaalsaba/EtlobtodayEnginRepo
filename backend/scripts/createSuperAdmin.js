import dotenv from 'dotenv';
import bcrypt from 'bcryptjs';
import { pool } from '../db/init.js';

dotenv.config();

async function main() {
  const [, , name, email, password] = process.argv;

  if (!name || !email || !password) {
    console.error('Usage: node scripts/createSuperAdmin.js "Name" "email@example.com" "PlaintextPassword"');
    process.exit(1);
  }

  try {
    const [existing] = await pool.execute(
      'SELECT id FROM super_admins WHERE email = ?',
      [email]
    );

    if (existing.length > 0) {
      console.error(`A super admin with email ${email} already exists (id=${existing[0].id}).`);
      process.exit(1);
    }

    const passwordHash = await bcrypt.hash(password, 10);

    const [result] = await pool.execute(
      'INSERT INTO super_admins (name, email, password_hash) VALUES (?, ?, ?)',
      [name, email, passwordHash]
    );

    console.log('✅ Super admin created successfully.');
    console.log(`   ID: ${result.insertId}`);
    console.log(`   Email: ${email}`);
  } catch (err) {
    console.error('❌ Failed to create super admin:', err.message);
    process.exit(1);
  } finally {
    await pool.end().catch(() => {});
  }
}

main();

