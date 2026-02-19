# Migration Summary: Replace areas.city_id with areas.region_id

## Overview
This migration replaces the `city_id` foreign key in the `areas` table with `region_id`, linking areas to regions instead of directly to cities. All existing areas are set to `region_id = 1` (Abu Nseir region).

## Changes Made

### 1. Database Schema
- **Updated table**: `areas`
  - Removed: `city_id` (FK to `cities.id`)
  - Added: `region_id` (FK to `regions.id`)
  - All existing records updated to `region_id = 1`

### 2. Migration File
- **File**: `migration_areas_city_id_to_region_id.sql`
- **Steps**:
  1. Adds `region_id` column to `areas` (nullable first)
  2. Updates all existing areas to `region_id = 1`
  3. Adds foreign key constraint on `region_id`
  4. Makes `region_id` NOT NULL
  5. Drops foreign key constraint on `city_id` (dynamic lookup)
  6. Drops `city_id` column

### 3. Backend Updates
**Updated files**:
- `backend/db/init.js` - Updated `areas` table definition for new installations
- `backend/routes/deliveryCompanyDashboard.js`:
  - Updated `/areas` endpoint to JOIN through `regions` to get city information
  - Updated `/zones` grouped query to JOIN: `areas` → `regions` → `cities`
  - Updated grouping logic to include region information

### 4. Data Hierarchy
New structure:
```
cities (1: Amman)
  └── regions (1: Abu Nseir, 2: Al Jubeiha)
      └── areas (all areas now have region_id = 1)
          └── delivery_zones
```

### 5. API Response Changes
The `/api/delivery-company/areas` endpoint now returns:
- `region_id`, `region_name`, `region_name_ar` (new fields)
- `city_id`, `city_name`, `city_name_ar` (still available via JOIN)

The `/api/delivery-company/zones?grouped=true` endpoint now includes:
- `region_id`, `region_name`, `region_name_ar` in grouped zone data

## How to Run Migration

```bash
# Make sure regions table exists and has region_id = 1 (Abu Nseir)
mysql -u root -p restaurant_websites < backend/db/migration_add_regions.sql
mysql -u root -p restaurant_websites < backend/db/seed_regions.sql

# Run the migration
mysql -u root -p restaurant_websites < backend/db/migration_areas_city_id_to_region_id.sql
```

## Notes
- All existing areas are set to `region_id = 1` (Abu Nseir)
- City information is still accessible via `regions.city_id` JOIN
- Seed files (`seed_*.sql`) still reference `city_id` - these are historical and don't need updating unless re-run
- `restaurant_websites.area_id` references remain unchanged (they reference `areas.id`, not `city_id`)

## Testing Checklist
- [ ] Run migration on test database
- [ ] Verify all areas have `region_id = 1`
- [ ] Test `/api/delivery-company/areas` endpoint returns region info
- [ ] Test `/api/delivery-company/zones?grouped=true` includes region info
- [ ] Verify frontend displays region information correctly
