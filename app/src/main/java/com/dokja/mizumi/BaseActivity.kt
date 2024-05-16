package com.dokja.mizumi

import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.dokja.mizumi.di.AppModule
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
open class BaseActivity :
    AppCompatActivity()
//    ThemingDelegate by ThemingDelegateImpl()
{
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        applyAppTheme(this)
//        super.onCreate(savedInstanceState)
//    }

    val appPreferences: AppPreferences by lazy { AppModule.provideAppPreferences(applicationContext) }


    private var activitiesCallbacksCounter: Int = 0
    private val activitiesCallbacks = mutableMapOf<Int, (resultCode: Int, data: Intent?) -> Unit>()

    fun activityRequest(intent: Intent, reply: (resultCode: Int, data: Intent?) -> Unit) {
        val requestCode = activitiesCallbacksCounter++
        activitiesCallbacks[requestCode] = reply
        startActivityForResult(intent, requestCode)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        activitiesCallbacks.remove(requestCode)?.let { it(resultCode, data) }
    }

    private var permissionsCallbacksCounter: Int = 0
    private val permissionsCallbacks = mutableMapOf<Int, Pair<() -> Unit, (List<String>) -> Unit>>()

    fun permissionRequest(
        vararg permissions: String,
        denied: (deniedPermissions: List<String>) -> Unit = {  },
        granted: () -> Unit
    ) {
        val hasPermissions = permissions.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }

        if (hasPermissions) granted()
        else {
            val requestCode = permissionsCallbacksCounter++
            permissionsCallbacks[requestCode] = Pair(granted, denied)
            requestPermissions(permissions, requestCode)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionsCallbacks.remove(requestCode)?.let {
            when {
                grantResults.isEmpty() -> it.second(listOf())
                grantResults.all { result -> result == PackageManager.PERMISSION_GRANTED } -> it.first()
                else -> it.second(permissions.filterIndexed { index, _ -> grantResults[index] == PackageManager.PERMISSION_DENIED })
            }
        }
    }
}