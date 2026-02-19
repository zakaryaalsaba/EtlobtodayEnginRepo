# Migration Summary: delivery_zones_names Refactoring

## Overview
This migration normalizes the `delivery_zones` table by moving `zone_name_ar` and `zone_name_en` to a separate `delivery_zones_names` table. This allows multiple zones to share the same name, reducing data duplication.

## Changes Made

### 1. Database Schema
- **New table**: `delivery_zones_names` (id, name_ar, name_en, created_at, updated_at)
- **Updated table**: `delivery_zones` 
  - Removed: `zone_name_ar`, `zone_name_en`
  - Added: `zone_name_id` (FK to `delivery_zones_names.id`)

### 2. Migration File
- **File**: `migration_delivery_zones_names_refactor.sql`
- **Steps**:
  1. Creates `delivery_zones_names` table
  2. Migrates existing unique zone name combinations to the new table
  3. Adds `zone_name_id` column to `delivery_zones`
  4. Updates all `delivery_zones` records to reference the new table
  5. Adds foreign key constraint
  6. Makes `zone_name_id` NOT NULL
  7. Drops old `zone_name_ar` and `zone_name_en` columns

### 3. Backend Updates
All backend routes now:
- JOIN with `delivery_zones_names` when fetching zones
- Return `zone_name_ar` and `zone_name_en` as aliases (maintaining API compatibility)
- Find or create zone names in `delivery_zones_names` when creating/updating zones

**Updated files**:
- `backend/db/init.js` - Updated table creation for new installations
- `backend/routes/deliveryCompanyDashboard.js` - All zone endpoints
- `backend/routes/deliveryZones.js` - Super admin zone endpoints
- `backend/routes/restaurant.js` - Zone lookup endpoint

### 4. Frontend/API Compatibility
- **No changes required** - The backend still returns `zone_name_ar` and `zone_name_en` as aliases from the JOIN
- Frontend components (`DeliveryCompanyDashboard.vue`, `WebsiteBuilder.vue`) continue to work
- Android apps (`OrderManageAndroid`) continue to work

### 5. Seed Files
**Note**: Existing seed SQL files (`seed_*.sql`) still reference `zone_name_ar` and `zone_name_en` directly. These files:
- Are safe if already executed (migration handles existing data)
- Need to be updated if you want to re-run them after migration
- Should insert into `delivery_zones_names` first, then reference the `id` in `delivery_zones`

## How to Run Migration

```bash
mysql -u root -p restaurant_websites < migration_delivery_zones_names_refactor.sql
```

## Rollback (if needed)
If you need to rollback, you would need to:
1. Add back `zone_name_ar` and `zone_name_en` columns to `delivery_zones`
2. Populate them from `delivery_zones_names` JOIN
3. Drop `zone_name_id` column
4. Drop `delivery_zones_names` table

## Testing Checklist
- [ ] Run migration on test database
- [ ] Verify zones display correctly in frontend
- [ ] Test creating new zones
- [ ] Test updating existing zones
- [ ] Test zone deletion
- [ ] Verify Android app still works with zone names
