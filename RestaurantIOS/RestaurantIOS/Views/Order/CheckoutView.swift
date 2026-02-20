//
//  CheckoutView.swift
//  RestaurantIOS
//
//  Created on 1/21/26.
//

import SwiftUI
import CoreLocation

struct CheckoutView: View {
    @StateObject private var cartManager = CartManager.shared
    @StateObject private var orderViewModel = OrderViewModel()
    @StateObject private var sessionManager = SessionManager.shared
    @StateObject private var restaurantViewModel = RestaurantViewModel()
    
    @State private var customerName = ""
    @State private var customerEmail = ""
    @State private var customerPhone = ""
    @State private var customerAddress = ""
    @State private var selectedOrderType = "pickup"
    @State private var selectedPaymentMethod = "cash"
    @State private var notes = ""
    @State private var deliveryInstructions = ""
    @State private var showConfirmation = false
    @State private var confirmedOrder: Order?
    @State private var serviceFeeFromSettings: Double = 0
    @State private var selectedAddress: Address? = nil
    @State private var deliveryLat: Double?
    @State private var deliveryLng: Double?
    @State private var locationError: String?
    @State private var gettingLocation = false
    @State private var tipAmount: Double = 0
    @State private var tipCustomText: String = ""
    @State private var selectedTipChip: Int? = 0 // 0=none, 1=1, 2=2, 3=custom
    @State private var showCustomTipRow = false
    @State private var selectedDeliveryInstruction: String?
    @State private var useInstructionsForAddress = false
    @State private var showAddressPicker = false
    @State private var savedAddresses: [Address] = []
    
    @Environment(\.dismiss) private var dismiss
    var onOrderComplete: (() -> Void)?
    
    var restaurant: Restaurant? {
        guard let restaurantId = cartManager.restaurantId else { return nil }
        return restaurantViewModel.restaurants.first { $0.id == restaurantId }
    }
    
    var subtotal: Double {
        cartManager.total
    }
    
    var tax: Double {
        guard let restaurant = restaurant, let taxEnabled = restaurant.tax_enabled, taxEnabled,
              let taxRate = restaurant.tax_rate else {
            return 0
        }
        return subtotal * (taxRate / 100)
    }
    
    /// Delivery fee: use zone_price from selected address when restaurant uses delivery company; else restaurant.delivery_fee.
    var deliveryFee: Double {
        guard selectedOrderType == "delivery" else { return 0 }
        if let restaurant = restaurant,
           let companyId = restaurant.delivery_company_id,
           companyId > 0,
           let zonePrice = selectedAddress?.zone_price,
           zonePrice > 0 {
            return zonePrice
        }
        guard let restaurant = restaurant, let fee = restaurant.delivery_fee else { return 0 }
        return fee
    }
    
    var pickupDiscount: Double {
        guard selectedOrderType == "pickup", let r = restaurant, (r.delivery_fee ?? 0) > 0 else { return 0 }
        return r.delivery_fee ?? 0
    }
    
    var effectiveTip: Double {
        guard selectedOrderType == "delivery" else { return 0 }
        if selectedTipChip == 1 { return 1 }
        if selectedTipChip == 2 { return 2 }
        if selectedTipChip == 3 { return Double(tipCustomText.replacingOccurrences(of: ",", with: ".")) ?? 0 }
        return tipAmount
    }
    
    var total: Double {
        let base = subtotal + tax + serviceFeeFromSettings
        if selectedOrderType == "delivery" {
            return base + deliveryFee + effectiveTip
        }
        if selectedOrderType == "pickup" {
            return base - pickupDiscount
        }
        return base
    }
    
    private func formatPrice(_ amount: Double) -> String {
        CurrencyFormatter.format(amount, currencyCode: restaurant?.currency_code, symbolPosition: restaurant?.currency_symbol_position)
    }
    
