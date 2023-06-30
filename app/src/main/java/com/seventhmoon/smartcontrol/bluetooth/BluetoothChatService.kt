package com.seventhmoon.smartcontrol.bluetooth

import android.bluetooth.*
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.util.Log

import com.seventhmoon.smartcontrol.R
import com.seventhmoon.smartcontrol.data.Constants.BluetoothState.Companion.MESSAGE_DEVICE_NAME
import com.seventhmoon.smartcontrol.data.Constants.BluetoothState.Companion.MESSAGE_READ
import com.seventhmoon.smartcontrol.data.Constants.BluetoothState.Companion.MESSAGE_STATE_CHANGE
import com.seventhmoon.smartcontrol.data.Constants.BluetoothState.Companion.MESSAGE_TOAST
import com.seventhmoon.smartcontrol.data.Constants.BluetoothState.Companion.MESSAGE_WRITE
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.Charset
import java.util.*

class BluetoothChatService(context: Context, handler: Handler) {
    //internal var MESSAGE_STATE_CHANGE = 1
    //internal var MESSAGE_READ = 2
    //internal var MESSAGE_WRITE = 3
    //internal var MESSAGE_DEVICE_NAME = 4
    //internal var MESSAGE_TOAST = 5

    // Key names received from the BluetoothChatService Handler
    private var mDeviceName = "device_name"
    private var mTOAST = "toast"
    // Debugging
    private val mTAG = "BluetoothChatService"

    // Name for the SDP record when creating server socket
    private val mNameSecure = "BluetoothChatSecure"
    private val mNameInsecure = "BluetoothChatInsecure"

    // Unique UUID for this application
    private val myUuidSecure =
        //UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
        UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private val myUuidInSecure = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66")

    // Member fields
    private var mAdapter: BluetoothAdapter? = null
    private var mHandler: Handler? = null
    private var mSecureAcceptThread: AcceptThread? = null
    private var mInsecureAcceptThread: AcceptThread? = null
    private var mConnectThread: ConnectThread? = null
    private var mConnectedThread: ConnectedThread? = null
    private var mState: Int = 0
    private var mNewState: Int = 0

    // Constants that indicate the current connection state
    companion object {
        const val STATE_NONE = 0       // we're doing nothing
        const val STATE_LISTEN = 1     // now listening for incoming connections
        const val STATE_CONNECTING = 2 // now initiating an outgoing connection
        const val STATE_CONNECTED = 3  // now connected to a remote device
    }
    private var mContext: Context? = null
    /**
     * Constructor. Prepares a new BluetoothChat session.
     *
     */
    init {
        mContext = context
        //mAdapter = BluetoothAdapter.getDefaultAdapter()
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        mAdapter = bluetoothManager.adapter
        mState = STATE_NONE
        mNewState = mState
        mHandler = handler
    }

    /*fun BluetoothChatService(context: Context, handler: Handler): {
        mContext = context
        mAdapter = BluetoothAdapter.getDefaultAdapter()
        mState = STATE_NONE
        mNewState = mState
        mHandler = handler
    }*/

    /**
     * Update UI title according to the current state of the chat connection
     */
    @Synchronized
    private fun updateUserInterfaceTitle() {
        mState = getState()
        Log.d(mTAG, "updateUserInterfaceTitle() $mNewState -> $mState")
        mNewState = mState

        // Give the new state to the Handler so the UI Activity can update
        mHandler!!.obtainMessage(MESSAGE_STATE_CHANGE, mNewState, -1).sendToTarget()
    }

