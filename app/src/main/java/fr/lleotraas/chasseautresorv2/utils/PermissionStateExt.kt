package fr.lleotraas.chasseautresorv2.utils

import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState

@ExperimentalPermissionsApi
fun PermissionState.isPermanentDenied(): Boolean {
    return !shouldShowRationale && !hasPermission
}