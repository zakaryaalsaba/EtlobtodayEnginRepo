package com.order.resturantandroid.service

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import com.order.resturantandroid.data.model.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.util.*

class PrinterService(private val context: Context) {
    
    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    
    suspend fun connectToPrinter(deviceAddress: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
                Log.e(TAG, "Bluetooth not available or not enabled")
                return@withContext false
            }
            
            val device: BluetoothDevice? = bluetoothAdapter.getRemoteDevice(deviceAddress)
            if (device == null) {
                Log.e(TAG, "Device not found: $deviceAddress")
                return@withContext false
            }
            
            val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
            bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid)
            bluetoothSocket?.connect()
            
            outputStream = bluetoothSocket?.outputStream
            Log.d(TAG, "Connected to printer")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to printer", e)
            disconnect()
            false
        }
    }
    
    suspend fun printOrder(order: Order): Boolean = withContext(Dispatchers.IO) {
        try {
            val output = outputStream ?: return@withContext false
            
            // ESC/POS commands
            val commands = buildPrintCommands(order)
            
            output.write(commands)
            output.flush()
            
            Log.d(TAG, "Order printed successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error printing order", e)
            false
        }
    }
    
    private fun buildPrintCommands(order: Order): ByteArray {
        val commands = mutableListOf<Byte>()
        
        // Initialize printer
        commands.add(0x1B) // ESC
        commands.add(0x40) // @
        
        // Center align
        commands.add(0x1B) // ESC
        commands.add(0x61) // a
        commands.add(0x01) // 1 = center
        
        // Restaurant name (from session or order)
        val restaurantName = "Restaurant Name" // TODO: Get from session
        restaurantName.toByteArray().forEach { commands.add(it) }
        commands.add(0x0A) // Line feed
        
        // Order number
        "Order: ${order.orderNumber}".toByteArray().forEach { commands.add(it) }
        commands.add(0x0A)
        
        // Date & time
        "Date: ${order.createdAt}".toByteArray().forEach { commands.add(it) }
        commands.add(0x0A)
        commands.add(0x0A)
        
        // Left align
        commands.add(0x1B) // ESC
        commands.add(0x61) // a
        commands.add(0x00) // 0 = left
        
        // Items
        "Items:".toByteArray().forEach { commands.add(it) }
        commands.add(0x0A)
        order.getItemsList().forEach { item ->
            val line = "${item.quantity}x ${item.productName} - $${item.subtotal}"
            line.toByteArray().forEach { commands.add(it) }
            commands.add(0x0A)
        }
        commands.add(0x0A)
        
        // Total
        commands.add(0x1B) // ESC
        commands.add(0x45) // E (bold on)
        commands.add(0x01) // 1
        "Total: $${order.totalAmount}".toByteArray().forEach { commands.add(it) }
        commands.add(0x0A)
        commands.add(0x1B) // ESC
        commands.add(0x45) // E (bold off)
        commands.add(0x00) // 0
        
        // Order type
        commands.add(0x0A)
        "Type: ${order.orderType.replaceFirstChar { it.uppercaseChar() }}".toByteArray().forEach { commands.add(it) }
        commands.add(0x0A)
        
        // Notes
        order.notes?.let {
            commands.add(0x0A)
            "Notes: $it".toByteArray().forEach { commands.add(it) }
            commands.add(0x0A)
        }
        
        // Cut paper
        commands.add(0x0A)
        commands.add(0x0A)
        commands.add(0x1D) // GS
        commands.add(0x56) // V
        commands.add(0x41) // A
        commands.add(0x03) // Cut
        
        return commands.toByteArray()
    }
    
    fun disconnect() {
        try {
            outputStream?.close()
            bluetoothSocket?.close()
            outputStream = null
            bluetoothSocket = null
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting", e)
        }
    }
    
    companion object {
        private const val TAG = "PrinterService"
    }
}