    /**
     * Return the current connection state.
     */
    @Synchronized
    fun getState(): Int {
        return mState
    }

    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume()
     */
    @Synchronized
    fun start() {
        Log.d(mTAG, "start")

        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {
            mConnectThread!!.cancel()
            mConnectThread = null
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread!!.cancel()
            mConnectedThread = null
        }
        // Start the thread to listen on a BluetoothServerSocket
        if (mSecureAcceptThread == null) {
            mSecureAcceptThread = AcceptThread(true)
            mSecureAcceptThread!!.start()
        }
        if (mInsecureAcceptThread == null) {
            mInsecureAcceptThread = AcceptThread(false)
            mInsecureAcceptThread!!.start()
        }
        // Update UI title
        updateUserInterfaceTitle()
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     *
     * @param device The BluetoothDevice to connect
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    @Synchronized
    fun connect(device: BluetoothDevice, secure: Boolean) {
        Log.d(mTAG, "connect to: $device")

        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread!!.cancel()
                mConnectThread = null
            }
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread!!.cancel()
            mConnectedThread = null
        }

        // Start the thread to connect with the given device
        mConnectThread = ConnectThread(device, secure)
        mConnectThread!!.start()
        // Update UI title
        updateUserInterfaceTitle()
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     *
     * @param socket The BluetoothSocket on which the connection was made
     * @param device The BluetoothDevice that has been connected
     */
    @Synchronized
    fun connected(socket: BluetoothSocket, device: BluetoothDevice, socketType: String) {
        Log.d(mTAG, "connected, Socket Type:$socketType")

        // Cancel the thread that completed the connection
        if (mConnectThread != null) {
            mConnectThread!!.cancel()
            mConnectThread = null
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread!!.cancel()
            mConnectedThread = null
        }

        // Cancel the accept thread because we only want to connect to one device
        if (mSecureAcceptThread != null) {
            mSecureAcceptThread!!.cancel()
            mSecureAcceptThread = null
        }
        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread!!.cancel()
            mInsecureAcceptThread = null
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = ConnectedThread(socket, socketType)
        mConnectedThread!!.start()

        // Send the name of the connected device back to the UI Activity
        val msg = mHandler!!.obtainMessage(MESSAGE_DEVICE_NAME)
        val bundle = Bundle()
        bundle.putString(mDeviceName, device.name)
        msg.data = bundle
        mHandler!!.sendMessage(msg)
        // Update UI title
        updateUserInterfaceTitle()
    }

