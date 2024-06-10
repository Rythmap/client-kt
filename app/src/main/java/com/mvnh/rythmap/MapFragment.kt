package com.mvnh.rythmap

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import com.google.android.material.imageview.ShapeableImageView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mvnh.rythmap.utils.SecretData.TAG
import com.mvnh.rythmap.databinding.FragmentMapBinding
import com.mvnh.rythmap.retrofit.ServiceGenerator
import com.mvnh.rythmap.retrofit.account.AccountApi
import com.mvnh.rythmap.retrofit.account.entities.AccountInfoPrivate
import com.mvnh.rythmap.retrofit.map.MapWSResponse
import com.mvnh.rythmap.retrofit.yandex.YandexApi
import com.mvnh.rythmap.utils.SecretData
import com.mvnh.rythmap.utils.TokenManager
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.plugins.annotation.SymbolManager
import org.maplibre.android.plugins.annotation.SymbolOptions
import retrofit2.Call
import retrofit2.Callback
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class MapFragment : Fragment() {

    private lateinit var binding: FragmentMapBinding
    private lateinit var tokenManager: TokenManager
    private lateinit var mapView: MapView
    private lateinit var accountApi: AccountApi
    private lateinit var yandexApi: YandexApi

    private lateinit var locationProvider: FusedLocationProviderClient
    private var requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                getUserLocation().addOnCompleteListener { task ->
                    if (task.isSuccessful && task.result != null) {
                        val location = task.result
                        mapView.getMapAsync { map ->
                            map.cameraPosition = CameraPosition.Builder()
                                .target(LatLng(location.latitude, location.longitude)).zoom(15.0)
                                .build()
                        }
                    }
                }
            } else {
                Log.e(TAG, "Location permission denied")
            }
        }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        MapLibre.getInstance(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMapBinding.inflate(inflater, container, false)

        tokenManager = TokenManager(requireContext())
        locationProvider = LocationServices.getFusedLocationProviderClient(requireContext())

        val jawgAccessToken = SecretData.JAWG_ACCESS_TOKEN
        val styleId =
            if (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) {
                "jawg-matrix"
            } else {
                "jawg-terrain"
            }
        val styleUrl = "https://api.jawg.io/styles/$styleId.json?access-token=$jawgAccessToken"

        yandexApi = ServiceGenerator.createService(YandexApi::class.java)
        accountApi = ServiceGenerator.createService(AccountApi::class.java)

        mapView = binding.mapView
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync { map -> map.setStyle(styleUrl) }

        requestPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)

        val client = OkHttpClient.Builder().readTimeout(0, TimeUnit.SECONDS).build()
        val request = Request.Builder().url("wss://${SecretData.SERVER_URL}/map").build()
        val scheduledExecutor = Executors.newSingleThreadScheduledExecutor()
        val webSocketListener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                super.onOpen(webSocket, response)

                if (ContextCompat.checkSelfPermission(
                        requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    scheduledExecutor.scheduleWithFixedDelay({
                        getUserLocation().addOnCompleteListener { task ->
                            if (task.isSuccessful && task.result != null) {
                                val location = task.result

                                val nicknameSharedPref =
                                    requireContext().getSharedPreferences("nickname", Context.MODE_PRIVATE)
                                val nickname = nicknameSharedPref.getString("nickname", null)
                                if (nickname != null) {
                                    val message =
                                        "{\"nickname\": \"${nickname}\", \"location\": {\"lat\": ${location.latitude}, \"lng\": ${location.longitude}}, \"status\": \"online\"}"
                                    webSocket.send(message)
                                    Log.d(TAG, "Sent location: $message")
                                } else {
                                    Log.e(TAG, "Failed to send location: nickname is null")
                                }
                            }
                        }
                    }, 0, 15, TimeUnit.SECONDS)
                } else {
                    requestPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                super.onMessage(webSocket, text)

                activity?.runOnUiThread {
                    mapView.getMapAsync { map ->
                        map.setStyle(styleUrl) { style ->
                            val gson = Gson()
                            val type = object : TypeToken<List<MapWSResponse>>() {}.type
                            val jsons = gson.fromJson<List<MapWSResponse>>(text, type)
                            Log.d(TAG, "Received JSONs: $jsons")

                            val symbolManager = SymbolManager(mapView, map, style)
                            symbolManager.iconAllowOverlap = true
                            symbolManager.iconIgnorePlacement = true

                            for (json in jsons) {
                                val nickname = json.nickname
                                val latitude = json.location.lat
                                val longitude = json.location.lng

                                val getAvatarCall = accountApi.getMedia(nickname, "avatar")
                                getAvatarCall.enqueue(object : Callback<ResponseBody> {
                                    override fun onResponse(
                                        call: Call<ResponseBody>,
                                        response: retrofit2.Response<ResponseBody>
                                    ) {
                                        if (response.isSuccessful) {
                                            Log.d(TAG, "Retrieved avatar for nickname: $nickname")
                                            val avatar = response.body()?.bytes()
                                            var bitmap: Bitmap? = null
                                            bitmap = createMarkerBitmap(avatar!!, nickname)

                                            style.addImage(nickname, bitmap)

                                            val symbolOptions = SymbolOptions().withLatLng(
                                                LatLng(
                                                    latitude,
                                                    longitude
                                                )
                                            ).withIconImage(nickname).withIconSize(0.75f)
                                            val symbol = symbolManager.create(symbolOptions)

                                            symbolManager.update(symbol)
                                        } else {
                                            Log.e(
                                                TAG,
                                                "Failed to retrieve avatar: ${response.message()}"
                                            )

                                            val bitmap = createMarkerBitmap(byteArrayOf(), nickname)
                                            style.addImage(nickname, bitmap)

                                            val symbolOptions = SymbolOptions().withLatLng(
                                                LatLng(
                                                    latitude,
                                                    longitude
                                                )
                                            ).withIconImage(nickname).withIconSize(0.75f)
                                            val symbol = symbolManager.create(symbolOptions)
                                            symbolManager.update(symbol)
                                        }
                                    }

                                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                                        Log.e(TAG, "Failed to retrieve avatar: ${t.message}")
                                    }
                                })
                            }
                        }
                    }
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                super.onFailure(webSocket, t, response)
                Log.e(TAG, "WebSocket failed: ${t.message}")
                scheduledExecutor.shutdown()
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                super.onClosed(webSocket, code, reason)
                Log.d(TAG, "WebSocket closed: $code, $reason")
                scheduledExecutor.shutdown()
            }
        }
        val webSocket = client.newWebSocket(request, webSocketListener)

        return binding.root
    }

    private fun createMarkerBitmap(avatar: ByteArray, nickname: String): Bitmap {
        val markerLayout =
            LayoutInflater.from(requireContext()).inflate(R.layout.default_marker, null)

        val imageView = markerLayout.findViewById<ShapeableImageView>(R.id.profilePfp)
        val textView = markerLayout.findViewById<TextView>(R.id.nicknameTextView)

        val avatarBitmap = BitmapFactory.decodeByteArray(avatar, 0, avatar.size)
        imageView.setImageBitmap(avatarBitmap)
        textView.text = nickname

        markerLayout.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        val bitmap = Bitmap.createBitmap(
            markerLayout.measuredWidth, markerLayout.measuredHeight, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        markerLayout.layout(0, 0, markerLayout.measuredWidth, markerLayout.measuredHeight)
        markerLayout.draw(canvas)

        return bitmap
    }

    private fun getUserLocation(): Task<Location> {
        if (ContextCompat.checkSelfPermission(
                requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return locationProvider.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token
            )
        } else {
            requestPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
            return locationProvider.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token
            )
        }
    }

//    private fun setOffline(webSocket: WebSocket) {
//        getUserLocation().addOnCompleteListener { task ->
//            if (task.isSuccessful && task.result != null) {
//                val location = task.result
//                if (nickname != null) {
//                    val message =
//                        "{\"nickname\": \"${nickname}\", \"location\": {\"lat\": ${location.latitude}, \"lng\": ${location.longitude}}, \"status\": \"online\", \"command\": \"updateStatus\"}"
//                    webSocket.send(message)
//                    Log.d(TAG, "Sent location: $message")
//                } else {
//                    Log.e(TAG, "Failed to send location: nickname is null")
//                }
//            }
//        }
//        scheduledExecutor.shutdown()
//        Log.d(TAG, "WebSocket closing")
//    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        // setOffline(webSocket)
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }
}