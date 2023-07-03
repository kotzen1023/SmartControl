package com.seventhmoon.smartcontrol.data

import com.seventhmoon.smartcontrol.BuildConfig

class Constants {
    class BluetoothState {
        companion object {
            const val MESSAGE_STATE_CHANGE = 1
            const val MESSAGE_READ = 2
            const val MESSAGE_WRITE = 3
            const val MESSAGE_DEVICE_NAME = 4
            const val MESSAGE_TOAST = 5
        }
    }

    class BluetoothService {
        companion object {
            const val INTENT_ACTION_DISCONNECT: String = BuildConfig.APPLICATION_ID + ".Disconnect"
            const val NOTIFICATION_CHANNEL: String = BuildConfig.APPLICATION_ID + ".Channel"
            const val INTENT_CLASS_MAIN_ACTIVITY: String = BuildConfig.APPLICATION_ID + ".MainActivity"

            // values have to be unique within each app
            const val NOTIFY_MANAGER_START_FOREGROUND_SERVICE = 1001
        }
    }
}