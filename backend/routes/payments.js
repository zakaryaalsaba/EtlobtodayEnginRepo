import express from 'express';
import Stripe from 'stripe';
import dotenv from 'dotenv';

dotenv.config();

const router = express.Router();

// Initialize Stripe only when key is set (allows server to start without Stripe, e.g. when using PayTabs only)
const stripe = process.env.STRIPE_SECRET_KEY
  ? new Stripe(process.env.STRIPE_SECRET_KEY, { apiVersion: '2024-11-20.acacia' })
  : null;

/**
 * POST /api/payments/create-intent
 * Create a Stripe payment intent
 */
router.post('/create-intent', async (req, res) => {
  try {
    const { website_id, amount, currency = 'usd' } = req.body;

    if (!website_id || !amount) {
      return res.status(400).json({ error: 'Missing required fields: website_id and amount' });
    }

    if (!stripe) {
      return res.status(503).json({ error: 'Stripe is not configured. Please set STRIPE_SECRET_KEY in environment variables.' });
    }

    // Create payment intent with only card payment methods
    const paymentIntent = await stripe.paymentIntents.create({
      amount: Math.round(amount), // Amount in cents
      currency: currency.toLowerCase(),
      metadata: {
        website_id: website_id.toString()
      },
      payment_method_types: ['card'],
      automatic_payment_methods: {
        enabled: false // Disable automatic payment methods to avoid warnings
      }
    });

    res.json({
      client_secret: paymentIntent.client_secret,
      payment_intent_id: paymentIntent.id
    });
  } catch (error) {
    console.error('Error creating payment intent:', error);
    res.status(500).json({ 
      error: 'Failed to create payment intent',
      message: error.message 
    });
  }
});

/**
 * POST /api/payments/confirm
 * Confirm a payment intent (webhook or manual confirmation)
 */
router.post('/confirm', async (req, res) => {
  try {
    const { payment_intent_id } = req.body;

    if (!payment_intent_id) {
      return res.status(400).json({ error: 'Missing payment_intent_id' });
    }
    if (!stripe) {
      return res.status(503).json({ error: 'Stripe is not configured.' });
    }

    const paymentIntent = await stripe.paymentIntents.retrieve(payment_intent_id);

    res.json({
      status: paymentIntent.status,
      payment_intent_id: paymentIntent.id,
      amount: paymentIntent.amount,
      currency: paymentIntent.currency
    });
  } catch (error) {
    console.error('Error confirming payment:', error);
    res.status(500).json({ 
      error: 'Failed to confirm payment',
      message: error.message 
    });
  }
});

export default router;

