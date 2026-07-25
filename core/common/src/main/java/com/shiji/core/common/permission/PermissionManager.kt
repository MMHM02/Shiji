package com.shiji.core.common.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

class PermissionManager(private val context: Context) {

    enum class AppPermission(val manifestPermission: String, val rationale: String) {
        CAMERA(Manifest.permission.CAMERA, "需要相机权限来拍摄食物照片"),
        RECORD_AUDIO(Manifest.permission.RECORD_AUDIO, "需要录音权限来进行语音记录")
    }

    fun isGranted(permission: AppPermission): Boolean {
        return ContextCompat.checkSelfPermission(context, permission.manifestPermission) ==
                PackageManager.PERMISSION_GRANTED
    }

    fun isAnyGranted(permissions: List<AppPermission>): Boolean = permissions.any { isGranted(it) }

    fun areAllGranted(permissions: List<AppPermission>): Boolean = permissions.all { isGranted(it) }

    fun getNotGranted(permissions: List<AppPermission>): List<AppPermission> = permissions.filter { !isGranted(it) }

    fun getCorePermissions(): List<AppPermission> = listOf(AppPermission.CAMERA, AppPermission.RECORD_AUDIO)
}
