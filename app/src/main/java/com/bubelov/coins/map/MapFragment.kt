/*
 * This is free and unencumbered software released into the public domain.
 *
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means.
 *
 * In jurisdictions that recognize copyright laws, the author or authors
 * of this software dedicate any and all copyright interest in the
 * software to the public domain. We make this dedication for the benefit
 * of the public at large and to the detriment of our heirs and
 * successors. We intend this dedication to be an overt act of
 * relinquishment in perpetuity of all present and future rights to this
 * software under copyright law.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 * For more information, please refer to <https://unlicense.org>
 */

package com.bubelov.coins.map

import android.Manifest
import android.annotation.SuppressLint
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import com.google.android.material.bottomsheet.BottomSheetBehavior
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import android.text.TextUtils
import android.view.*
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import com.bubelov.coins.BuildConfig
import com.bubelov.coins.R
import com.bubelov.coins.auth.AuthResultViewModel
import com.bubelov.coins.model.Location
import com.bubelov.coins.model.Place
import com.bubelov.coins.search.PlacesSearchResultViewModel
import com.bubelov.coins.util.*
import com.bubelov.coins.util.extention.getLocation
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.view.DefaultClusterRenderer
import com.squareup.picasso.Picasso
import dagger.android.support.DaggerFragment
import kotlinx.android.synthetic.main.fragment_map.*
import kotlinx.android.synthetic.main.navigation_drawer_header.view.*
import javax.inject.Inject

