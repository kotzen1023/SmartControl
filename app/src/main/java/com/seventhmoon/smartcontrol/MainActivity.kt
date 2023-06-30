package com.seventhmoon.smartcontrol

import android.Manifest
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.widget.Toast
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
import com.seventhmoon.smartcontrol.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private val mTAG = MainActivity::class.java.name

    private val requestIdMultiplePermission = 1

    private val requestConnectDeviceSecure = 1

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    companion object {
        @JvmStatic var screenWidth: Int = 0
        @JvmStatic var screenHeight: Int = 0

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    private fun checkAndRequestPermissions() {

        val bluetoothAdminPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN)

        val bluetoothPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH)

        var bluetoothConnect = 0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            bluetoothConnect = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
        }

        val listPermissionsNeeded = ArrayList<String>()

        if (bluetoothAdminPermission != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.BLUETOOTH_ADMIN)
        }

        if (bluetoothPermission != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.BLUETOOTH)
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
}