    private var displayAddressString: String {
        if let addr = selectedAddress {
            return [addr.address_label, addr.street, addr.building_name].compactMap { $0 }.filter { !$0.isEmpty }.joined(separator: ", ")
        }
        return customerAddress.isEmpty ? "—" : customerAddress
    }
    
    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    // Order type (match Android: allow change)
                    if let restaurant = restaurant {
                        orderTypeSection(restaurant: restaurant)
                    }
                    if selectedOrderType == "pickup" {
                        pickupInfoCard
                    }
                    if selectedOrderType == "delivery" {
                        deliveryAddressCard
                        deliveryTimeCard
                        tipCard
                        deliveryInstructionsCard
                        // Customer info for delivery (name, phone, address)
                        customerInfoSection
                    }
                    if restaurant != nil {
                        payWithCard
                        paymentSummaryCard
                    }
                    notesSection
                }
                .padding(.horizontal, 20)
                .padding(.bottom, 120)
            }
            .background(Color(.systemGroupedBackground))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(action: { dismiss() }) {
                        Image(systemName: "chevron.left")
                    }
                }
                ToolbarItem(placement: .principal) {
                    VStack(spacing: 2) {
                        Text(LocalizedStrings.checkout)
                            .font(.headline)
                        if let r = restaurant {
                            Text(r.restaurant_name)
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    }
                }
            }
            .onAppear {
                selectedOrderType = sessionManager.getOrderType()
                if sessionManager.isLoggedIn {
                    customerName = sessionManager.getCustomerName() ?? ""
                    customerEmail = sessionManager.getCustomerEmail() ?? ""
                    customerPhone = sessionManager.getCustomerPhone() ?? ""
                    customerAddress = sessionManager.getCustomerAddress() ?? ""
                }
                if let restaurantId = cartManager.restaurantId {
                    Task {
                        if restaurantViewModel.restaurants.first(where: { $0.id == restaurantId }) == nil {
                            await restaurantViewModel.fetchRestaurant(id: restaurantId)
                        } else {
                            await restaurantViewModel.fetchRestaurants()
                        }
                        await loadSettings()
                        await loadAddressesForDeliveryFeeIfNeeded()
                    }
                }
            }
            .sheet(isPresented: $showAddressPicker) {
                AddressPickerSheet(
                    addresses: savedAddresses,
                    onSelect: { addr in
                        selectedAddress = addr
                        customerAddress = [addr.address_label, addr.street, addr.building_name].compactMap { $0 }.filter { !$0.isEmpty }.joined(separator: ", ")
                    },
                    onDismiss: { showAddressPicker = false }
                )
            }
            .alert("Error", isPresented: .constant(orderViewModel.errorMessage != nil)) {
                Button("OK") {
                    orderViewModel.errorMessage = nil
                }
            } message: {
                if let error = orderViewModel.errorMessage {
                    Text(error)
                }
            }
            .navigationDestination(isPresented: $showConfirmation) {
                if let order = confirmedOrder {
                    OrderConfirmationView(order: order) {
                        dismiss()
                        onOrderComplete?()
                    }
                }
            }
            .safeAreaInset(edge: .bottom) {
                Button(action: { placeOrder() }) {
                    if orderViewModel.isLoading {
                        ProgressView()
                            .frame(maxWidth: .infinity)
                    } else {
                        Text(LocalizedStrings.placeOrder)
                            .frame(maxWidth: .infinity)
                    }
                }
                .buttonStyle(.borderedProminent)
                .padding(.horizontal, 20)
                .padding(.vertical, 16)
                .disabled(orderViewModel.isLoading || !isFormValid)
            }
        }
    }
    
    private func orderTypeSection(restaurant: Restaurant) -> some View {
        CheckoutCard {
            VStack(alignment: .leading, spacing: 12) {
                Text(LocalizedStrings.orderType)
                    .font(.headline)
                Picker(LocalizedStrings.orderType, selection: $selectedOrderType) {
                    if restaurant.order_type_pickup_enabled == true {
                        Text(LocalizedStrings.pickup).tag("pickup")
                    }
                    if restaurant.order_type_dine_in_enabled == true {
                        Text(LocalizedStrings.dineIn).tag("dine_in")
                    }
                    if restaurant.order_type_delivery_enabled == true {
                        Text(LocalizedStrings.delivery).tag("delivery")
                    }
                }
                .pickerStyle(.segmented)
                .onChange(of: selectedOrderType) { _, newValue in
                    sessionManager.saveOrderType(newValue)
                }
            }
        }
    }
    
    private var pickupInfoCard: some View {
        CheckoutCard {
            VStack(alignment: .leading, spacing: 0) {
                Rectangle()
                    .fill(Color.gray.opacity(0.2))
                    .frame(height: 160)
                    .overlay { Image(systemName: "map").font(.largeTitle).foregroundColor(.gray) }
                HStack(spacing: 12) {
                    Image(systemName: "building.2")
                        .foregroundColor(.accentColor)
                    VStack(alignment: .leading, spacing: 2) {
                        if let r = restaurant {
                            Text(r.restaurant_name)
                                .font(.subheadline)
                                .fontWeight(.semibold)
                        }
                        Text(LocalizedStrings.readyForPickupApprox)
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                    Spacer()
                }
                .padding(16)
            }
        }
    }
    
    private var deliveryAddressCard: some View {
        CheckoutCard {
            VStack(alignment: .leading, spacing: 0) {
                Rectangle()
                    .fill(Color.gray.opacity(0.2))
                    .frame(height: 160)
                    .overlay { Image(systemName: "map").font(.largeTitle).foregroundColor(.gray) }
                HStack(alignment: .top, spacing: 12) {
                    Image(systemName: "location")
                        .foregroundColor(.secondary)
                    VStack(alignment: .leading, spacing: 4) {
                        Text(selectedAddress?.address_label ?? LocalizedStrings.deliveryAddress)
                            .font(.subheadline)
                            .fontWeight(.semibold)
                        Text(displayAddressString)
                            .font(.caption)
                            .foregroundColor(.secondary)
                        if let phone = selectedAddress?.phone_number ?? (customerPhone.isEmpty ? nil : customerPhone) {
                            Text("\(LocalizedStrings.mobileNumber): \(phone)")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    }
                    Spacer()
                    Button(LocalizedStrings.change) { showAddressPicker = true }
                        .font(.subheadline)
                }
                .padding(16)
            }
        }
    }
    
    private var deliveryTimeCard: some View {
        CheckoutCard {
            HStack(spacing: 12) {
                Image(systemName: "car")
                    .foregroundColor(.primary)
                VStack(alignment: .leading, spacing: 2) {
                    Text(LocalizedStrings.delivery)
                        .font(.subheadline)
                        .fontWeight(.semibold)
                    Text(LocalizedStrings.arrivingInApproxMins(15, 25))
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                Spacer()
            }
            .padding(16)
        }
    }
    
    private var tipCard: some View {
        CheckoutCard {
            VStack(alignment: .leading, spacing: 12) {
                Text(LocalizedStrings.sayThanksWithTip)
                    .font(.subheadline)
                    .fontWeight(.semibold)
                Text(LocalizedStrings.yourRiderKeepsTips)
                    .font(.caption)
                    .foregroundColor(.secondary)
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        tipChip(amount: 1, tag: 1)
                        tipChip(amount: 2, tag: 2)
                        tipChipCustom
                    }
                }
                if showCustomTipRow {
                    HStack(spacing: 8) {
                        TextField(LocalizedStrings.tipCustomHint, text: $tipCustomText)
                            .keyboardType(.decimalPad)
                            .textFieldStyle(.roundedBorder)
                        Button(action: {
                            if let v = Double(tipCustomText.replacingOccurrences(of: ",", with: ".")) {
                                tipAmount = max(0, v)
                            }
                            showCustomTipRow = false
                        }) {
                            Text(LocalizedStrings.apply)
                                .font(.caption)
                        }
                        .buttonStyle(.borderedProminent)
                    }
                }
            }
            .padding(16)
        }
    }
    
    private func tipChip(amount: Int, tag: Int) -> some View {
        let isSelected = selectedTipChip == tag
        return Button(action: {
            selectedTipChip = tag
            showCustomTipRow = false
        }) {
            Text(formatPrice(Double(amount)))
                .font(.subheadline)
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
        }
        .buttonStyle(.bordered)
        .tint(isSelected ? .accentColor : .secondary)
        .overlay(RoundedRectangle(cornerRadius: 8).stroke(isSelected ? Color.accentColor : Color.clear, lineWidth: 2))
    }
    
    private var tipChipCustom: some View {
        let isSelected = selectedTipChip == 3
        return Button(action: {
            selectedTipChip = 3
            showCustomTipRow = true
        }) {
            Text(LocalizedStrings.tipCustom)
                .font(.subheadline)
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
        }
        .buttonStyle(.bordered)
        .tint(isSelected ? .accentColor : .secondary)
        .overlay(RoundedRectangle(cornerRadius: 8).stroke(isSelected ? Color.accentColor : Color.clear, lineWidth: 2))
    }
    
    private var deliveryInstructionsCard: some View {
        CheckoutCard {
            VStack(alignment: .leading, spacing: 12) {
                Text(LocalizedStrings.deliveryInstructions)
                    .font(.subheadline)
                    .fontWeight(.semibold)
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        instructionChip(LocalizedStrings.callOnArrival)
                        instructionChip(LocalizedStrings.dontRingBell)
                        instructionChip(LocalizedStrings.leaveAtReception)
                        instructionChip(LocalizedStrings.ringDoorbell)
                    }
                }
                Toggle(LocalizedStrings.useMyInstructionsForAddress, isOn: $useInstructionsForAddress)
                    .font(.caption)
            }
            .padding(16)
        }
    }
    
    private func instructionChip(_ title: String) -> some View {
        let isSelected = selectedDeliveryInstruction == title
        return Button(action: {
            selectedDeliveryInstruction = isSelected ? nil : title
        }) {
            Text(title)
                .font(.caption)
                .padding(.horizontal, 10)
                .padding(.vertical, 6)
        }
        .buttonStyle(.bordered)
        .tint(isSelected ? .accentColor : .secondary)
    }
    
    private var customerInfoSection: some View {
        CheckoutCard {
            VStack(alignment: .leading, spacing: 12) {
                Text(LocalizedStrings.customerInformation)
                    .font(.headline)
                TextField(LocalizedStrings.namePlaceholder, text: $customerName)
                TextField(LocalizedStrings.emailPlaceholder, text: $customerEmail)
                    .keyboardType(.emailAddress)
                    .autocapitalization(.none)
                TextField(LocalizedStrings.phonePlaceholder, text: $customerPhone)
                    .keyboardType(.phonePad)
                TextField(LocalizedStrings.deliveryAddressPlaceholder, text: $customerAddress, axis: .vertical)
                    .lineLimit(2...4)
                Button(action: requestLocation) {
                    HStack {
                        if gettingLocation { ProgressView().scaleEffect(0.8); Text(LocalizedStrings.loading) }
                        else { Image(systemName: "location.fill"); Text("Use my location") }
                    }
                    .frame(maxWidth: .infinity)
                }
                .disabled(gettingLocation)
            }
            .padding(16)
        }
    }
    
    private var payWithCard: some View {
        Group {
            if let restaurant = restaurant {
                let methods = parsePaymentMethods(restaurant.payment_methods)
                let hasCash = (selectedOrderType == "delivery" && methods.cashOnDelivery) || (selectedOrderType != "delivery" && methods.cashOnPickup)
                let hasAny = hasCash || methods.creditCard || methods.onlinePayment || methods.mobilePayment || (methods.cliQServices?.enabled == true)
                if hasAny {
                    CheckoutCard {
                        VStack(alignment: .leading, spacing: 12) {
                            Text(LocalizedStrings.payWith)
                                .font(.headline)
                            Picker(LocalizedStrings.paymentMethod, selection: $selectedPaymentMethod) {
                                if hasCash {
                                    Text(LocalizedStrings.cash).tag(selectedOrderType == "delivery" ? "cash_on_delivery" : "cash")
                                }
                                if methods.creditCard { Text(LocalizedStrings.creditCard).tag("credit_card") }
                                if methods.onlinePayment { Text(LocalizedStrings.creditCard).tag("online") }
                                if methods.mobilePayment { Text("Mobile").tag("mobile") }
                                if methods.cliQServices?.enabled == true { Text("CliQ").tag("cliq") }
                            }
                            .pickerStyle(.menu)
                            HStack(spacing: 8) {
                                Image(systemName: "lock.shield")
                                    .foregroundColor(.accentColor)
                                Text(LocalizedStrings.protectedByPci)
                                    .font(.caption)
                                    .foregroundColor(.accentColor)
                            }
                        }
                        .padding(16)
                    }
                }
            }
        }
    }
    
    private var paymentSummaryCard: some View {
        CheckoutCard {
            VStack(alignment: .leading, spacing: 12) {
                Text(LocalizedStrings.paymentSummary)
                    .font(.headline)
                Text("\(cartManager.itemCount) \(LocalizedStrings.items)")
                    .font(.subheadline)
                row(LocalizedStrings.subtotal, formatPrice(subtotal))
                if selectedOrderType == "delivery" && deliveryFee > 0 {
                    row(LocalizedStrings.deliveryFee, formatPrice(deliveryFee))
                }
                if selectedOrderType == "pickup" && pickupDiscount > 0 {
                    row(LocalizedStrings.pickupDiscount, "- " + formatPrice(pickupDiscount))
                }
                if tax > 0 { row(LocalizedStrings.salesTax, formatPrice(tax)) }
                if serviceFeeFromSettings > 0 {
                    HStack {
                        Text(LocalizedStrings.serviceFee)
                        Spacer()
                        Text(formatPrice(serviceFeeFromSettings))
                    }
                    .font(.subheadline)
                }
                if selectedOrderType == "delivery" && effectiveTip > 0 {
                    row(LocalizedStrings.riderTip, formatPrice(effectiveTip))
                }
                Divider()
                HStack {
                    Text(LocalizedStrings.totalAmount)
                        .fontWeight(.semibold)
                    Spacer()
                    Text(formatPrice(total))
                        .fontWeight(.semibold)
                }
            }
            .padding(16)
        }
    }
    
    private func row(_ label: String, _ value: String) -> some View {
        HStack {
            Text(label)
            Spacer()
            Text(value)
        }
        .font(.subheadline)
    }
    
    private var notesSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(LocalizedStrings.notesOptional)
                .font(.caption)
                .foregroundColor(.secondary)
            TextField(LocalizedStrings.notes, text: $notes, axis: .vertical)
                .textFieldStyle(.roundedBorder)
                .lineLimit(2...4)
        }
        .padding(4)
    }
    
    private var isFormValid: Bool {
        guard !customerName.isEmpty, !customerPhone.isEmpty else { return false }
        if selectedOrderType == "delivery" {
            return (deliveryLat != nil && deliveryLng != nil) || !customerAddress.isEmpty || (selectedAddress != nil)
        }
        return true
    }
    
    private func requestLocation() {
        gettingLocation = true
        locationError = nil
        LocationHelper.shared.requestLocationPermission()
        LocationHelper.shared.getCurrentLocation { [self] location in
            Task { @MainActor in
                if let loc = location {
                    deliveryLat = loc.coordinate.latitude
                    deliveryLng = loc.coordinate.longitude
                    locationError = nil
                } else {
                    locationError = "Could not get location"
                }
                gettingLocation = false
            }
        }
    }
    
    private func loadSettings() async {
        do {
            let response = try await APIService.shared.getSettings()
            await MainActor.run {
                serviceFeeFromSettings = response.service_fee ?? 0
            }
        } catch { _ = () }
    }
    
    private func loadAddressesForDeliveryFeeIfNeeded() async {
        guard let restaurant = restaurant,
              let companyId = restaurant.delivery_company_id,
              companyId > 0,
              sessionManager.isLoggedIn,
              let token = sessionManager.getAuthToken() else { return }
        let customerId = sessionManager.getCustomerId()
        guard customerId != -1 else { return }
        do {
            let response = try await APIService.shared.getAddresses(customerId: customerId, token: token)
            await MainActor.run {
                savedAddresses = response.addresses
                selectedAddress = response.addresses.first { $0.is_default } ?? response.addresses.first
            }
        } catch { _ = () }
    }
    
    private func parsePaymentMethods(_ jsonString: String?) -> PaymentMethods {
        guard let jsonString = jsonString,
              let data = jsonString.data(using: .utf8),
              let paymentMethods = try? JSONDecoder().decode(PaymentMethods.self, from: data) else {
            // Default payment methods
            return PaymentMethods(
                cashOnPickup: true,
                cashOnDelivery: true,
                creditCard: false,
                onlinePayment: false,
                mobilePayment: false
            )
        }
        return paymentMethods
    }
    
    private func placeOrder() {
        guard let restaurantId = cartManager.restaurantId else { return }
        let customerId: Int? = sessionManager.getCustomerId() != -1 ? sessionManager.getCustomerId() : nil
        let deliveryAddr = selectedOrderType == "delivery" ? (selectedAddress?.street ?? (customerAddress.isEmpty ? nil : customerAddress)) : nil
        let instructions = selectedOrderType == "delivery" ? (selectedDeliveryInstruction ?? (deliveryInstructions.isEmpty ? nil : deliveryInstructions)) : nil
        let tipValue = selectedOrderType == "delivery" ? effectiveTip : nil
        
        Task {
            let success = await orderViewModel.placeOrder(
                websiteId: restaurantId,
                customerId: customerId,
                customerName: customerName,
                customerEmail: customerEmail.isEmpty ? nil : customerEmail,
                customerPhone: customerPhone,
                customerAddress: deliveryAddr,
                orderType: selectedOrderType,
                paymentMethod: selectedPaymentMethod,
                deliveryLatitude: selectedOrderType == "delivery" ? deliveryLat : nil,
                deliveryLongitude: selectedOrderType == "delivery" ? deliveryLng : nil,
                deliveryInstructions: instructions,
                tip: tipValue,
                items: cartManager.getItemsForOrder(),
                totalAmount: total,
                notes: notes.isEmpty ? nil : notes
            )
            if success, let order = orderViewModel.currentOrder {
                cartManager.clearCart()
                confirmedOrder = order
                showConfirmation = true
            }
        }
    }
}

private struct CheckoutCard<Content: View>: View {
    let content: Content
    init(@ViewBuilder content: () -> Content) {
        self.content = content()
    }
    var body: some View {
        content
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color(.systemBackground))
            .cornerRadius(12)
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(Color(.systemGray4), lineWidth: 1)
            )
    }
}

private struct AddressPickerSheet: View {
    let addresses: [Address]
    let onSelect: (Address) -> Void
    let onDismiss: () -> Void
    @Environment(\.dismiss) private var dismiss
    
    var body: some View {
        NavigationStack {
            List(addresses) { addr in
                Button(action: {
                    onSelect(addr)
                    dismiss()
                }) {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(addr.address_label ?? LocalizedStrings.deliveryAddress)
                            .font(.subheadline)
                            .fontWeight(.medium)
                        if let street = addr.street, !street.isEmpty {
                            Text(street)
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    }
                }
            }
            .navigationTitle(LocalizedStrings.deliveryAddress)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(LocalizedStrings.close) {
                        onDismiss()
                        dismiss()
                    }
                }
            }
        }
    }
}

#Preview {
    CheckoutView(onOrderComplete: nil)
}

