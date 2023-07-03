package com.seventhmoon.smartcontrol

import android.Manifest
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import com.seventhmoon.smartcontrol.bluetooth.BluetoothChatService
import com.seventhmoon.smartcontrol.bluetooth.DeviceListActivity
import com.seventhmoon.smartcontrol.data.Constants.BluetoothState.Companion.MESSAGE_DEVICE_NAME
import com.seventhmoon.smartcontrol.data.Constants.BluetoothState.Companion.MESSAGE_READ
import com.seventhmoon.smartcontrol.data.Constants.BluetoothState.Companion.MESSAGE_STATE_CHANGE
import com.seventhmoon.smartcontrol.data.Constants.BluetoothState.Companion.MESSAGE_TOAST
import com.seventhmoon.smartcontrol.data.Constants.BluetoothState.Companion.MESSAGE_WRITE
import com.seventhmoon.smartcontrol.databinding.ActivityMainBinding
import java.lang.ref.WeakReference

class MainActivity : AppCompatActivity() {
    private val mTAG = MainActivity::class.java.name

    private val requestIdMultiplePermission = 1

    private val requestConnectDeviceSecure = 1

    private val requestEnableBt = 3

    private val setPrinterDev = 5

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private var mContext: Context? = null
    private var toastHandle: Toast? = null

    private var menuItemBluetoothScan: MenuItem? = null

    companion object {
        @JvmStatic var screenWidth: Int = 0
        @JvmStatic var screenHeight: Int = 0
        @JvmStatic var mConnectedDeviceName: String = ""
        @JvmStatic var currentRequestCode: Int = 0

    }
    private var mBluetoothAdapter: BluetoothAdapter? = null
    var mChatService: BluetoothChatService? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mContext = applicationContext

        setSupportActionBar(binding.appBarMain.toolbar)

