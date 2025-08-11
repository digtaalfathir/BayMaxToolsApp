package com.example.blescannerapp

import android.Manifest
import android.bluetooth.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class ControlActivity : AppCompatActivity() {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private var bluetoothGatt: BluetoothGatt? = null
    private lateinit var characteristicToWrite: BluetoothGattCharacteristic

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_control)

        val inputCommand = findViewById<EditText>(R.id.inputCommand)
        val sendButton = findViewById<Button>(R.id.sendButton)
        val sendTime = findViewById<Button>(R.id.sendTime)
        val permissionButton: Button = findViewById(R.id.permissionButton)
        val settingsButton: ImageButton = findViewById(R.id.btnSettings) // tombol baru

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        val deviceAddress = intent.getStringExtra("DEVICE_ADDRESS")
        if (deviceAddress != null) {
            val device = bluetoothAdapter.getRemoteDevice(deviceAddress)
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                bluetoothGatt = device.connectGatt(this, false, gattCallback)
            }
        }

        sendButton.setOnClickListener {
            val textToSend = inputCommand.text.toString()
            if (textToSend.isNotBlank()) {
                sendToBle(textToSend)
            } else {
                Toast.makeText(this, "Perintah tidak boleh kosong", Toast.LENGTH_SHORT).show()
            }
        }

        sendTime.setOnClickListener {
            val calendar = java.util.Calendar.getInstance()
            val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)
            val month = calendar.get(java.util.Calendar.MONTH) + 1 // bulan dimulai dari 0
            val year = calendar.get(java.util.Calendar.YEAR)
            val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
            val minute = calendar.get(java.util.Calendar.MINUTE)
            val second = calendar.get(java.util.Calendar.SECOND)

            val textToSend = String.format("SETTIME,%d,%d,%d,%d,%d,%d", day, month, year, hour, minute, second)

            if (textToSend.isNotBlank()) {
                sendToBle(textToSend)
            } else {
                Toast.makeText(this, "Perintah tidak boleh kosong", Toast.LENGTH_SHORT).show()
            }
        }


        permissionButton.setOnClickListener {
            startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
        }

        // Klik tombol settings untuk buka halaman pengaturan
        settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        NotificationService.listener = { message ->
            runOnUiThread {
                sendToBle(message)
            }
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                        gatt.discoverServices()
                    }
                } else {
                    gatt.discoverServices()
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            for (service in gatt.services) {
                for (characteristic in service.characteristics) {
                    val props = characteristic.properties
                    if ((props and BluetoothGattCharacteristic.PROPERTY_WRITE) > 0 ||
                        (props and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) > 0
                    ) {
                        characteristicToWrite = characteristic
                        break
                    }
                }
            }
        }
    }

    private fun sendToBle(text: String) {
        if (::characteristicToWrite.isInitialized && bluetoothGatt != null) {
            characteristicToWrite.setValue(text.toByteArray())
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                bluetoothGatt!!.writeCharacteristic(characteristicToWrite)
            }
        }
    }

    override fun onDestroy() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                bluetoothGatt?.close()
            }
        } else {
            bluetoothGatt?.close()
        }
        super.onDestroy()
    }
}
