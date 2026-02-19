# PayTabs Recurring Transactions Setup

## Current Issue

When processing saved card payments server-to-server, you're getting:
```
PCI DSS certification to a minimum of SAQ A-EP is required
```

This happens because:
1. **Recurring transactions** are not enabled on your PayTabs profile
2. **PCI DSS compliance** is required for server-to-server token payments

## Solution: Enable Recurring Transactions

### Step 1: Contact PayTabs Support

You need to contact PayTabs support to enable recurring transactions on your account:

1. **Email**: customercare@paytabs.com
2. **Subject**: "Enable Recurring Transactions for Profile ID 169360"
3. **Include**:
   - Your Profile ID: `169360`
   - Request to enable recurring transaction mode
   - Mention you need it for token-based payments (saved cards)

### Step 2: Verify in Dashboard

After PayTabs enables recurring transactions:

1. Log in to PayTabs Dashboard
2. Go to **Developers** > **API Keys** > **Key Management**
3. Check if recurring transactions are enabled
4. Verify your Web API Server Key is active

### Step 3: Test Again

Once enabled, test a saved card payment. The backend will use `tran_class: 'recurring'` which should work after recurring is enabled.

## Alternative: Use PayTabs SDK for Saved Cards

Until recurring transactions are enabled, you can use the PayTabs SDK even for saved cards (it will show the payment UI, but will use the saved token). This is a temporary workaround.

## Why This Is Required

PayTabs requires:
- **PCI DSS SAQ A-EP compliance** for server-to-server token payments
- **Recurring transaction mode** to be enabled on your profile
- **Web API Server Key** (not Mobile SDK key) for server-to-server calls

All of these are account-level configurations that must be set up by PayTabs support.

## Current Status

✅ **Working**:
- Web API Server Key configured in `.env`
- Backend correctly using `tran_class: 'recurring'`
- Token is being sent correctly

❌ **Blocked**:
- Recurring transactions not enabled on PayTabs profile
- PCI DSS compliance not configured

## Next Steps

1. Contact PayTabs support to enable recurring transactions
2. Wait for confirmation from PayTabs
3. Test saved card payments again
4. If still failing, contact PayTabs about PCI DSS compliance requirements
