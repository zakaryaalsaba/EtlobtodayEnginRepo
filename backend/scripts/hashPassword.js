import bcrypt from 'bcryptjs';

async function main() {
  const [, , plain] = process.argv;
  if (!plain) {
    console.error('Usage: node scripts/hashPassword.js "PlaintextPassword"');
    process.exit(1);
  }

  const hash = await bcrypt.hash(plain, 10);
  console.log(hash);
}

main();

