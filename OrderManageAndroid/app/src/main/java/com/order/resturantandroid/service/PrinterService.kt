package com.order.resturantandroid.service

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import com.order.resturantandroid.data.model.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

class PrinterService(private val context: Context) {
    
    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private var lastError: String? = null
    
    suspend fun connectToPrinter(deviceAddress: String): Boolean = withContext(Dispatchers.IO) {
        try {
            lastError = null
            Log.d(TAG, "[PRINT_DEBUG] connectToPrinter start address=$deviceAddress")
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
                lastError = "Bluetooth unavailable or disabled"
                Log.e(TAG, lastError!!)
                return@withContext false
            }
            Log.d(TAG, "[PRINT_DEBUG] bluetooth enabled=${bluetoothAdapter.isEnabled}, discovering=${bluetoothAdapter.isDiscovering}")
            
            val device: BluetoothDevice? = bluetoothAdapter.getRemoteDevice(deviceAddress)
            if (device == null) {
                lastError = "Paired device not found: $deviceAddress"
                Log.e(TAG, lastError!!)
                return@withContext false
            }
            Log.d(TAG, "[PRINT_DEBUG] target device name=${device.name}, address=${device.address}")

            // Stop discovery before connecting to improve connection reliability.
            if (bluetoothAdapter.isDiscovering) {
                bluetoothAdapter.cancelDiscovery()
            }

            val sppUuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

            // Attempt 1: secure RFCOMM (standard SPP)
            bluetoothSocket = try {
                Log.d(TAG, "[PRINT_DEBUG] trying secure RFCOMM SPP")
                val s = device.createRfcommSocketToServiceRecord(sppUuid)
                s.connect()
                s
            } catch (secureEx: Exception) {
                Log.w(TAG, "Secure RFCOMM connect failed, trying insecure fallback", secureEx)
                try {
                    // Attempt 2: insecure RFCOMM fallback (works with many thermal printers)
                    Log.d(TAG, "[PRINT_DEBUG] trying insecure RFCOMM SPP")
                    val s2 = device.createInsecureRfcommSocketToServiceRecord(sppUuid)
                    s2.connect()
                    s2
                } catch (insecureEx: Exception) {
                    // Attempt 3: channel 1 fallback
                    Log.w(TAG, "Insecure RFCOMM connect failed, trying channel-1 fallback", insecureEx)
                    Log.d(TAG, "[PRINT_DEBUG] trying reflection RFCOMM channel=1")
                    val m = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                    val s3 = m.invoke(device, 1) as BluetoothSocket
                    s3.connect()
                    s3
                }
            }
            
            outputStream = bluetoothSocket?.outputStream
            Log.d(TAG, "Connected to printer")
            Log.d(TAG, "[PRINT_DEBUG] connectToPrinter success outputStream=${outputStream != null}")
            true
        } catch (se: SecurityException) {
            lastError = "Bluetooth security exception: ${se.message}"
            Log.e(TAG, lastError!!, se)
            disconnect()
            false
        } catch (e: Exception) {
            lastError = "Connection failed: ${e.message}"
            Log.e(TAG, "Error connecting to printer", e)
            disconnect()
            false
        }
    }

    suspend fun printOrderSilently(order: Order, restaurantName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            lastError = null
            Log.d(TAG, "[PRINT_DEBUG] printOrderSilently start order=${order.orderNumber} restaurant=$restaurantName")
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
                lastError = "Bluetooth unavailable or disabled"
                Log.e(TAG, lastError!!)
                return@withContext false
            }
            Log.d(TAG, "[PRINT_DEBUG] bonded devices count=${bluetoothAdapter.bondedDevices?.size ?: 0}")
            bluetoothAdapter.bondedDevices?.forEach { d ->
                Log.d(TAG, "[PRINT_DEBUG] bonded device name=${d.name} address=${d.address}")
            }

            // If not already connected, connect to best matched paired printer.
            if (outputStream == null) {
                val target = pickBestPrinterDevice(bluetoothAdapter.bondedDevices)
                if (target == null) {
                    lastError = "No paired printer device found"
                    Log.e(TAG, lastError!!)
                    return@withContext false
                }
                Log.d(TAG, "Auto-connecting printer: ${target.name} (${target.address})")
                if (!connectToPrinter(target.address)) {
                    Log.e(TAG, "[PRINT_DEBUG] connectToPrinter returned false lastError=$lastError")
                    return@withContext false
                }
            }

            val output = outputStream ?: return@withContext false
            val commands = buildBitmapPrintCommands(order, restaurantName)
            Log.d(TAG, "[PRINT_DEBUG] writing ${commands.size} bytes to printer")
            output.write(commands)
            output.flush()
            Log.d(TAG, "Silent print succeeded")
            Log.d(TAG, "[PRINT_DEBUG] printOrderSilently success")
            true
        } catch (se: SecurityException) {
            lastError = "Bluetooth security exception: ${se.message}"
            Log.e(TAG, "Missing Bluetooth runtime permission", se)
            false
        } catch (e: Exception) {
            lastError = "Print failed: ${e.message}"
            Log.e(TAG, "Silent print failed", e)
            false
        } finally {
            disconnect()
        }
    }
    
    suspend fun printOrder(order: Order): Boolean = withContext(Dispatchers.IO) {
        try {
            val output = outputStream ?: return@withContext false
            
            // ESC/POS commands
            val commands = buildBitmapPrintCommands(order, "Restaurant")
            
            output.write(commands)
            output.flush()
            
            Log.d(TAG, "Order printed successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error printing order", e)
            false
        }
    }
    
    private fun buildBitmapPrintCommands(order: Order, restaurantName: String): ByteArray {
        val out = ByteArrayOutputStream()

        // Initialize printer
        out.write(byteArrayOf(0x1B, 0x40)) // ESC @

        // Build and print as raster bitmap so Arabic/Unicode render correctly.
        val bitmap = buildReceiptBitmap(order, restaurantName, PRINTER_WIDTH_PX)
        out.write(bitmapToEscPosRaster(bitmap))

        // Feed and cut
        out.write(byteArrayOf(0x0A, 0x0A, 0x1D, 0x56, 0x41, 0x03))

        return out.toByteArray()
    }

    private fun buildReceiptBitmap(order: Order, restaurantName: String, widthPx: Int): Bitmap {
        val lines = mutableListOf<String>()
        val nowText = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.US).format(Date())
        val orderTime = order.createdAt.ifBlank { nowText }

        lines += "================================"
        lines += restaurantName
        lines += "================================"
        lines += "Order: ${order.orderNumber}"
        lines += "Date: $orderTime"
        lines += ""
        lines += "Items:"
        order.getItemsList().forEach { item ->
            lines += "${item.quantity}x ${item.productName} - ${item.subtotal}"
        }
        lines += ""
        lines += "Total: ${order.totalAmount}"
        lines += "Type: ${order.orderType.replaceFirstChar { it.uppercaseChar() }}"
        if (!order.notes.isNullOrBlank()) {
            lines += "Notes: ${order.notes}"
        }
        lines += "================================"

        val padding = 8
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 24f
            textAlign = Paint.Align.LEFT
            typeface = android.graphics.Typeface.DEFAULT
        }

        val lineHeight = (paint.fontSpacing + 8f).toInt().coerceAtLeast(30)
        val height = (padding * 2 + lines.size * lineHeight).coerceAtLeast(200)
        val bitmap = Bitmap.createBitmap(widthPx, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        var y = padding + lineHeight
        lines.forEach { line ->
            canvas.drawText(line, padding.toFloat(), y.toFloat(), paint)
            y += lineHeight
        }
        return bitmap
    }

    private fun bitmapToEscPosRaster(bitmap: Bitmap): ByteArray {
        // ESC/POS raster format: GS v 0
        val width = bitmap.width
        val height = bitmap.height
        val widthBytes = (width + 7) / 8

        val result = ArrayList<Byte>(8 + widthBytes * height)
        result.add(0x1D) // GS
        result.add(0x76) // 'v'
        result.add(0x30) // '0'
        result.add(0x00) // normal density
        result.add((widthBytes and 0xFF).toByte())
        result.add(((widthBytes shr 8) and 0xFF).toByte())
        result.add((height and 0xFF).toByte())
        result.add(((height shr 8) and 0xFF).toByte())

        for (y in 0 until height) {
            for (xByte in 0 until widthBytes) {
                var byte = 0
                for (bit in 0 until 8) {
                    val x = xByte * 8 + bit
                    if (x < width) {
                        val pixel = bitmap.getPixel(x, y)
                        val r = Color.red(pixel)
                        val g = Color.green(pixel)
                        val b = Color.blue(pixel)
                        // luminance threshold
                        val luminance = (0.299 * r + 0.587 * g + 0.114 * b)
                        if (luminance < 160) {
                            byte = byte or (0x80 shr bit)
                        }
                    }
                }
                result.add(byte.toByte())
            }
        }
        return result.toByteArray()
    }

    private fun pickBestPrinterDevice(devices: Set<BluetoothDevice>?): BluetoothDevice? {
        if (devices.isNullOrEmpty()) return null
        val list = devices.toList()
        val preferred = list.firstOrNull { d ->
            val n = (d.name ?: "").lowercase()
            n.contains("printer") || n.contains("pos") || n.contains("sunmi") || n.contains("ap12") || n.contains("imachine")
        }
        return preferred ?: list.firstOrNull()
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
        private const val PRINTER_WIDTH_PX = 384 // 58mm thermal common printable width
    }

    fun getLastError(): String? = lastError
}

