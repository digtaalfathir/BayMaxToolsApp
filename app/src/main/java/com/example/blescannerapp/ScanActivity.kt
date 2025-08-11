package com.example.blescannerapp

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class ScanActivity : AppCompatActivity() {

    private lateinit var scanButton: Button
    private lateinit var listView: ListView
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bleScanner: BluetoothLeScanner
    private val deviceList = mutableListOf<BluetoothDevice>()
    private lateinit var adapter: ArrayAdapter<String>
    private var bluetoothGatt: BluetoothGatt? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)

        scanButton = findViewById(R.id.scanButton)
        listView = findViewById(R.id.deviceList)

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        if (!hasAllPermissions()) {
            requestPermissions()
        }

        bleScanner = bluetoothAdapter.bluetoothLeScanner
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        listView.adapter = adapter

        scanButton.setOnClickListener {
            if (hasAllPermissions()) {
                startScan()
            } else {
                requestPermissions()
            }
        }

        listView.setOnItemClickListener { _, _, position, _ ->
            val device = deviceList[position]
            connectToDevice(device)
        }
    }

    private fun startScan() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permission belum diberikan!", Toast.LENGTH_SHORT).show()
            return
        }

        deviceList.clear()
        adapter.clear()
        Toast.makeText(this, "Scanning BLE...", Toast.LENGTH_SHORT).show()

        val scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device
                if (!deviceList.contains(device)) {
                    val deviceName = getDeviceNameSafe(device)
                    deviceList.add(device)
                    adapter.add("$deviceName (${device.address})")
                }
            }
        }

        bleScanner.startScan(scanCallback)

        Handler(Looper.getMainLooper()).postDelayed({
            bleScanner.stopScan(scanCallback)
            Toast.makeText(this, "Scan selesai", Toast.LENGTH_SHORT).show()
        }, 5000)
    }

    private fun connectToDevice(device: BluetoothDevice) {
        Toast.makeText(this, "Menghubungkan ke ${getDeviceNameSafe(device)}...", Toast.LENGTH_SHORT).show()

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permission connect belum diberikan!", Toast.LENGTH_SHORT).show()
            return
        }

        bluetoothGatt = device.connectGatt(this, false, object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                super.onConnectionStateChange(gatt, status, newState)

                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    runOnUiThread {
                        Toast.makeText(applicationContext, "Terhubung ke ${getDeviceNameSafe(device)}", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@ScanActivity, ControlActivity::class.java)
                        intent.putExtra("DEVICE_ADDRESS", device.address)
                        startActivity(intent)
                    }
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    runOnUiThread {
                        Toast.makeText(applicationContext, "Koneksi terputus", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun getDeviceNameSafe(device: BluetoothDevice): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                device.name ?: "Unknown Device"
            } else {
                "Unknown Device"
            }
        } else {
            device.name ?: "Unknown Device"
        }
    }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ),
                1
            )
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }
    }

    private fun hasAllPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        } else {
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }
}