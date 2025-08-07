package com.example.blescannerapp

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.content.pm.PackageManager
import android.os.*
import android.widget.*
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class MainActivity : AppCompatActivity() {

    private lateinit var scanButton: Button
    private lateinit var listView: ListView
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bleScanner: BluetoothLeScanner
    private val deviceList = mutableListOf<BluetoothDevice>()
    private lateinit var adapter: ArrayAdapter<String>
    private var bluetoothGatt: BluetoothGatt? = null
    private lateinit var characteristicToWrite: BluetoothGattCharacteristic

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Connected to ${gatt.device.address}", Toast.LENGTH_SHORT).show()
                }
                if (ActivityCompat.checkSelfPermission(this@MainActivity, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    gatt.discoverServices()
                } else {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Permission BLUETOOTH_CONNECT belum diberikan", Toast.LENGTH_SHORT).show()
                    }
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Disconnected from ${gatt.device.address}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                runOnUiThread {
//                    Toast.makeText(this@MainActivity, "Services discovered", Toast.LENGTH_SHORT).show()
                }

                // Cari karakteristik untuk menulis
                for (service in gatt.services) {
                    for (characteristic in service.characteristics) {
                        val props = characteristic.properties
                        if ((props and BluetoothGattCharacteristic.PROPERTY_WRITE) > 0 ||
                            (props and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) > 0) {

                            characteristicToWrite = characteristic
                            runOnUiThread {
//                                Toast.makeText(this@MainActivity, "Karakteristik WRITE ditemukan", Toast.LENGTH_SHORT).show()
                            }
                            break
                        }
                    }
                }

            } else {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Service discovery failed: $status", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        scanButton = findViewById(R.id.scanButton)
        listView = findViewById(R.id.deviceList)
        val inputCommand = findViewById<EditText>(R.id.inputCommand)
        val sendButton = findViewById<Button>(R.id.sendButton)
        val permissionButton: Button = findViewById(R.id.permissionButton)

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        sendButton.setOnClickListener {
            val textToSend = inputCommand.text.toString()
            if (textToSend.isNotBlank()) {
                sendToBle(textToSend)
            } else {
                Toast.makeText(this, "Perintah tidak boleh kosong", Toast.LENGTH_SHORT).show()
            }
        }

        permissionButton.setOnClickListener {
            startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
        }

        NotificationService.listener = { message ->
            runOnUiThread {
                sendToBle(message)
            }
        }

        // Validasi device support BLE dan Bluetooth aktif
        if (bluetoothAdapter == null || !packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "Perangkat tidak mendukung BLE", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            Toast.makeText(this, "Aktifkan Bluetooth terlebih dahulu", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        bleScanner = bluetoothAdapter.bluetoothLeScanner ?: run {
            Toast.makeText(this, "BLE Scanner tidak tersedia", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        listView.adapter = adapter

        requestPermissions()

        scanButton.setOnClickListener {
            if (hasAllPermissions()) {
                startScan()
            } else {
                requestPermissions()
            }
        }

        listView.setOnItemClickListener { _, _, position, _ ->
            val device = deviceList[position]
            val name = if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED)
                device.name ?: device.address else device.address

            Toast.makeText(this, "Connecting to $name", Toast.LENGTH_SHORT).show()

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                bluetoothGatt?.close() // Close previous GATT if any
                bluetoothGatt = device.connectGatt(this, false, gattCallback)
            } else {
                Toast.makeText(this, "Permission BLUETOOTH_CONNECT belum diberikan", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendToBle(text: String) {
        if (::characteristicToWrite.isInitialized && bluetoothGatt != null) {
            characteristicToWrite.setValue(text.toByteArray())
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                val success = bluetoothGatt!!.writeCharacteristic(characteristicToWrite)

                if (success) {
                    Toast.makeText(this, "Terkirim: $text", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Gagal mengirim ke BLE", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Permission BLUETOOTH_CONNECT belum diberikan", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Belum terhubung ke karakteristik BLE", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startScan() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permission belum diberikan!", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "Scanning BLE...", Toast.LENGTH_SHORT).show()

        val scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device
                if (
                    ActivityCompat.checkSelfPermission(this@MainActivity, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                    !deviceList.contains(device)
                ) {
                    val name = try {
                        device.name
                    } catch (e: SecurityException) {
                        null
                    }

                    if (name != null) {
                        deviceList.add(device)
                        adapter.add("$name (${device.address})")
                    }
                }
            }

            override fun onScanFailed(errorCode: Int) {
                Toast.makeText(this@MainActivity, "Scan gagal: $errorCode", Toast.LENGTH_SHORT).show()
            }
        }

        bleScanner.startScan(scanCallback)

        Handler(Looper.getMainLooper()).postDelayed({
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                bleScanner.stopScan(scanCallback)
                Toast.makeText(this, "Scan selesai", Toast.LENGTH_SHORT).show()
            }
        }, 5000)
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
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
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

    override fun onDestroy() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            bluetoothGatt?.close()
        }
        bluetoothGatt = null
        super.onDestroy()
    }
}
