package com.example.android_1

import android.app.Activity
import android.bluetooth.*
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.util.Log
import java.util.*
import android.Manifest
import androidx.core.app.ActivityCompat
import android.bluetooth.BluetoothGattWriteCallback

import android.bluetooth.BluetoothGattCharacteristic


class MainActivity : Activity() {

    companion object{
        const val REQUEST_BLUETOOTH_PERMISSION = 1
    }
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothLeScanner: BluetoothLeScanner
    private var bluetoothGatt: BluetoothGatt? = null

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i("BluetoothGattCallback", "Connected to GATT server.")
                // เมื่อเชื่อมต่อสำเร็จ, ทำงานต่อไปที่นี่
                sendTestDataToGattServer()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i("BluetoothGattCallback", "Disconnected from GATT server.")
                // เมื่อหลุดการเชื่อมต่อ, ทำงานต่อไปที่นี่
            }
        }
    }
    private fun sendTestDataToGattServer() {
        // สร้าง BluetoothGattCharacteristic ของ GATT server ที่ต้องการส่งข้อมูล
        val serviceUuid = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb") // ตัวอย่าง UUID สำหรับ Heart Rate Service
        val characteristicUuid = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb") // ตัวอย่าง UUID สำหรับ Heart Rate Measurement Characteristic
        val service = bluetoothGatt?.getService(serviceUuid)
        val characteristic = service?.getCharacteristic(characteristicUuid)

        // เตรียมข้อมูลที่ต้องการส่ง
        val value: Byte = 21
        val data = byteArrayOf(value)

        // กำหนดข้อมูลใน characteristic
        if (checkBluetoothPermission()) {
            characteristic?.setValue(data)

            // เริ่มกระบวนการเขียนข้อมูลไปยัง GATT server
            bluetoothGatt?.writeCharacteristic(characteristic, object : BluetoothGattWriteCallback() {
                override fun onWriteComplete(status: Int) {
                    // ตรวจสอบสถานะการเขียนข้อมูล
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        // การเขียนข้อมูลสำเร็จ
                        Log.d("BluetoothGatt", "Write successful")
                    } else {
                        // การเขียนข้อมูลล้มเหลว
                        Log.e("BluetoothGatt", "Write failed: $status")
                    }
                }
            })
        }
    }

    private fun checkBluetoothPermission(): Boolean {
        if (checkSelfPermission(Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED) {
            return true
        } else {
            requestPermissions(arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN), REQUEST_BLUETOOTH_PERMISSION)
            return false
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ตรวจสอบสิทธิ์ก่อนเข้าถึง BluetoothAdapter
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // ขอสิทธิ์จากผู้ใช้
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), 1)
            return
        }

        // กำหนดค่า BluetoothAdapter
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        // ตรวจสอบว่า Bluetooth ได้เปิดใช้งานหรือไม่
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            // ถ้า Bluetooth ไม่ได้เปิดใช้งาน, ให้เปิดใช้งาน Bluetooth
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, 1)
        } else {
            // ต่อไปทำการสแกนอุปกรณ์ที่ใกล้เคียง
            scanDevices()
        }
    }

    private fun scanDevices() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            // ขอสิทธิ์จากผู้ใช้
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_SCAN), 1)
            return
        }

        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
        bluetoothLeScanner.startScan(scanCallback)

        // หยุดการสแกนหลังจาก 10 วินาที
        Handler().postDelayed({
            bluetoothLeScanner.stopScan(scanCallback)
        }, 10000)
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            Log.i("ScanCallback", "Device found: ${device.name}, ${device.address}")

            // ตรวจสอบสิทธิ์ก่อนเชื่อมต่อ
            if (ActivityCompat.checkSelfPermission(this@MainActivity, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED) {
                // ผู้ใช้อนุญาตให้ใช้สิทธิ์แล้ว สามารถเชื่อมต่อได้
                if (device.name == "YourDeviceName") {
                    bluetoothGatt = device.connectGatt(this@MainActivity, false, gattCallback)
                }
            } else {
                // ขอสิทธิ์จากผู้ใช้
                ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.BLUETOOTH), 2)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // ตรวจสอบสิทธิ์ก่อนปิดการเชื่อมต่อ
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            // ผู้ใช้อนุญาตให้ใช้สิทธิ์แล้ว สามารถปิดการเชื่อมต่อได้
            bluetoothGatt?.close()
        } else {
            // ขอสิทธิ์จากผู้ใช้
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 3)
        }
    }
}
