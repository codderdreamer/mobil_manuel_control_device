package com.atlastek.manuelcontrolapp

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.ParcelUuid
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.atlastek.manuelcontrolapp.databinding.ActivityHandedectBinding
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.IOException
import java.lang.Integer.min
import java.util.UUID
import org.tensorflow.lite.InterpreterApi
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.nio.ByteBuffer
import java.nio.ByteOrder

lateinit var activity_binding: ActivityHandedectBinding

open class HandedectActivity : AppCompatActivity() {

    private lateinit var m_pairedDevices: Set<BluetoothDevice>
    private val REQUEST_ENABLE_BLUETOOTH = 1

    companion object {
        var m_myUUID:UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        var m_bluetoothSocket: BluetoothSocket? = null
        lateinit var m_progress: ProgressDialog
        var m_isConnected: Boolean = false
        var m_bluetoothAdapter: BluetoothAdapter? = null
        var m_device: BluetoothDevice? = null
        var m_address: String? = null
        var filew : FileWriter? = null
        var filer : FileReader? = null
        var path : String = ""
        var file_line = ""

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity_binding = ActivityHandedectBinding.inflate(layoutInflater)
        setContentView(activity_binding.root)

        path = this.filesDir.path
        try {

            var file = File( path + "record.txt")
            val isNewFileCreated :Boolean = file.createNewFile()
            if(isNewFileCreated){
                println("record.txt is created successfully.")
            } else{
                println("record.txt already exists.")
            }
            filew = FileWriter(path + "record.txt")
            filer = FileReader(path + "record.txt")



        } catch (hata:Exception) {
            println("Hata: " + hata)
        }



        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestMultiplePermissions.launch(arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT))
        }
        else{
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            requestBluetooth.launch(enableBtIntent)
        }

        m_bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        if(m_bluetoothAdapter == null){
           Toast.makeText(this, "This device doesn't support bluetooth", Toast.LENGTH_SHORT).show()
            return
        }

        if(!m_bluetoothAdapter!!.isEnabled){
            val enabledBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                Toast.makeText(this, "No permission to turn on bluetooth!", Toast.LENGTH_SHORT).show()
                return
            }
            startActivityForResult(enabledBluetoothIntent,REQUEST_ENABLE_BLUETOOTH)
        }

        activity_binding.selectDeviceRefresh.setOnClickListener{
            pairedDeviceList()
        }
    }

    class UpdateText: Runnable, HandedectActivity()
    {
        override fun run() {
            var text : String? = null
            var new_text = ""
            var text_parse_list : List<Int> = listOf()

            var file_lines = ""
            activity_binding.add.setOnClickListener{
                file_lines = file_lines + "\n" + file_line
                println(file_lines)
            }
            while (true){
                if(m_isConnected==true)
                {
                    try {
                        val available  = m_bluetoothSocket?.inputStream?.available()
                        var bytes = available?.let { ByteArray(it) }
                        if (available != null) {
                            new_text = ""
                            file_line = ""
                            m_bluetoothSocket?.inputStream?.read(bytes, 0 , available)

                            text = bytes?.let { String(it) }
                            if (text != null) {
                                for (i in 85 downTo 1) {
                                    new_text = new_text + text[text.length - i]                                                    // gelen değerleri aldığımız yer
                                }
                            }
                        }
                        //println(new_text)
                        val text_parse = new_text.split("\n").toTypedArray()
                        //println(text_parse)
                        new_text = ""
                        for(value in text_parse)
                        {
                            if( (("R" in value) && !("R" in new_text)) || (("S" in value) && !("S" in new_text)) || (("T" in value) && !("T" in new_text)) || (("U" in value) && !("U" in new_text)) || (("V" in value) && !("V" in new_text)) || (("W" in value) && !("W" in new_text)) )
                            {
                                //println(value)

                                    new_text = new_text + value + "\n"
                                    file_line = file_line + value + "\t"



                            }
                        }


                        runOnUiThread {
                            activity_binding?.connectKnowledge?.setText(new_text)

                            //filew = FileWriter(path + "record.txt",true)
                            //filew?.write(file_line + "\t NOK" + "\n")
                            //filew?.close()
                            //val bufferedReader: BufferedReader = File(path + "record.txt").bufferedReader()
                            //val inputString = bufferedReader.use { it.readText() }
                            //println("\n\n\n")
                            //println("**************************************************************************************************************************************")
                            //println(inputString)
                            //println("**************************************************************************************************************************************")
                        }
                    } catch (e: Exception) {
                        Log.e("client", "Cannot read data", e)
                    }
                }
                Thread.sleep(500)
            }
        }
    }


    private var requestBluetooth = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            //granted
        }else{
            //deny
        }
    }

    private val requestMultiplePermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach {
                Log.d("test006", "${it.key} = ${it.value}")
            }
        }

    private fun pairedDeviceList() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "No permission to turn on bluetooth!", Toast.LENGTH_SHORT).show()
            return
        }
        m_pairedDevices = m_bluetoothAdapter!!.bondedDevices
        val list : ArrayList<BluetoothDevice> = ArrayList()

        if(!m_pairedDevices.isEmpty()) {
            for(device : BluetoothDevice in m_pairedDevices){
                list.add(device)
                Log.i("device", ""+device)
            }
        } else {
            Toast.makeText(this, "no paired bluetooth devices found", Toast.LENGTH_SHORT).show()
        }

        val adapter = ArrayAdapter(this,android.R.layout.simple_list_item_1,list)
        activity_binding.selectDeviceList.adapter = adapter
        activity_binding.selectDeviceList.onItemClickListener = AdapterView.OnItemClickListener{ _, _, position, _ ->
            var device: BluetoothDevice = list[position]
            m_device = device
            var address: String = device.address
            m_address = address

            activity_binding.connectKnowledge.visibility = View.VISIBLE
            activity_binding.connectKnowledge.setTextColor(Color.parseColor("#1CB600"));
            activity_binding.connectKnowledge.text = "Connecting..."

            ConnectToDevice(this).execute()

            val threadWithRunnable = Thread(UpdateText())
            threadWithRunnable.start()

        }
    }

    private class ConnectToDevice(c: Context) : AsyncTask<Void, Void, String>()
    {
        private var connectSuccess: Boolean = true
        private val context: Context

        init {
            this.context = c
        }

        override fun onPreExecute() {
            super.onPreExecute()
            m_progress = ProgressDialog.show(context,"Connecting...", "please wait")
        }

        override fun doInBackground(vararg p0: Void?): String?
        {
            try {
                if(m_bluetoothSocket == null || !m_isConnected){
                    if (ActivityCompat.checkSelfPermission(
                            context,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        return null
                    }
                    m_bluetoothSocket = m_device?.createInsecureRfcommSocketToServiceRecord(m_myUUID)
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery()
                    m_bluetoothSocket!!.connect()
                }
            } catch (e: IOException) {
                connectSuccess = false
                e.printStackTrace()
            }
            return null
        }

        override fun onPostExecute(result: String?)
        {
            super.onPostExecute(result)
            if(!connectSuccess) {
                Log.i("data","couldn't connect")
                m_isConnected = false
            } else {
                m_isConnected = true
            }
            m_progress.dismiss()
            try {
                val available  = m_bluetoothSocket?.inputStream?.available()
                val bytes = available?.let { ByteArray(it) }
                if (available != null) {
                    m_bluetoothSocket?.inputStream?.read(bytes, 0, available)
                    val text = bytes?.let { String(it) }
                }
            } catch (e: Exception) {
                Log.e("client", "Cannot read data", e)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
    {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == REQUEST_ENABLE_BLUETOOTH) {
            if(requestCode == Activity.RESULT_OK){
                if(m_bluetoothAdapter!!.isEnabled) {
                    Toast.makeText(this, "Bluetooth has been enabled", Toast.LENGTH_SHORT).show()
                } else{
                    Toast.makeText(this, "Bluetooth has been disabled", Toast.LENGTH_SHORT).show()
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
                Toast.makeText(this, "Bluetooth enabling has been canceled", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