class MapFragment :
    DaggerFragment(),
    OnMapReadyCallback,
    Toolbar.OnMenuItemClickListener,
    MapViewModel.Callback {
    @Inject internal lateinit var modelFactory: ViewModelProvider.Factory

    private val model by lazy {
        activityViewModelProvider(modelFactory) as MapViewModel
    }

    private val placesSearchResultModel by lazy {
        activityViewModelProvider(modelFactory) as PlacesSearchResultViewModel
    }

    private val authResultModel by lazy {
        activityViewModelProvider(modelFactory) as AuthResultViewModel
    }

    private lateinit var drawerHeader: View

    private lateinit var drawerToggle: ActionBarDrawerToggle

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<*>

    private var map: GoogleMap? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        model.callback = this

        drawerHeader = navigationView.getHeaderView(0)

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        bottomSheetBehavior = BottomSheetBehavior.from(placeDetails).apply {
            state = BottomSheetBehavior.STATE_HIDDEN

            setBottomSheetCallback(object :
                BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    placeDetails.fullScreen = newState == BottomSheetBehavior.STATE_EXPANDED
                    editPlaceFab.isVisible = placeDetails.fullScreen
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    locationFab.isVisible = slideOffset < 0.5f
                }
            })

            peekHeight = resources.getDimensionPixelSize(R.dimen.map_header_height)
        }

        editPlaceFab.setOnClickListener {
            model.onEditPlaceClick()
        }

        placeDetails.onDismissed = {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }

        toolbar.apply {
            setNavigationOnClickListener { drawerLayout.openDrawer(navigationView) }
            inflateMenu(R.menu.map)
            setOnMenuItemClickListener(this@MapFragment)
        }

        navigationView.setNavigationItemSelectedListener { item ->
            drawerLayout.closeDrawer(navigationView, false)

            when (item.itemId) {
                R.id.action_exchange_rates -> {
                    findNavController().navigate(R.id.action_mapFragment_to_exchangeRatesFragment)
                }

                R.id.action_notification_area -> {
                    findNavController().navigate(R.id.action_mapFragment_to_notificationAreaFragment)
                }

                R.id.action_chat -> openSupportChat()

                R.id.action_support_project -> {
                    findNavController().navigate(R.id.action_mapFragment_to_supportProjectFragment)
                }

                R.id.action_settings -> {
                    findNavController().navigate(R.id.action_mapFragment_to_settingsFragment)
                }
            }

            true
        }

        drawerToggle = ActionBarDrawerToggle(
            requireActivity(),
            drawerLayout,
            toolbar,
            R.string.open,
            R.string.close
        )

        drawerLayout.addDrawerListener(drawerToggle)

        updateDrawerHeader()

        placeDetails.setOnClickListener {
            if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            } else {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED)
            }
        }

        model.selectedPlace.observe(viewLifecycleOwner, Observer { place ->
            if (place != null) {
                placeDetails.place = place
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            } else {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            }
        })

        model.userLocation.observe(viewLifecycleOwner, Observer {
            locationFab.setOnClickListener {
                model.onLocationButtonClick()
            }
        })

        placesSearchResultModel.pickedPlaceId.observe(viewLifecycleOwner, Observer { id ->
            model.navigateToNextSelectedPlace = true
            model.selectPlace(id ?: "")
        })

        authResultModel.authorized.observe(viewLifecycleOwner, Observer {
            updateDrawerHeader()
            model.onAuthSuccess()
        })

        model.openSignInScreen.observe(viewLifecycleOwner, Observer {
            findNavController().navigate(R.id.action_mapFragment_to_authFragment)
        })

        model.openAddPlaceScreen.observe(viewLifecycleOwner, Observer {
            val action = MapFragmentDirections.actionMapFragmentToEditPlaceFragment(
                null,
                map.getLocation()
            )

            findNavController().navigate(action)
        })

        model.openEditPlaceScreen.observe(viewLifecycleOwner, Observer {
            val selectedPlace = model.selectedPlace.value ?: return@Observer

            val action = MapFragmentDirections.actionMapFragmentToEditPlaceFragment(
                selectedPlace,
                map.getLocation()
            )

            findNavController().navigate(action)
        })

        model.requestLocationPermissions.observe(viewLifecycleOwner, Observer {
            requestLocationPermissions()
        })
    }

    override fun onDestroyView() {
        map?.clear()
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()
        drawerToggle.syncState()

        val placeArg = arguments?.getParcelable(PLACE_ARG) as Place?

        if (placeArg != null) {
            model.selectPlace(placeArg.id)
            model.moveToLocation(Location(placeArg.latitude, placeArg.longitude))
        }
    }

    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_ACCESS_LOCATION -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                model.onLocationPermissionGranted()
            }
        }
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_add -> model.onAddPlaceClick()

            R.id.action_search -> {
                val action = MapFragmentDirections.actionMapFragmentToPlacesSearchFragment(
                    model.userLocation.value
                )

                findNavController().navigate(action)
            }

            else -> return super.onOptionsItemSelected(item)
        }

        return true
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        drawerToggle.onConfigurationChanged(newConfig)
        super.onConfigurationChanged(newConfig)
    }

    override fun showUserProfile() {
        findNavController().navigate(R.id.action_mapFragment_to_profileFragment)
    }

    private fun requestLocationPermissions() {
        requestPermissions(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            REQUEST_ACCESS_LOCATION
        )
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(map: GoogleMap) {
        if (view == null) {
            return
        }

        this.map = map

        map.uiSettings.apply {
            isMyLocationButtonEnabled = false
            isZoomControlsEnabled = false
            isCompassEnabled = false
            isMapToolbarEnabled = false
        }

        initClustering(map)

        model.onMapReady()

        model.selectedPlace.observe(viewLifecycleOwner, Observer { place ->
            if (place != null && model.navigateToNextSelectedPlace) {
                map.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        place.toLatLng(),
                        DEFAULT_MAP_ZOOM
                    )
                )

                model.navigateToNextSelectedPlace = false
            }
        })

        model.moveMapToLocation.observe(viewLifecycleOwner, Observer {
            it?.let { location ->
                map.isMyLocationEnabled = true

                map.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        location.toLatLng(),
                        DEFAULT_MAP_ZOOM
                    )
                )
            }
        })
    }

    private fun updateDrawerHeader() {
        val user = model.userRepository.user

        if (user != null) {
            if (!TextUtils.isEmpty(user.avatarUrl)) {
                Picasso.with(requireContext())
                    .load(user.avatarUrl)
                    .transform(CircleTransformation())
                    .into(drawerHeader.avatar)
            } else {
                drawerHeader.avatar.setImageResource(R.drawable.ic_no_avatar)
            }

            if (!TextUtils.isEmpty(user.firstName)) {
                drawerHeader.userName.text = String.format("%s %s", user.firstName, user.lastName)
            } else {
                drawerHeader.userName.text = user.email
            }
        } else {
            drawerHeader.avatar.setImageResource(R.drawable.ic_no_avatar)
            drawerHeader.userName.setText(R.string.guest)
        }

        drawerHeader.setOnClickListener {
            drawerLayout.closeDrawer(navigationView)
            model.onDrawerHeaderClick()
        }
    }

    private fun openSupportChat() {
        requireContext().openUrl("https://t.me/joinchat/AAAAAAwVT4aVBdFzcKKbsw")
    }

    private fun initClustering(map: GoogleMap) {
        val placesManager = ClusterManager<PlaceMarker>(requireContext(), map)
        placesManager.setAnimation(false)
        map.setOnMarkerClickListener(placesManager)

        val renderer = PlacesRenderer(requireContext(), map, placesManager)
        renderer.setAnimation(false)
        placesManager.renderer = renderer

        renderer.setOnClusterItemClickListener(ClusterItemClickListener())

        map.setOnCameraIdleListener {
            placesManager.onCameraIdle()
            model.mapBounds.value = map.projection.visibleRegion.latLngBounds
        }

        map.setOnMapClickListener {
            model.selectPlace("")
        }

        model.placeMarkers.observe(viewLifecycleOwner, Observer { markers ->
            placesManager.clearItems()
            placesManager.addItems(markers)
            placesManager.cluster()
        })
    }

    private inner class PlacesRenderer internal constructor(
        context: Context,
        map: GoogleMap,
        clusterManager: ClusterManager<PlaceMarker>
    ) : DefaultClusterRenderer<PlaceMarker>(context, map, clusterManager) {
        override fun onBeforeClusterItemRendered(
            placeMarker: PlaceMarker,
            markerOptions: MarkerOptions
        ) {
            super.onBeforeClusterItemRendered(placeMarker, markerOptions)

            markerOptions
                .icon(BitmapDescriptorFactory.fromBitmap(placeMarker.icon))
                .anchor(BuildConfig.MAP_MARKER_ANCHOR_U, BuildConfig.MAP_MARKER_ANCHOR_V)
        }
    }

    private inner class ClusterItemClickListener :
        ClusterManager.OnClusterItemClickListener<PlaceMarker> {
        override fun onClusterItemClick(placeMarker: PlaceMarker): Boolean {
            model.selectPlace(placeMarker.placeId)
            return true
        }
    }

    companion object {
        private const val REQUEST_ACCESS_LOCATION = 10

        private const val DEFAULT_MAP_ZOOM = 15f

        private const val PLACE_ARG = "place"

        fun newOpenPlaceArguments(place: Place): Bundle {
            return bundleOf(Pair(PLACE_ARG, place))
        }
    }
}