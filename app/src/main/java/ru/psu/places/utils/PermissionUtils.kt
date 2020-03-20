/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.psu.places.utils

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.DialogFragment
import ru.psu.places.R

object PermissionUtils {
    fun requestPermission(
        activity: AppCompatActivity, requestId: Int,
        permission: String, finishActivity: Boolean
    ) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                permission
            )
        )
            RationaleDialog.newInstance(requestId, finishActivity)
                .show(activity.supportFragmentManager, "dialog")
        else
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(permission),
                requestId
            )
    }

    fun isPermissionGranted(
        grantPermissions: Array<String>, grantResults: IntArray,
        permission: String
    ): Boolean {
        for (i in grantPermissions.indices) {
            if (permission == grantPermissions[i]) {
                return grantResults[i] == PackageManager.PERMISSION_GRANTED
            }
        }
        return false
    }

    class PermissionDeniedDialog : DialogFragment() {
        private var mFinishActivity = false
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            mFinishActivity =
                arguments!!.getBoolean(ARGUMENT_FINISH_ACTIVITY)
            return AlertDialog.Builder(activity)
                .setMessage(R.string.location_permission_denied)
                .setPositiveButton(R.string.ok, null)
                .create()
        }

        override fun onDismiss(dialog: DialogInterface) {
            super.onDismiss(dialog)
            if (mFinishActivity) {
                Toast.makeText(
                    activity, R.string.permission_required_toast,
                    Toast.LENGTH_SHORT
                ).show()
                activity!!.finish()
            }
        }

        companion object {
            private const val ARGUMENT_FINISH_ACTIVITY = "finish"
            fun newInstance(finishActivity: Boolean): PermissionDeniedDialog {
                val arguments = Bundle()
                arguments.putBoolean(
                    ARGUMENT_FINISH_ACTIVITY,
                    finishActivity
                )
                val dialog = PermissionDeniedDialog()
                dialog.arguments = arguments
                return dialog
            }
        }
    }

    class RationaleDialog : DialogFragment() {
        private var mFinishActivity = false
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val arguments = arguments
            val requestCode =
                arguments!!.getInt(ARGUMENT_PERMISSION_REQUEST_CODE)
            mFinishActivity =
                arguments.getBoolean(ARGUMENT_FINISH_ACTIVITY)
            return AlertDialog.Builder(activity)
                .setMessage(R.string.permission_rationale_location)
                .setPositiveButton(R.string.ok,
                    { dialog, which ->
                        ActivityCompat.requestPermissions(
                            activity!!,
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                            requestCode
                        )
                        mFinishActivity = false
                    })
                .setNegativeButton(R.string.cancel, null)
                .create()
        }

        override fun onDismiss(dialog: DialogInterface) {
            super.onDismiss(dialog)
            if (mFinishActivity) {
                Toast.makeText(
                    activity,
                    R.string.permission_required_toast,
                    Toast.LENGTH_SHORT
                )
                    .show()
                activity!!.finish()
            }
        }

        companion object {
            private const val ARGUMENT_PERMISSION_REQUEST_CODE = "requestCode"
            private const val ARGUMENT_FINISH_ACTIVITY = "finish"
            fun newInstance(requestCode: Int, finishActivity: Boolean): RationaleDialog {
                val arguments = Bundle()
                arguments.putInt(
                    ARGUMENT_PERMISSION_REQUEST_CODE,
                    requestCode
                )
                arguments.putBoolean(
                    ARGUMENT_FINISH_ACTIVITY,
                    finishActivity
                )
                val dialog = RationaleDialog()
                dialog.arguments = arguments
                return dialog
            }
        }
    }
}