    /**
     * Stop all threads
     */
    @Synchronized
    fun stop() {
        Log.d(mTAG, "stop")

        if (mConnectThread != null) {
            mConnectThread!!.cancel()
            mConnectThread = null
        }

        if (mConnectedThread != null) {
            mConnectedThread!!.cancel()
            mConnectedThread = null
        }

        if (mSecureAcceptThread != null) {
            mSecureAcceptThread!!.cancel()
            mSecureAcceptThread = null
        }

        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread!!.cancel()
            mInsecureAcceptThread = null
        }
        mState = STATE_NONE
        // Update UI title
        updateUserInterfaceTitle()
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     *
     * @param out The bytes to write
     * @see ConnectedThread.write
     */
    fun write(out: ByteArray) {
        // Create temporary object
        val r: ConnectedThread?
        // Synchronize a copy of the ConnectedThread
        synchronized(this) {
            if (mState != STATE_CONNECTED) return
            r = mConnectedThread
        }
        // Perform the write unsynchronized
        r!!.write(out)
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private fun connectionFailed() {
        // Send a failure message back to the Activity
        val msg = mHandler!!.obtainMessage(MESSAGE_TOAST)
        val bundle = Bundle()
        bundle.putString(mTOAST, mContext!!.getString(R.string.bluetooth_unable_to_connect_printer))
        msg.data = bundle
        mHandler!!.sendMessage(msg)

        mState = STATE_NONE
        // Update UI title
        updateUserInterfaceTitle()

        // Start the service over to restart listening mode
        this@BluetoothChatService.start()
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private fun connectionLost() {
        // Send a failure message back to the Activity
        val msg = mHandler!!.obtainMessage(MESSAGE_TOAST)
        val bundle = Bundle()
        bundle.putString(mTOAST, "Device connection was lost")
        msg.data = bundle
        mHandler!!.sendMessage(msg)

        mState = STATE_NONE
        // Update UI title
        updateUserInterfaceTitle()

        // Start the service over to restart listening mode
        this@BluetoothChatService.start()
    }

    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    private inner class AcceptThread(secure: Boolean) : Thread() {
        // The local server socket
        private val mmServerSocket: BluetoothServerSocket?
        private val mSocketType: String

        init {
            var tmp: BluetoothServerSocket? = null
            mSocketType = if (secure) "Secure" else "Insecure"

            // Create a new listening server socket
            try {
                tmp = if (secure) {
                    mAdapter!!.listenUsingRfcommWithServiceRecord(
                        mNameSecure,
                        myUuidSecure
                    )
                } else {
                    mAdapter!!.listenUsingInsecureRfcommWithServiceRecord(
                        mNameInsecure, myUuidInSecure
                    )
                }
            } catch (e: IOException) {
                e.printStackTrace()
                Log.e(mTAG, "Socket Type: " + mSocketType + "listen() failed", e)
            }

            mmServerSocket = tmp
            mState = STATE_LISTEN
        }

        override fun run() {
            Log.d(
                mTAG, "Socket Type: " + mSocketType +
                        "BEGIN mAcceptThread" + this
            )
            name = "AcceptThread$mSocketType"

            var socket: BluetoothSocket

            // Listen to the server socket if we're not connected
            while (mState != STATE_CONNECTED) {

                if (mmServerSocket != null) {
                    try {
                        // This is a blocking call and will only return on a
                        // successful connection or an exception
                        socket = mmServerSocket.accept()
                    } catch (e: IOException) {
                        e.printStackTrace()
                        Log.e(mTAG, "Socket Type: " + mSocketType + "accept() failed", e)
                        break
                    }
                } else {
                    Log.e(mTAG, "mmServerSocket = null")
                    break
                }



                // If a connection was accepted
                if (socket != null) {
                    synchronized(this@BluetoothChatService) {
                        when (mState) {
                            STATE_LISTEN, STATE_CONNECTING ->
                                // Situation normal. Start the connected thread.
                                connected(
                                    socket, socket.remoteDevice,
                                    mSocketType
                                )
                            STATE_NONE, STATE_CONNECTED ->
                                // Either not ready or already connected. Terminate new socket.
                                try {
                                    socket.close()
                                } catch (e: IOException) {
                                    e.printStackTrace()
                                    Log.e(mTAG, "Could not close unwanted socket", e)
                                }
                            else -> {

                            }
                        }
                    }
                }
            }
            Log.i(mTAG, "END mAcceptThread, socket Type: $mSocketType")

        }

        fun cancel() {
            Log.d(mTAG, "Socket Type" + mSocketType + "cancel " + this)

            if (mmServerSocket != null) {
                try {
                    mmServerSocket.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                    Log.e(mTAG, "Socket Type" + mSocketType + "close() of server failed", e)
                }
            } else {
                Log.e(mTAG, "mmServerSocket = null")
            }



        }
    }


    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private inner class ConnectThread(private val mmDevice: BluetoothDevice, secure: Boolean) : Thread() {
        private val mmSocket: BluetoothSocket?
        private val mSocketType: String

        init {
            var tmp: BluetoothSocket? = null
            mSocketType = if (secure) "Secure" else "Insecure"

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                tmp = if (secure) {
                    mmDevice.createRfcommSocketToServiceRecord(
                        myUuidSecure
                    )
                } else {
                    mmDevice.createInsecureRfcommSocketToServiceRecord(
                        myUuidInSecure
                    )
                }
                mState = STATE_CONNECTING
            } catch (e: IOException) {
                e.printStackTrace()
                mState = STATE_NONE
                Log.e(mTAG, "Socket Type: " + mSocketType + "create() failed", e)
            }

            mmSocket = tmp

        }

        override fun run() {
            Log.i(mTAG, "BEGIN mConnectThread SocketType:$mSocketType")
            name = "ConnectThread$mSocketType"

            // Always cancel discovery because it will slow down a connection
            mAdapter!!.cancelDiscovery()

            if (mmSocket != null) {
                // Make a connection to the BluetoothSocket
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    mmSocket.connect()
                } catch (e: IOException) {
                    // Close the socket
                    e.printStackTrace()
                    try {
                        mmSocket.close()
                    } catch (e2: IOException) {
                        e2.printStackTrace()
                        Log.e(
                            mTAG, "unable to close() " + mSocketType +
                                    " socket during connection failure", e2
                        )
                    }

                    connectionFailed()
                    return
                }

                // Reset the ConnectThread because we're done
                synchronized(this@BluetoothChatService) {
                    mConnectThread = null
                }

                // Start the connected thread
                connected(mmSocket, mmDevice, mSocketType)
            } else {
                Log.e(mTAG, "mmSocket == null")
            }


        }

        fun cancel() {

            if (mmSocket != null) {
                try {
                    mmSocket.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                    Log.e(mTAG, "close() of connect $mSocketType socket failed", e)
                }
            } else {
                Log.e(mTAG, "mmSocket == null")
            }



        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private inner class ConnectedThread(private val mmSocket: BluetoothSocket, socketType: String) : Thread() {
        private val mmInStream: InputStream?
        private val mmOutStream: OutputStream?
        private val writer: OutputStreamWriter?

        init {
            Log.d(mTAG, "create ConnectedThread: $socketType")
            var tmpIn: InputStream? = null
            var tmpOut: OutputStream? = null
            val tmpwriter: OutputStreamWriter? = null

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = mmSocket.inputStream
                tmpOut = mmSocket.outputStream
            } catch (e: IOException) {
                e.printStackTrace()
                Log.e(mTAG, "temp sockets not created", e)
            }

            mmInStream = tmpIn
            mmOutStream = tmpOut
            writer = tmpwriter
            mState = STATE_CONNECTED
        }

        override fun run() {
            Log.i(mTAG, "BEGIN mConnectedThread")
            var bytes: Int
            val buffer = ByteArray(1024)
            val end = "\r"
            val curMsg = StringBuilder()
            while (mState == STATE_CONNECTED) {
                try {
                    //while (-1 != (bytes = mmInStream!!.read(buffer))) {
                    bytes = mmInStream!!.read(buffer)

                    while (-1 != bytes) {
                        curMsg.append(String(buffer, 0, bytes, Charset.forName("ISO-8859-1")))
                        val endIdx = curMsg.indexOf(end)
                        if (endIdx != -1) {
                            val fullMessage = curMsg.substring(0, endIdx + end.length)
                            curMsg.delete(0, endIdx + end.length)

                            // Now send fullMessage
                            // Send the obtained bytes to the UI Activity
                            mHandler!!.obtainMessage(MESSAGE_READ, bytes, -1, fullMessage)
                                .sendToTarget()
                        }

                        bytes = mmInStream.read(buffer)
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                    Log.e(mTAG, "disconnected", e)
                    connectionLost()
                    break
                }

            }
        }

        /**
         * Write to the connected OutStream.
         *
         * @param buffer The bytes to write
         */
        fun write(buffer: ByteArray) {
            try {
                mmOutStream!!.write(buffer)
                mmOutStream.flush()
                // Share the sent message back to the UI Activity
                mHandler!!.obtainMessage(MESSAGE_WRITE, -1, -1, buffer)
                    .sendToTarget()
            } catch (e: IOException) {
                e.printStackTrace()
                Log.e(mTAG, "Exception during write", e)
            }

        }

        fun cancel() {
            try {
                mmSocket.close()
            } catch (e: IOException) {
                e.printStackTrace()
                Log.e(mTAG, "close() of connect socket failed", e)
            }

        }
    }
}