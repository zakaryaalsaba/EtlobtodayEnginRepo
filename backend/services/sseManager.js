/**
 * Server-Sent Events Manager
 * Manages SSE connections for real-time order updates
 */

// Store active SSE connections by website_id (for admin)
const connections = new Map(); // Map<websiteId, Set<Response>>

// Store active customer SSE connections by order_id (for customers tracking orders)
const customerConnections = new Map(); // Map<orderId, Set<Response>>

/**
 * Add a new SSE connection
 */
export function addConnection(websiteId, res) {
  if (!connections.has(websiteId)) {
    connections.set(websiteId, new Set());
  }
  connections.get(websiteId).add(res);

  // Remove connection when client disconnects
  res.on('close', () => {
    removeConnection(websiteId, res);
  });

  // Send initial connection message
  res.write(`data: ${JSON.stringify({ type: 'connected', message: 'Connected to order updates' })}\n\n`);
}

/**
 * Remove an SSE connection
 */
export function removeConnection(websiteId, res) {
  if (connections.has(websiteId)) {
    connections.get(websiteId).delete(res);
    if (connections.get(websiteId).size === 0) {
      connections.delete(websiteId);
    }
  }
}

/**
 * Send order update to all connected clients for a specific website
 */
export function broadcastOrderUpdate(websiteId, order) {
  if (connections.has(websiteId)) {
    const message = JSON.stringify({
      type: 'new_order',
      order: order
    });
    
    const data = `data: ${message}\n\n`;
    
    connections.get(websiteId).forEach(res => {
      try {
        res.write(data);
      } catch (error) {
        console.error('Error sending SSE message:', error);
        // Remove broken connection
        removeConnection(websiteId, res);
      }
    });
  }
}

/**
 * Send order status update to all connected clients for a specific website
 */
export function broadcastOrderStatusUpdate(websiteId, orderId, status) {
  if (connections.has(websiteId)) {
    const message = JSON.stringify({
      type: 'order_status_update',
      orderId: orderId,
      status: status
    });
    
    const data = `data: ${message}\n\n`;
    
    connections.get(websiteId).forEach(res => {
      try {
        res.write(data);
      } catch (error) {
        console.error('Error sending SSE message:', error);
        // Remove broken connection
        removeConnection(websiteId, res);
      }
    });
  }
}

/**
 * Get connection count for a website (for debugging)
 */
export function getConnectionCount(websiteId) {
  return connections.has(websiteId) ? connections.get(websiteId).size : 0;
}

/**
 * Add a new customer SSE connection for order tracking
 */
export function addCustomerConnection(orderId, res) {
  if (!customerConnections.has(orderId)) {
    customerConnections.set(orderId, new Set());
  }
  customerConnections.get(orderId).add(res);

  // Remove connection when client disconnects
  res.on('close', () => {
    removeCustomerConnection(orderId, res);
  });

  // Send initial connection message
  res.write(`data: ${JSON.stringify({ type: 'connected', message: 'Connected to order tracking' })}\n\n`);
}

/**
 * Remove a customer SSE connection
 */
export function removeCustomerConnection(orderId, res) {
  if (customerConnections.has(orderId)) {
    customerConnections.get(orderId).delete(res);
    if (customerConnections.get(orderId).size === 0) {
      customerConnections.delete(orderId);
    }
  }
}

/**
 * Send order status update to customer tracking this order
 */
export function broadcastOrderStatusToCustomer(orderId, order) {
  if (customerConnections.has(orderId)) {
    const message = JSON.stringify({
      type: 'order_status_update',
      order: order
    });
    
    const data = `data: ${message}\n\n`;
    
    customerConnections.get(orderId).forEach(res => {
      try {
        res.write(data);
      } catch (error) {
        console.error('Error sending SSE message to customer:', error);
        // Remove broken connection
        removeCustomerConnection(orderId, res);
      }
    });
  }
}