        binding.appBarMain.fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }
        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkAndRequestPermissions()
        } else {
            //initView()
            //if (isLogEnable)
            //    initLog()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        menuItemBluetoothScan = menu.findItem(R.id.action_settings)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.action_settings -> {
                Log.e(mTAG, "action_settings")
                openSomeActivityForResult()
                //val intent = Intent(this, DeviceListActivity::class.java)
                //intent.putExtra("SET_DEV", setPrinterDev)
                //startActivityForResult(intent, setPrinterDev)
            }
        }

        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    private fun checkAndRequestPermissions() {

        val bluetoothAdminPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN)

        val bluetoothPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH)

        val bluetoothScanPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)

        var bluetoothConnect = 0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Log.e(mTAG, "Build.VERSION.SDK_INT >= Build.VERSION_CODES.S")
            bluetoothConnect = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
        }

        val listPermissionsNeeded = ArrayList<String>()

        if (bluetoothAdminPermission != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.BLUETOOTH_ADMIN)
        }

        if (bluetoothPermission != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.BLUETOOTH)
        }

        if (bluetoothScanPermission != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.BLUETOOTH_SCAN)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (bluetoothConnect != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
        }


        if (listPermissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                listPermissionsNeeded.toTypedArray(),
                requestIdMultiplePermission
            )
            //return false;
        } else {
            Log.e(mTAG, "All permission are granted")
            //initView()
            //initLog()
        }
        //return true;
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        Log.d(mTAG, "Permission callback called------- permissions.size = ${permissions.size}")
        when (requestCode) {
            requestIdMultiplePermission -> {

                val perms: HashMap<String, Int> = HashMap()

                // Initialize the map with both permissions

                perms[Manifest.permission.BLUETOOTH_ADMIN] = PackageManager.PERMISSION_GRANTED
                perms[Manifest.permission.BLUETOOTH] = PackageManager.PERMISSION_GRANTED
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    perms[Manifest.permission.BLUETOOTH_CONNECT] = PackageManager.PERMISSION_GRANTED
                }
                perms[Manifest.permission.BLUETOOTH_SCAN] = PackageManager.PERMISSION_GRANTED

                // Fill with actual results from user
                //if (grantResults.size > 0) {
                if (grantResults.isNotEmpty()) {
                    for (i in permissions.indices) {
                        perms[permissions[i]] = grantResults[i]
                        Log.e(mTAG, "perms[permissions[$i]] = ${permissions[i]}")

                    }
                    // Check for both permissions
                    if (perms[Manifest.permission.BLUETOOTH_ADMIN] == PackageManager.PERMISSION_GRANTED
                        && perms[Manifest.permission.BLUETOOTH] == PackageManager.PERMISSION_GRANTED
                        && perms[Manifest.permission.BLUETOOTH_CONNECT] == PackageManager.PERMISSION_GRANTED
                        && perms[Manifest.permission.BLUETOOTH_SCAN] == PackageManager.PERMISSION_GRANTED
                    ) {
                        Log.d(mTAG, "write permission granted")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            if (perms[Manifest.permission.BLUETOOTH_CONNECT] == PackageManager.PERMISSION_GRANTED) {
                                Log.e(mTAG, "BLUETOOTH_CONNECT is permitted.")

                            } else {
                                Log.e(mTAG, "BLUETOOTH_CONNECT not permitted.")
                            }


                            //initView()
                            //initLog()
                        } else {
                            //initView()
                            //initLog()
                        }
                        // process the normal flow
                        //else any one or both the permissions are not granted
                        //init_folder_and_files()
                        //init_setting();

                    } else {
                        Log.d(mTAG, "Some permissions are not granted ask again ")
                        //permission is denied (this is the first time, when "never ask again" is not checked) so ask again explaining the usage of permission
                        //                        // shouldShowRequestPermissionRationale will return true
                        //show the dialog or snackbar saying its necessary and try again otherwise proceed with setup.
                        if (ActivityCompat.shouldShowRequestPermissionRationale(
                                this,
                                Manifest.permission.BLUETOOTH_ADMIN
                            )
                            || ActivityCompat.shouldShowRequestPermissionRationale(
                                this,
                                Manifest.permission.BLUETOOTH
                            )
                            || ActivityCompat.shouldShowRequestPermissionRationale(
                                this,
                                Manifest.permission.BLUETOOTH_CONNECT
                            )
                            || ActivityCompat.shouldShowRequestPermissionRationale(
                                this,
                                Manifest.permission.BLUETOOTH_SCAN
                            )
                        ) {
                            showDialogOK { _, which ->
                                when (which) {
                                    DialogInterface.BUTTON_POSITIVE -> checkAndRequestPermissions()
                                    DialogInterface.BUTTON_NEGATIVE ->
                                        // proceed with logic by disabling the related features or quit the app.
                                        finish()
                                }
                            }
                        } else {
                            Toast.makeText(this, "Go to settings and enable permissions", Toast.LENGTH_LONG)
                                .show()
                            //                            //proceed with logic by disabling the related features or quit the app.
                        }//|| ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_NETWORK_STATE )
                        //|| ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_WIFI_STATE )
                        //permission is denied (and never ask again is  checked)
                        //shouldShowRequestPermissionRationale will return false
                    }//&& perms.get(Manifest.permission.ACCESS_NETWORK_STATE) == PackageManager.PERMISSION_GRANTED &&
                    //perms.get(Manifest.permission.ACCESS_WIFI_STATE) == PackageManager.PERMISSION_GRANTED
                }
            }
        }

    }

    private fun showDialogOK(okListener: DialogInterface.OnClickListener) {
        AlertDialog.Builder(this)
            .setMessage("Warning")
            .setPositiveButton("Ok", okListener)
            .setNegativeButton("Cancel", okListener)
            .create()
            .show()
    }

    private class BluetoothHandler(activity: MainActivity) : Handler(Looper.getMainLooper()) {
        private val mActivity: WeakReference<MainActivity> = WeakReference(activity)
        private val mTAG = MainActivity::class.java.name

        override fun handleMessage(msg: Message) {

            if (mActivity.get() == null) {
                return
            }
            val activity = mActivity.get()

            when (msg.what) {
                MESSAGE_STATE_CHANGE -> when (msg.arg1) {
                    BluetoothChatService.STATE_CONNECTED -> {
                        //toast("Connected to $mConnectedDeviceName")
                        Log.e(mTAG, "Connected to $mConnectedDeviceName")

                    }
                    BluetoothChatService.STATE_CONNECTING -> {
                        //toast(getString(R.string.title_connecting))
                        Log.e(mTAG, "connecting...")

                    }
                    BluetoothChatService.STATE_LISTEN, BluetoothChatService.STATE_NONE -> {
                        Log.e(mTAG, "not connected")



                    }

                }

                MESSAGE_READ -> {
                }

                MESSAGE_WRITE -> {
                }


                MESSAGE_DEVICE_NAME -> {
                    // save the connected device's name
                    mConnectedDeviceName = msg.data.getString("device_name") as String
                    if (null != activity) {
                        Toast.makeText(activity, "Connected to $mConnectedDeviceName", Toast.LENGTH_SHORT).show()

                    }
                }
                MESSAGE_TOAST -> if (null != activity) {
                    Toast.makeText(
                        activity, msg.data.getString("toast"),
                        Toast.LENGTH_SHORT
                    ).show()


                }
            }

        }
    }

    private fun setupChat() {
        Log.e(mTAG, "setupChat()")
        //mChatService = BluetoothChatService(mContext as Context, mHandler)

        val blHandler = BluetoothHandler(this)

        mChatService = BluetoothChatService(mContext as Context, blHandler)
    }

    private var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        Log.e(mTAG, "result.resultCode = $result.resultCode")
        when (currentRequestCode) {
            requestConnectDeviceSecure -> {
                Log.e(mTAG, "requestConnectDeviceSecure")
                // When DeviceListActivity returns with a device to connect
                if (result.resultCode == RESULT_OK) {
                    //connectDevice(data, true)
                    Log.d(mTAG, "requestConnectDeviceSecure RESULT_OK")
                }
            }

            setPrinterDev -> {
                Log.e(mTAG, "setPrinterDev")
                if (result.resultCode == RESULT_OK) {
                    Log.e(mTAG, "RESULT_OK")
                    //if (data?.getExtras() != null) {
                    val data: Intent? = result.data
                    if (data?.extras != null) {
                        Log.e(mTAG, "data.getExtras() = " + data.extras.toString())
                        /*
                        printerAddress = data.getStringExtra("PrinterAddress") as String
                        //PrinterAddres = data.getExtras()
                        //        .getString("PrinterAddress");
                        Log.e(mTAG, "PrinterAddress = $printerAddress")
                        //SaveBluetoothDev(1)

                        //save printer address
                        editor = pref!!.edit()
                        editor!!.putString("PRINTER_ADDRESS", printerAddress)
                        editor!!.apply()

                        //connect
                        if (mChatService != null) {
                            mChatService!!.stop()
                            Thread.sleep(500)

                            //connect printer
                            if (printerAddress == "") {
                                toast(getString(R.string.set_printer_first))
                            } else {
                                if (mBluetoothAdapter != null) {
                                    val device = mBluetoothAdapter!!.getRemoteDevice(printerAddress)
                                    mChatService!!.connect(device, true)
                                    //set printer
                                    bluetoothPrintFunc = BluetoothPrinterFuncs(mChatService as BluetoothChatService)
                                    if (bluetoothPrintFunc != null) {
                                        Log.e(mTAG, "Bluetooth Printer ready.")
                                    } else {
                                        Log.e(mTAG, "bluetoothPrintFunc == null")
                                    }
                                } else {
                                    Log.e(mTAG, "mBluetoothAdapter = null")
                                }
                            }
                        } else{
                            Log.e(mTAG, "mChatService = null")
                            if (!mBluetoothAdapter!!.isEnabled) {
                                currentRequestCode = requestEnableBt
                                openSomeActivityForResult()
                                //val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                                //startActivityForResult(enableIntent, requestEnableBt)
                                // Otherwise, setup the chat session
                            } else {
                                Log.d(mTAG, "===>mBluetoothAdapter is enabled")
                                setupChat()
                                if (mChatService != null) {
                                    // Only if the state is STATE_NONE, do we know that we haven't started already
                                    if (mChatService!!.getState() == BluetoothChatService.STATE_NONE) {
                                        Log.d(mTAG, "--->mChatService start")
                                        // Start the Bluetooth chat services
                                        mChatService!!.start()
                                    }



                                    //connect printer
                                    if (printerAddress == "") {
                                        toast(getString(R.string.set_printer_first))
                                    } else {
                                        val device = mBluetoothAdapter!!.getRemoteDevice(printerAddress)
                                        mChatService!!.connect(device, true)
                                        //set printer
                                        bluetoothPrintFunc = BluetoothPrinterFuncs(mChatService as BluetoothChatService)
                                        if (bluetoothPrintFunc != null) {
                                            Log.e(mTAG, "Bluetooth Printer ready.")
                                        } else {
                                            Log.e(mTAG, "bluetoothPrintFunc == null")
                                        }
                                    }

                                } else {
                                    Log.e(mTAG, "mChatService = null")
                                }
                            }
                        }*/
                    }

                }
            }
            requestEnableBt ->
                // When the request to enable Bluetooth returns
                if (result.resultCode == RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    setupChat()

                    if (mChatService != null) {
                        // Only if the state is STATE_NONE, do we know that we haven't started already
                        if (mChatService!!.getState() == BluetoothChatService.STATE_NONE) {
                            // Start the Bluetooth chat services
                            Log.d(mTAG, "--->mChatService start")
                            mChatService!!.start()
                        }



                        //connect printer
                        /*if (printerAddress == "") {
                            toast(getString(R.string.set_printer_first))
                        } else {
                            val device = mBluetoothAdapter!!.getRemoteDevice(printerAddress)
                            mChatService!!.connect(device, true)
                            //set printer
                            bluetoothPrintFunc = BluetoothPrinterFuncs(mChatService as BluetoothChatService)
                            if (bluetoothPrintFunc != null) {
                                Log.e(mTAG, "Bluetooth Printer ready.")
                            } else {
                                Log.e(mTAG, "bluetoothPrintFunc == null")
                            }
                        }*/

                    } else {
                        Log.e(mTAG, "mChatService = null")
                    }


                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(mTAG, "BT not enabled")
                    toast(getString(R.string.bt_not_enabled_leaving))
                }
        }

        /*if (result.resultCode == Activity.RESULT_OK) {
            // There are no request codes
            val data: Intent? = result.data

        }*/
    }

    private fun toast(message: String) {

        if (toastHandle != null) {
            toastHandle!!.cancel()
        }

        toastHandle = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            val toast = Toast.makeText(this, HtmlCompat.fromHtml("<h1>$message</h1>", HtmlCompat.FROM_HTML_MODE_COMPACT), Toast.LENGTH_SHORT)
            toast.setGravity(Gravity.CENTER_HORIZONTAL or Gravity.CENTER_VERTICAL, 0, 0)
            toast.show()

            toast
        } else { //Android 11
            val toast = Toast.makeText(this, message, Toast.LENGTH_SHORT)
            toast.show()

            toast
        }
        /*val group = toast.view as ViewGroup
        val textView = group.getChildAt(0) as TextView
        textView.textSize = 30.0f*/



    }

    private fun toastLong(message: String) {

        if (toastHandle != null)
            toastHandle!!.cancel()

        toastHandle = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            val toast = Toast.makeText(this, HtmlCompat.fromHtml("<h1>$message</h1>", HtmlCompat.FROM_HTML_MODE_COMPACT), Toast.LENGTH_LONG)
            toast.setGravity(Gravity.CENTER_HORIZONTAL or Gravity.CENTER_VERTICAL, 0, 0)
            toast.show()

            toast
        } else { //Android 11
            val toast = Toast.makeText(this, message, Toast.LENGTH_LONG)
            toast.show()

            toast
        }

        /*val group = toast.view as ViewGroup
        val textView = group.getChildAt(0) as TextView
        textView.textSize = 30.0f*/

    }

    private fun initView() {



        //init bluetooth
        //mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        mBluetoothAdapter = bluetoothManager.adapter
        if (mBluetoothAdapter == null) {

            toastLong("Bluetooth is not available")
        } else {
            Log.d(mTAG, "Bluetooth is available")

            //check if bluetooth is enabled
            if (!mBluetoothAdapter!!.isEnabled) {
                currentRequestCode = requestEnableBt
                openSomeActivityForResult()
                //val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                //startActivityForResult(enableIntent, requestEnableBt)
                // Otherwise, setup the chat session
            } else {
                Log.d(mTAG, "===>mBluetoothAdapter is enabled")
                setupChat()
                if (mChatService != null) {
                    // Only if the state is STATE_NONE, do we know that we haven't started already
                    if (mChatService!!.getState() == BluetoothChatService.STATE_NONE) {
                        Log.d(mTAG, "--->mChatService start")
                        // Start the Bluetooth chat services
                        mChatService!!.start()
                    }

                    //connect printer
                    /*
                    if (printerAddress == "") {
                        toast(getString(R.string.set_printer_first))
                    } else {
                        val device = mBluetoothAdapter!!.getRemoteDevice(printerAddress)
                        mChatService!!.connect(device, true)
                        //set printer
                        bluetoothPrintFunc = BluetoothPrinterFuncs(mChatService as BluetoothChatService)
                        if (bluetoothPrintFunc != null) {
                            Log.e(mTAG, "Bluetooth Printer ready.")
                        } else {
                            Log.e(mTAG, "bluetoothPrintFunc == null")
                        }
                    }
                    */

                } else {
                    Log.e(mTAG, "mChatService = null")
                }
            }

        }
    }

    fun openSomeActivityForResult() {
        if (currentRequestCode == requestEnableBt) {
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            resultLauncher.launch(intent)
        //} else if (currentRequestCode == setPrinterDev) {
        } else {
            val intent = Intent(this, DeviceListActivity::class.java)
            intent.putExtra("SET_DEV", setPrinterDev)
            resultLauncher.launch(intent)
        }

    }
}