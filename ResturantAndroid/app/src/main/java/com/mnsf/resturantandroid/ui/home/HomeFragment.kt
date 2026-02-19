package com.mnsf.resturantandroid.ui.home

import android.content.res.ColorStateList
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.mnsf.resturantandroid.R
import com.mnsf.resturantandroid.data.model.Offer
import com.mnsf.resturantandroid.databinding.FragmentHomeBinding
import com.mnsf.resturantandroid.network.RetrofitClient
import com.mnsf.resturantandroid.repository.RestaurantRepository
import com.mnsf.resturantandroid.repository.OrderRepository
import com.mnsf.resturantandroid.ui.restaurant.RestaurantDetailsActivity
import com.mnsf.resturantandroid.ui.order.OrderTrackingActivity
import com.mnsf.resturantandroid.viewmodel.RestaurantViewModel
import com.mnsf.resturantandroid.viewmodel.OrderViewModel
import com.mnsf.resturantandroid.util.SessionManager
import com.mnsf.resturantandroid.util.CurrencyFormatter
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    /** Set to true to show only "Hello World" and skip fragment_home inflation (find Binary XML crash). Set false and rebuild to run real home. */
    private val TEST_HELLO_WORLD = false
    /** Set to false to disable real-time order tracking (card + polling). Leave commented/disabled until stable. */
    private val ENABLE_ORDER_TRACKING = false

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var restaurantViewModel: RestaurantViewModel
    private lateinit var restaurantAdapter: RestaurantAdapter
    private lateinit var freeDeliveryAdapter: RestaurantHorizontalAdapter
    private lateinit var offersAdapter: OfferHorizontalAdapter
    private lateinit var popularAdapter: RestaurantHorizontalAdapter
    private lateinit var orderViewModel: OrderViewModel
    private lateinit var sessionManager: SessionManager
    private var pollingJob: Job? = null
    private var isPolling = false
    private var currentTrackingOrderNumber: String? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        android.util.Log.e("LoginFlow", "HomeFragment onCreate ENTERED")
        Log.d("HomeFragment", "onCreate: Fragment created")
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("HomeFragment", "onViewCreated: View created successfully")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // TEST: show only Hello World. Set TEST_HELLO_WORLD = false above and rebuild to run real home and find crash.
        if (TEST_HELLO_WORLD) {
            android.util.Log.e("LoginFlow", "HomeFragment onCreateView: TEST_HELLO_WORLD=true, creating Hello World")
            val tv = TextView(requireContext()).apply {
                text = "Hello World"
                textSize = 28f
                setPadding(48, 48, 48, 48)
                setGravity(Gravity.CENTER)
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                ).apply { gravity = Gravity.CENTER }
            }
            android.util.Log.e("LoginFlow", "HomeFragment onCreateView: returning Hello World view")
            return tv
        }

        try {
            Log.d("HomeFragment", "onCreateView: Starting")
            val restaurantRepository = RestaurantRepository(RetrofitClient.apiService)
            Log.d("HomeFragment", "onCreateView: RestaurantRepository created")
            
            restaurantViewModel = ViewModelProvider(this, RestaurantViewModelFactory(restaurantRepository))[RestaurantViewModel::class.java]
            Log.d("HomeFragment", "onCreateView: RestaurantViewModel created")

            var root: View
            try {
                Log.d("HomeFragment", "onCreateView: Inflating fragment_home.xml...")
                _binding = FragmentHomeBinding.inflate(inflater, container, false)
                root = binding.root
                Log.d("HomeFragment", "onCreateView: fragment_home inflated OK")
            } catch (e: Exception) {
                Log.e("HomeFragment", "onCreateView: BINARY XML / INFLATE ERROR in fragment_home", e)
                Log.e("HomeFragment", "  message=${e.message} cause=${e.cause?.message}")
                throw e
            }

            // Setup session manager and order view model
            sessionManager = SessionManager(requireContext())
            if (sessionManager.isLoggedIn()) {
                val orderRepository = OrderRepository(RetrofitClient.apiService, sessionManager)
                orderViewModel = ViewModelProvider(
                    this,
                    OrderViewModelFactory(orderRepository, sessionManager)
                )[OrderViewModel::class.java]
                // Order tracking disabled (ENABLE_ORDER_TRACKING = false)
                // setupOrderTracking()
                // orderViewModel.loadOrders()
                if (ENABLE_ORDER_TRACKING) {
                    setupOrderTracking()
                    orderViewModel.loadOrders()
                }
            }

            setupRecyclerView()
            Log.d("HomeFragment", "onCreateView: RecyclerView setup complete")
            
            setupOrderTypeToggle()
            Log.d("HomeFragment", "onCreateView: Order type toggle setup complete")
            
            setupObservers()
            Log.d("HomeFragment", "onCreateView: Observers setup complete")
            
            setupSearch()
            Log.d("HomeFragment", "onCreateView: Search setup complete")

            val orderType = sessionManager.getOrderType()
            restaurantViewModel.setOrderType(if (orderType == "pickup") "pickup" else "delivery")
            updateOrderTypeToggleUi(orderType == "pickup")
            restaurantViewModel.loadRestaurants()
            Log.d("HomeFragment", "onCreateView: loadRestaurants called")

            Log.d("HomeFragment", "onCreateView: Completed successfully")
            return root
        } catch (e: Exception) {
            Log.e("HomeFragment", "onCreateView: Fatal error", e)
            e.printStackTrace()
            throw e
        }
    }

    private fun openRestaurant(restaurant: com.mnsf.resturantandroid.data.model.Restaurant) {
        try {
            val intent = Intent(requireContext(), RestaurantDetailsActivity::class.java)
            intent.putExtra("restaurant_id", restaurant.id)
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error starting RestaurantDetailsActivity", e)
        }
    }

    private fun openRestaurantFromOffer(offer: Offer) {
        try {
            val intent = Intent(requireContext(), RestaurantDetailsActivity::class.java)
            intent.putExtra("restaurant_id", offer.website_id)
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error starting RestaurantDetailsActivity from offer", e)
        }
    }

    private fun setupRecyclerView() {
        try {
            Log.d("HomeFragment", "setupRecyclerView: Starting")
            restaurantAdapter = RestaurantAdapter(::openRestaurant)
            binding.recyclerViewRestaurants.layoutManager = LinearLayoutManager(requireContext())
            binding.recyclerViewRestaurants.adapter = restaurantAdapter

            val horizontalManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            freeDeliveryAdapter = RestaurantHorizontalAdapter(::openRestaurant)
            binding.recyclerViewFreeDelivery.layoutManager = horizontalManager
            binding.recyclerViewFreeDelivery.adapter = freeDeliveryAdapter

            offersAdapter = OfferHorizontalAdapter(::openRestaurantFromOffer)
            binding.recyclerViewOffers.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            binding.recyclerViewOffers.adapter = offersAdapter

            popularAdapter = RestaurantHorizontalAdapter(::openRestaurant)
            binding.recyclerViewPopular.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            binding.recyclerViewPopular.adapter = popularAdapter

            binding.tvViewAllOffers.setOnClickListener {
                binding.searchView.setText("")
                restaurantViewModel.searchRestaurants("")
                binding.recyclerViewRestaurants.smoothScrollToPosition(0)
            }
            binding.tvViewAllFreeDelivery.setOnClickListener {
                binding.searchView.setText("")
                restaurantViewModel.searchRestaurants("")
                binding.recyclerViewRestaurants.smoothScrollToPosition(0)
            }
            binding.tvViewAllPopular.setOnClickListener {
                binding.searchView.setText("")
                restaurantViewModel.searchRestaurants("")
                binding.recyclerViewRestaurants.smoothScrollToPosition(0)
            }
            Log.d("HomeFragment", "setupRecyclerView: Completed")
        } catch (e: Exception) {
            Log.e("HomeFragment", "setupRecyclerView: Error", e)
            throw e
        }
    }

    private fun setupObservers() {
        try {
            Log.d("HomeFragment", "setupObservers: Starting")
            restaurantViewModel.filteredRestaurants.observe(viewLifecycleOwner) { restaurants ->
                try {
                    Log.d("HomeFragment", "setupObservers: filteredRestaurants received: ${restaurants.size} restaurants")
                    restaurantAdapter.submitList(restaurants)

                    val freeDelivery = restaurants.filter { (it.delivery_fee ?: 0.0) == 0.0 }
                    val popular = restaurants.take(6)
                    freeDeliveryAdapter.submitList(freeDelivery)
                    popularAdapter.submitList(popular)

                    binding.sectionFreeDelivery.visibility = if (freeDelivery.isEmpty()) View.GONE else View.VISIBLE
                    binding.sectionPopular.visibility = if (popular.isEmpty()) View.GONE else View.VISIBLE

                    if (restaurants.isEmpty()) {
                        binding.textEmpty.visibility = View.VISIBLE
                        binding.recyclerViewRestaurants.visibility = View.GONE
                    } else {
                        binding.textEmpty.visibility = View.GONE
                        binding.recyclerViewRestaurants.visibility = View.VISIBLE
                    }

                } catch (e: Exception) {
                    Log.e("HomeFragment", "setupObservers: Error in filteredRestaurants observer", e)
                }
            }

            restaurantViewModel.offers.observe(viewLifecycleOwner) { offerList ->
                try {
                    offersAdapter.submitList(offerList)
                    binding.sectionOffers.visibility = if (offerList.isEmpty()) View.GONE else View.VISIBLE
                } catch (e: Exception) {
                    Log.e("HomeFragment", "setupObservers: Error in offers observer", e)
                }
            }

            restaurantViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
                try {
                    Log.d("HomeFragment", "setupObservers: isLoading: $isLoading")
                    binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                } catch (e: Exception) {
                    Log.e("HomeFragment", "setupObservers: Error in isLoading observer", e)
                }
            }

            restaurantViewModel.error.observe(viewLifecycleOwner) { error ->
                try {
                    error?.let {
                        Log.e("HomeFragment", "setupObservers: Error received: $it")
                        Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("HomeFragment", "setupObservers: Error in error observer", e)
                }
            }
            
            Log.d("HomeFragment", "setupObservers: Completed")
        } catch (e: Exception) {
            Log.e("HomeFragment", "setupObservers: Fatal error", e)
            throw e
        }
    }

    private fun setupSearch() {
        try {
            Log.d("HomeFragment", "setupSearch: Starting")
            binding.searchView.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    try {
                        restaurantViewModel.searchRestaurants(s?.toString() ?: "")
                    } catch (e: Exception) {
                        Log.e("HomeFragment", "setupSearch: Error in search", e)
                    }
                }
            })
            Log.d("HomeFragment", "setupSearch: Completed")
        } catch (e: Exception) {
            Log.e("HomeFragment", "setupSearch: Error", e)
            e.printStackTrace()
        }
    }

    private fun setupOrderTypeToggle() {
        binding.btnDelivery.setOnClickListener {
            sessionManager.saveOrderType("delivery")
            restaurantViewModel.setOrderType("delivery")
            updateOrderTypeToggleUi(isPickup = false)
        }
        binding.btnPickup.setOnClickListener {
            val currentType = sessionManager.getOrderType()
            if (currentType == "pickup") {
                sessionManager.saveOrderType("pickup")
                restaurantViewModel.setOrderType("pickup")
                updateOrderTypeToggleUi(isPickup = true)
                return@setOnClickListener
            }
            showPickUpBottomSheet()
        }
    }

    private fun showPickUpBottomSheet() {
        val dialog = BottomSheetDialog(requireContext())
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_pick_up, null)
        dialog.setContentView(sheetView)
        sheetView.findViewById<View>(R.id.btnClose).setOnClickListener { dialog.dismiss() }
        sheetView.findViewById<View>(R.id.btnNo).setOnClickListener { dialog.dismiss() }
        sheetView.findViewById<View>(R.id.btnYes).setOnClickListener {
            dialog.dismiss()
            sessionManager.saveOrderType("pickup")
            restaurantViewModel.setOrderType("pickup")
            updateOrderTypeToggleUi(isPickup = true)
        }
        dialog.show()
    }

    private fun updateOrderTypeToggleUi(isPickup: Boolean) {
        val primaryColor = ContextCompat.getColor(requireContext(), R.color.primary_color)
        val surfaceVariant = ContextCompat.getColor(requireContext(), R.color.surface_variant)
        val onSurface = ContextCompat.getColor(requireContext(), R.color.on_surface)
        val white = Color.WHITE
        binding.btnDelivery.setBackgroundTintList(ColorStateList.valueOf(if (isPickup) surfaceVariant else primaryColor))
        binding.btnDelivery.setTextColor(if (isPickup) onSurface else white)
        binding.btnDelivery.iconTint = ColorStateList.valueOf(if (isPickup) onSurface else white)
        binding.btnPickup.setBackgroundTintList(ColorStateList.valueOf(if (isPickup) primaryColor else surfaceVariant))
        binding.btnPickup.setTextColor(if (isPickup) white else onSurface)
        binding.btnPickup.iconTint = ColorStateList.valueOf(if (isPickup) white else onSurface)
    }

    private fun setupOrderTracking() {
        try {
            Log.d("HomeFragment", "setupOrderTracking: Starting")
            
            // Get the root view of the included layout
            val cardView = binding.root.findViewById<View>(R.id.cardOrderTracking)
            val trackButton = cardView?.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnTrackOrder)
            
            // Track button click listener will be set in displayOrderTrackingCard

            // Observe orders to show/hide tracking card
            orderViewModel.orders.observe(viewLifecycleOwner) { orders ->
                try {
                    val activeOrders = orders.filter { 
                        it.status.lowercase() in listOf("pending", "confirmed", "preparing", "ready")
                    }
                    
                    if (activeOrders.isNotEmpty()) {
                        // Show the most recent active order
                        val mostRecentOrder = activeOrders.maxByOrNull { 
                            it.created_at ?: ""
                        } ?: activeOrders.first()
                        
                        // Update tracking order number if it changed
                        if (currentTrackingOrderNumber != mostRecentOrder.order_number) {
                            currentTrackingOrderNumber = mostRecentOrder.order_number
                            stopPolling() // Stop old polling if order changed
                        }
                        
                        displayOrderTrackingCard(mostRecentOrder)
                        cardView?.visibility = View.VISIBLE
                        
                        // Start polling for real-time updates
                        if (!isPolling && mostRecentOrder.order_number != null) {
                            startPolling(mostRecentOrder.order_number)
                        }
                    } else {
                        cardView?.visibility = View.GONE
                        stopPolling()
                        currentTrackingOrderNumber = null
                    }
                } catch (e: Exception) {
                    Log.e("HomeFragment", "setupOrderTracking: Error in orders observer", e)
                }
            }
            
            Log.d("HomeFragment", "setupOrderTracking: Completed")
        } catch (e: Exception) {
            Log.e("HomeFragment", "setupOrderTracking: Error", e)
            e.printStackTrace()
        }
    }

    private fun displayOrderTrackingCard(order: com.mnsf.resturantandroid.data.model.Order) {
        try {
            val cardView = binding.root.findViewById<View>(R.id.cardOrderTracking)
            val orderNumberTextView = cardView?.findViewById<android.widget.TextView>(R.id.tvOrderNumber)
            val orderStatusTextView = cardView?.findViewById<android.widget.TextView>(R.id.tvOrderStatus)
            
            orderNumberTextView?.text = order.order_number
            
            // Format status
            val statusText = when (order.status.lowercase()) {
                "pending" -> getString(com.mnsf.resturantandroid.R.string.status_pending)
                "confirmed" -> getString(com.mnsf.resturantandroid.R.string.status_confirmed)
                "preparing" -> getString(com.mnsf.resturantandroid.R.string.status_preparing)
                "ready" -> getString(com.mnsf.resturantandroid.R.string.status_ready)
                "completed" -> getString(com.mnsf.resturantandroid.R.string.status_completed)
                "cancelled" -> getString(com.mnsf.resturantandroid.R.string.status_cancelled)
                else -> order.status
            }
            orderStatusTextView?.text = statusText
            
            // Update button click listener with current order
            val trackButton = cardView?.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnTrackOrder)
            trackButton?.setOnClickListener {
                val intent = Intent(requireContext(), OrderTrackingActivity::class.java)
                intent.putExtra("order", order as java.io.Serializable)
                intent.putExtra("order_number", order.order_number)
                startActivity(intent)
            }
        } catch (e: Exception) {
            Log.e("HomeFragment", "displayOrderTrackingCard: Error", e)
        }
    }

    private fun startPolling(orderNumber: String) {
        if (isPolling || orderNumber.isEmpty()) return
        
        isPolling = true
        pollingJob = lifecycleScope.launch {
            while (isPolling) {
                delay(5000) // Poll every 5 seconds
                
                if (!isPolling) break
                
                try {
                    val orderRepository = OrderRepository(RetrofitClient.apiService, sessionManager)
                    val result = orderRepository.getOrderByNumber(orderNumber)
                    
                    result.onSuccess { updatedOrder ->
                        // Check if order status changed
                        val cardView = binding.root.findViewById<View>(R.id.cardOrderTracking)
                        val currentStatusView = cardView?.findViewById<android.widget.TextView>(R.id.tvOrderStatus)
                        val currentStatus = currentStatusView?.text?.toString() ?: ""
                        
                        val newStatusText = when (updatedOrder.status.lowercase()) {
                            "pending" -> getString(com.mnsf.resturantandroid.R.string.status_pending)
                            "confirmed" -> getString(com.mnsf.resturantandroid.R.string.status_confirmed)
                            "preparing" -> getString(com.mnsf.resturantandroid.R.string.status_preparing)
                            "ready" -> getString(com.mnsf.resturantandroid.R.string.status_ready)
                            "completed" -> getString(com.mnsf.resturantandroid.R.string.status_completed)
                            "cancelled" -> getString(com.mnsf.resturantandroid.R.string.status_cancelled)
                            else -> updatedOrder.status
                        }
                        
                        // Update card if status changed
                        if (currentStatus != newStatusText) {
                            displayOrderTrackingCard(updatedOrder)
                            
                            // Animate status change
                            currentStatusView?.animate()
                                ?.scaleX(1.1f)
                                ?.scaleY(1.1f)
                                ?.setDuration(200)
                                ?.withEndAction {
                                    currentStatusView?.animate()
                                        ?.scaleX(1.0f)
                                        ?.scaleY(1.0f)
                                        ?.setDuration(200)
                                        ?.start()
                                }
                                ?.start()
                        }
                        
                        // Stop polling if order is completed or cancelled
                        if (updatedOrder.status.lowercase() in listOf("completed", "cancelled")) {
                            stopPolling()
                            // Reload orders to update the list
                            orderViewModel.loadOrders()
                        }
                    }.onFailure { exception ->
                        Log.e("HomeFragment", "Polling error: ${exception.message}")
                    }
                } catch (e: Exception) {
                    Log.e("HomeFragment", "Polling exception", e)
                }
            }
        }
    }

    private fun stopPolling() {
        isPolling = false
        pollingJob?.cancel()
        pollingJob = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopPolling()
        _binding = null
    }
    
    override fun onPause() {
        super.onPause()
        // Optionally stop polling when fragment is paused to save resources
        // stopPolling()
    }
    
    override fun onResume() {
        super.onResume()
        // When TEST_HELLO_WORLD=true we never set sessionManager/orderViewModel — skip order tracking to avoid crash
        if (TEST_HELLO_WORLD) return
        // Order tracking disabled (ENABLE_ORDER_TRACKING = false) — restart polling only when enabled
        if (!ENABLE_ORDER_TRACKING) return
        if (::sessionManager.isInitialized && sessionManager.isLoggedIn() && ::orderViewModel.isInitialized) {
            currentTrackingOrderNumber?.let { orderNumber ->
                if (!isPolling) {
                    startPolling(orderNumber)
                }
            }
        }
    }
}

class RestaurantViewModelFactory(
    private val restaurantRepository: RestaurantRepository
) : ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RestaurantViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RestaurantViewModel(restaurantRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class OrderViewModelFactory(
    private val orderRepository: OrderRepository,
    private val sessionManager: SessionManager
) : ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(com.mnsf.resturantandroid.viewmodel.OrderViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return com.mnsf.resturantandroid.viewmodel.OrderViewModel(orderRepository, sessionManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}