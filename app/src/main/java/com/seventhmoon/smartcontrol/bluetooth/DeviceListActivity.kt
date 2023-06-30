package com.seventhmoon.smartcontrol.bluetooth

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View

import android.widget.*
import androidx.core.text.HtmlCompat
import com.seventhmoon.smartcontrol.MainActivity
import com.seventhmoon.smartcontrol.R


class DeviceListActivity: Activity() {
    /**
     * Tag for Log
     */
    private val mTAG = "DeviceListActivity"

    /**
     * Return Intent extra
     */
    private var extraDeviceAddress = "device_address"

    /**
     * Member fields
     */
    private var mBtAdapter: BluetoothAdapter? = null

    /**
     * Newly discovered devices
     */
    private var mNewDevicesArrayAdapter: ArrayAdapter<String>? = null

    //private val REQUEST_ID_MULTIPLE_PERMISSIONS = 1

    //private val REQUEST_CONNECT_DEVICE_SECURE = 1
    //private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private val requestEnableBt = 3

    private var setDev = 0
    private val devScanner = 4
    private val devPrinter = 5

    private var pairedListView: ListView? = null

    private var progressBar: ProgressBar? = null
    private var relativeLayout: RelativeLayout? = null

    var scanButton: Button? = null
    private var textViewPairedDeviceTitle: TextView? = null
    private var pairedDevicesArrayAdapter: ArrayAdapter<String>? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Setup the window
        //requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS)
        setContentView(R.layout.activity_device_list)

        textViewPairedDeviceTitle = findViewById(R.id.title_paired_devices)

