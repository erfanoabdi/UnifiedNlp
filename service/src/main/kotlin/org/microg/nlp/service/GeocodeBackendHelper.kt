/*
 * SPDX-FileCopyrightText: 2014, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.nlp.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.location.Address
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import org.microg.nlp.api.GeocoderBackend

class GeocodeBackendHelper(context: Context, serviceIntent: Intent, signatureDigest: String?) : AbstractBackendHelper(TAG, context, serviceIntent, signatureDigest) {
    private var backend: GeocoderBackend? = null

    fun getFromLocation(latitude: Double, longitude: Double, maxResults: Int,
                        locale: String): List<Address> {
        if (backend == null) {
            Log.d(TAG, "Not (yet) bound.")
            return emptyList()
        }
        try {
            return backend!!.getFromLocation(latitude, longitude, maxResults, locale) ?: emptyList()
        } catch (e: Exception) {
            Log.w(TAG, e)
            unbind()
            return emptyList()
        }

    }

    fun getFromLocationName(locationName: String, maxResults: Int,
                            lowerLeftLatitude: Double, lowerLeftLongitude: Double,
                            upperRightLatitude: Double, upperRightLongitude: Double,
                            locale: String): List<Address> {
        if (backend == null) {
            Log.d(TAG, "Not (yet) bound.")
            return emptyList()
        }
        try {
            return backend!!.getFromLocationName(locationName, maxResults, lowerLeftLatitude,
                    lowerLeftLongitude, upperRightLatitude, upperRightLongitude, locale) ?: emptyList()
        } catch (e: Exception) {
            Log.w(TAG, e)
            unbind()
            return emptyList()
        }

    }

    override fun onServiceConnected(name: ComponentName, service: IBinder) {
        super.onServiceConnected(name, service)
        backend = GeocoderBackend.Stub.asInterface(service)
        if (backend != null) {
            try {
                backend!!.open()
            } catch (e: Exception) {
                Log.w(TAG, e)
                unbind()
            }

        }
    }

    override fun onServiceDisconnected(name: ComponentName) {
        super.onServiceDisconnected(name)
        backend = null
    }

    @Throws(RemoteException::class)
    public override fun close() {
        backend!!.close()
    }

    public override fun hasBackend(): Boolean {
        return backend != null
    }

    companion object {
        private val TAG = "UnifiedGeocoder"
    }
}