package com.example.android_1

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.Manifest
import android.content.ContentValues.TAG
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.IOException
import java.util.UUID

private const val REQUEST_BLUETOOTH_PERMISSION = 2

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val buttonSend = findViewById<Button>(R.id.button_send)

        // เรียกใช้ BluetoothManager เพื่อรับ BluetoothAdapter
        val bluetoothManager = getSystemService(BluetoothManager::class.java)
        val bluetoothAdapter = bluetoothManager.adapter

        fun sendData() {
            // ตรวจสอบสิทธิ์ Bluetooth
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH), REQUEST_BLUETOOTH_PERMISSION)
                return
            }

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_CONNECT), REQUEST_BLUETOOTH_PERMISSION)
                return
            }

            try {
                // สร้าง BluetoothDevice สำหรับมือถือ 2
                val device = bluetoothAdapter.getRemoteDevice("88:F7:BF:28:D8:0C")

                // สร้าง BluetoothSocket สำหรับเชื่อมต่อกับมือถือ 2
                val socket = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"))

                // เชื่อมต่อกับมือถือ 2
                socket.connect()
                Log.d(TAG,"not connect moblie 2")

                // ส่งข้อมูลไปยังมือถือ 2
                val data = "Helloworld".toByteArray()
                socket.outputStream.write(data)
                Log.d(TAG,"$socket")

                // ปิดการเชื่อมต่อ
                socket.close()
            } catch (securityException: SecurityException) {
                Toast.makeText(this, ("$securityException"), Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        // ตั้งค่าฟังก์ชันสำหรับปุ่ม Send
        buttonSend.setOnClickListener {
            sendData()
        }
    }
}