        relativeLayout = findViewById(R.id.devicelist_container)
        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleLarge)
        val params = RelativeLayout.LayoutParams(MainActivity.screenHeight / 4, MainActivity.screenWidth / 4)
        params.addRule(RelativeLayout.CENTER_IN_PARENT)
        relativeLayout!!.addView(progressBar, params)
        progressBar!!.visibility = View.GONE

        // Set result CANCELED in case the user backs out
        setResult(RESULT_CANCELED)


        setDev = intent.getIntExtra("setDev", 5)


        scanButton = findViewById(R.id.button_scan)
        scanButton!!.setOnClickListener { 

            if (mBtAdapter != null) {
                scanButton!!.visibility = View.GONE

                if (mBtAdapter!!.isEnabled) {
                    progressBar!!.visibility = View.VISIBLE
                    doDiscovery()
                    //v.visibility = View.GONE
                } else {
                    val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    startActivityForResult(enableIntent, requestEnableBt)
                }
            } else {
                toast("mBtAdapter = null")
            }




        }

        val btnCancel = findViewById<Button>(R.id.button_cancel)
        btnCancel.setOnClickListener {
            finish()
        }

        // Initialize array adapters. One for already paired devices and
        // one for newly discovered devices
        pairedDevicesArrayAdapter = ArrayAdapter(this, R.layout.device_name)
        mNewDevicesArrayAdapter = ArrayAdapter(this, R.layout.device_name)

        // Find and set up the ListView for paired devices
        pairedListView = findViewById(R.id.paired_devices)
        pairedListView!!.adapter = pairedDevicesArrayAdapter
        pairedListView!!.onItemClickListener = mDeviceClickListener

        // Find and set up the ListView for newly discovered devices
        val newDevicesListView = findViewById<ListView>(R.id.new_devices)
        newDevicesListView.adapter = mNewDevicesArrayAdapter
        newDevicesListView.onItemClickListener = mDeviceClickListener

        // Register for broadcasts when a device is discovered
        var filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        this.registerReceiver(mReceiver, filter)

        // Register for broadcasts when discovery has finished
        filter = IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        this.registerReceiver(mReceiver, filter)

        // Get the local Bluetooth adapter
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        mBtAdapter = bluetoothManager.adapter
        //mBtAdapter = BluetoothAdapter.getDefaultAdapter()

        if (mBtAdapter != null) {
            // Get a set of currently paired devices
            val pairedDevices = mBtAdapter!!.bondedDevices

            // If there are paired devices, add each one to the ArrayAdapter
            if (pairedDevices.size > 0) {
                textViewPairedDeviceTitle!!.visibility = View.VISIBLE
                for (device in pairedDevices) {
                    pairedDevicesArrayAdapter!!.add(device.name + "\n" + device.address)
                }
            } else {
                val noDevices = resources.getText(R.string.none_paired).toString()
                pairedDevicesArrayAdapter!!.add(noDevices)
            }
        }


    }

    override fun onDestroy() {
        super.onDestroy()

        // Make sure we're not doing discovery anymore
        if (mBtAdapter != null) {
            mBtAdapter!!.cancelDiscovery()
        }

        // Unregister broadcast listeners
        this.unregisterReceiver(mReceiver)
    }

    /**
     * Start device discover with the BluetoothAdapter
     */
    private fun doDiscovery() {
        Log.d(mTAG, "doDiscovery()")

        // Indicate scanning in the title
        //setProgressBarIndeterminateVisibility(true)
        setTitle(R.string.bluetooth_setting)

        if (mNewDevicesArrayAdapter != null) {
            mNewDevicesArrayAdapter!!.clear()

            //val noDevices = resources.getText(R.string.device_scan).toString()
            //mNewDevicesArrayAdapter!!.add(noDevices)

            //mNewDevicesArrayAdapter!!.notifyDataSetChanged()
        }

        if (mBtAdapter != null) {
            //paired device
            if (pairedDevicesArrayAdapter != null) {
                pairedDevicesArrayAdapter!!.clear()

                val pairedDevices = mBtAdapter!!.bondedDevices

                // If there are paired devices, add each one to the ArrayAdapter
                if (pairedDevices.size > 0) {
                    textViewPairedDeviceTitle!!.visibility = View.VISIBLE
                    for (device in pairedDevices) {
                        pairedDevicesArrayAdapter!!.add(device.name + "\n" + device.address)
                    }
                } else {
                    val noDevices = resources.getText(R.string.none_paired).toString()
                    pairedDevicesArrayAdapter!!.add(noDevices)
                }
            }




            // Turn on sub-title for new devices
            findViewById<View>(R.id.title_new_devices).visibility = View.VISIBLE

            // If we're already discovering, stop it
            if (mBtAdapter!!.isDiscovering) {
                mBtAdapter!!.cancelDiscovery()
            }

            // Request discover from BluetoothAdapter
            mBtAdapter!!.startDiscovery()
        } else {
            Log.e(mTAG, "mBtAdapter = null")
        }


    }

    /**
     * The on-click listener for all devices in the ListViews
     */
    private val mDeviceClickListener = AdapterView.OnItemClickListener { _, v, _, _ ->
        // Cancel discovery because it's costly and we're about to connect
        when (setDev) {
            devScanner -> {
                if (mBtAdapter != null) {
                    mBtAdapter!!.cancelDiscovery()
                    val intent = Intent()
                    val info = (v as TextView).text.toString()
                    val address = info.substring(info.length - 17)
                    intent.putExtra("SannerAddres", address)
                    setResult(RESULT_OK, intent)
                    finish()
                } else {
                    Log.e(mTAG, "mBtAdapter = null")
                }
            }
            devPrinter -> {
                if (mBtAdapter != null) {
                    mBtAdapter!!.cancelDiscovery()
                    val intent = Intent()
                    val info = (v as TextView).text.toString()
                    if (info.length >= 17) {
                        val address = info.substring(info.length - 17)
                        Log.e(
                            mTAG, "bluetooth " +
                                    "" +
                                    "address = " + address
                        )
                        intent.putExtra("PrinterAddress", address)
                        setResult(RESULT_OK, intent)
                        finish()
                    }
                } else {
                    Log.e(mTAG, "mBtAdapter = null")
                }
            }
            else -> {
                if (mBtAdapter != null) {
                    mBtAdapter!!.cancelDiscovery()

                    // Get the device MAC address, which is the last 17 chars in the View
                    val info = (v as TextView).text.toString()
                    val address = info.substring(info.length - 17)

                    // Create the result Intent and include the MAC address
                    val intent = Intent()
                    intent.putExtra(extraDeviceAddress, address)

                    // Set result and finish this Activity
                    setResult(RESULT_OK, intent)
                    finish()
                } else {
                    Log.e(mTAG, "mBtAdapter = null")
                }
            }
        }
    }

    /**
     * The BroadcastReceiver that listens for discovered devices and changes the title when
     * discovery is finished
     */
    private val mReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action

            /*if (mNewDevicesArrayAdapter != null) {
                mNewDevicesArrayAdapter!!.clear()
            }*/

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND == action) {
                // Get the BluetoothDevice object from the Intent
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                // If it's already paired, skip it, because it's been listed already
                if (device != null) {
                    if (device.bondState != BluetoothDevice.BOND_BONDED) {
                        mNewDevicesArrayAdapter!!.add(device.name + "\n" + device.address)
                    }
                }
                //scanButton!!.visibility = View.VISIBLE
                // When discovery is finished, change the Activity title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED == action) {
                progressBar!!.visibility = View.GONE
                //setProgressBarIndeterminateVisibility(false)
                setTitle(R.string.select_device)
                if (mNewDevicesArrayAdapter!!.count == 0) {
                    val noDevices = resources.getText(R.string.none_found).toString()
                    mNewDevicesArrayAdapter!!.add(noDevices)

                }
                scanButton!!.visibility = View.VISIBLE
                toast(getString(R.string.device_scan_finish))
            }



        }
    }

    fun toast(message: String) {

        val toast = Toast.makeText(this, HtmlCompat.fromHtml("<h1>$message</h1>", HtmlCompat.FROM_HTML_MODE_COMPACT), Toast.LENGTH_LONG)
        toast.setGravity(Gravity.CENTER_HORIZONTAL or Gravity.CENTER_VERTICAL, 0, 0)

        /*val toast = Toast.makeText(this, message, Toast.LENGTH_SHORT)
        toast.setGravity(Gravity.CENTER_HORIZONTAL or Gravity.CENTER_VERTICAL, 0, 0)
        val group = toast.view as ViewGroup
        val textView = group.getChildAt(0) as TextView
        textView.textSize = 30.0f*/
        toast.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.e(mTAG, "onActivityResult = request code = $requestCode, resultCode = $resultCode")

        when (requestCode) {
            /*REQUEST_CONNECT_DEVICE_SECURE -> {
                Log.e(mTAG, "REQUEST_CONNECT_DEVICE_SECURE")
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    //connectDevice(data, true)
                }
            }*/


            requestEnableBt ->
                // When the request to enable Bluetooth returns
                when(resultCode) {
                    RESULT_OK-> {
                        Log.d(mTAG, "BT enabled")




                        if (mBtAdapter != null) {

                            pairedDevicesArrayAdapter!!.clear()

                            if (pairedDevicesArrayAdapter != null) {
                                val pairedDevices = mBtAdapter!!.bondedDevices

                                // If there are paired devices, add each one to the ArrayAdapter
                                if (pairedDevices.size > 0) {
                                    textViewPairedDeviceTitle!!.visibility = View.VISIBLE
                                    for (device in pairedDevices) {
                                        pairedDevicesArrayAdapter!!.add(device.name + "\n" + device.address)
                                    }
                                } else {
                                    val noDevices = resources.getText(R.string.none_paired).toString()
                                    pairedDevicesArrayAdapter!!.add(noDevices)
                                }
                            }

                            //scan
                            progressBar!!.visibility = View.VISIBLE
                            doDiscovery()

                        }
                    }

                    else -> {
                        Log.d(mTAG, "BT not enabled")
                        scanButton!!.visibility = View.VISIBLE
                        toast(getString(R.string.bt_not_enabled_leaving))
                    }
                }



        }
    }
}