import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
import sharp from 'sharp';
import { S3Client, PutObjectCommand } from '@aws-sdk/client-s3';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const uploadsDir = path.join(__dirname, '../uploads');

let s3Client = null;
let spacesConfigured = false;

function initSpaces() {
  if (s3Client || spacesConfigured) return;

  const {
    SPACES_ENDPOINT,
    SPACES_REGION,
    SPACES_ACCESS_KEY_ID,
    SPACES_SECRET_ACCESS_KEY,
    SPACES_BUCKET
  } = process.env;

  if (!SPACES_BUCKET || !SPACES_ACCESS_KEY_ID || !SPACES_SECRET_ACCESS_KEY || !SPACES_REGION || !SPACES_ENDPOINT) {
    spacesConfigured = false;
    return;
  }

  s3Client = new S3Client({
    region: SPACES_REGION,
    endpoint: SPACES_ENDPOINT,
    credentials: {
      accessKeyId: SPACES_ACCESS_KEY_ID,
      secretAccessKey: SPACES_SECRET_ACCESS_KEY
    }
  });

  spacesConfigured = true;
}

function ensureUploadsDir() {
  if (!fs.existsSync(uploadsDir)) {
    fs.mkdirSync(uploadsDir, { recursive: true });
  }
}

async function processImageBuffer(buffer, baseFilename) {
  // Single medium variant for now (local), Spaces path building handled per target
  const image = sharp(buffer).rotate();

  // Default medium width 800px, WebP, quality 75
  const medium = image.clone().resize({ width: 800, withoutEnlargement: true }).webp({ quality: 75 });

  const mediumBuffer = await medium.toBuffer();

  return {
    mediumBuffer,
    mediumFilename: `${baseFilename}-md.webp`
  };
}

export async function saveImageToLocal(folder, file) {
  ensureUploadsDir();

  const baseName = `${folder}-${Date.now()}-${Math.round(Math.random() * 1e9)}`;
  const { mediumBuffer, mediumFilename } = await processImageBuffer(file.buffer, baseName);

  const targetPath = path.join(uploadsDir, mediumFilename);
  fs.writeFileSync(targetPath, mediumBuffer);

  const filePath = `/uploads/${mediumFilename}`;
  const apiBaseUrl = process.env.API_BASE_URL || `http://localhost:${process.env.PORT || 3000}`;
  const imageUrl = `${apiBaseUrl}${filePath}`;

  return {
    url: imageUrl,
    storagePath: targetPath
  };
}

export async function saveImageToSpaces(folder, file) {
  initSpaces();

  if (!spacesConfigured || !s3Client) {
    return saveImageToLocal(folder, file);
  }

  const baseName = `${folder}/${Date.now()}-${Math.round(Math.random() * 1e9)}`;
  const { mediumBuffer, mediumFilename } = await processImageBuffer(file.buffer, baseName);

  const keyMedium = mediumFilename;

  const bucket = process.env.SPACES_BUCKET;
  const putParams = {
    Bucket: bucket,
    Key: keyMedium,
    Body: mediumBuffer,
    ContentType: 'image/webp',
    ACL: 'public-read'
  };

  await s3Client.send(new PutObjectCommand(putParams));

  const publicBase = process.env.SPACES_PUBLIC_BASE_URL;
  const urlBase = publicBase && publicBase.trim()
    ? publicBase.replace(/\/$/, '')
    : `${process.env.SPACES_ENDPOINT?.replace(/^https?:\/\//, '').replace(/\/$/, '')}/${bucket}`;

  const imageUrl = publicBase && publicBase.trim()
    ? `${urlBase}/${keyMedium}`
    : `https://${urlBase}/${keyMedium}`;

  // We still write a local copy for dev/debugging if uploadsDir is available
  try {
    ensureUploadsDir();
    const localPath = path.join(uploadsDir, path.basename(mediumFilename));
    fs.writeFileSync(localPath, mediumBuffer);
  } catch {
    // ignore local write failures in Spaces mode
  }

  return {
    url: imageUrl,
    storagePath: `spaces://${bucket}/${keyMedium}`
  };
}

/**
 * Generic image save that decides between local filesystem and Spaces,
 * based on environment variables.
 */
export async function saveImage(folder, file) {
  initSpaces();
  if (spacesConfigured) {
    return saveImageToSpaces(folder, file);
  }
  return saveImageToLocal(folder, file);
}

