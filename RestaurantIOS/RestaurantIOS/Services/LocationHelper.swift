//
//  LocationHelper.swift
//  RestaurantIOS
//
//  Created on 1/24/26.
//

import Foundation
import CoreLocation
import Contacts

class LocationHelper: NSObject, ObservableObject {
    static let shared = LocationHelper()
    
    private let locationManager = CLLocationManager()
    private var completionHandler: ((CLLocation?) -> Void)?
    private var geocoder = CLGeocoder()
    
    @Published var authorizationStatus: CLAuthorizationStatus = .notDetermined
    
    override init() {
        super.init()
        locationManager.delegate = self
        locationManager.desiredAccuracy = kCLLocationAccuracyBest
        authorizationStatus = locationManager.authorizationStatus
    }
    
    func requestLocationPermission() {
        locationManager.requestWhenInUseAuthorization()
    }
    
    func hasLocationPermission() -> Bool {
        let status = locationManager.authorizationStatus
        return status == .authorizedWhenInUse || status == .authorizedAlways
    }
    
    func getCurrentLocation(completion: @escaping (CLLocation?) -> Void) {
        guard hasLocationPermission() else {
            print("⚠️ Location permission not granted")
            completion(nil)
            return
        }
        
        guard CLLocationManager.locationServicesEnabled() else {
            print("⚠️ Location services not enabled")
            completion(nil)
            return
        }
        
        completionHandler = completion
        locationManager.requestLocation()
    }
    
    func getAddressFromLocation(latitude: Double, longitude: Double, completion: @escaping (String?) -> Void) {
        let location = CLLocation(latitude: latitude, longitude: longitude)
        
        geocoder.reverseGeocodeLocation(location) { placemarks, error in
            if let error = error {
                print("❌ Reverse geocoding error: \(error.localizedDescription)")
                completion(nil)
                return
            }
            
            guard let placemark = placemarks?.first else {
                print("⚠️ No placemark found")
                completion(nil)
                return
            }
            
            // Build address string
            var addressComponents: [String] = []
            
            if let streetNumber = placemark.subThoroughfare {
                addressComponents.append(streetNumber)
            }
            
            if let streetName = placemark.thoroughfare {
                addressComponents.append(streetName)
            }
            
            if let city = placemark.locality {
                addressComponents.append(city)
            }
            
            if let state = placemark.administrativeArea {
                addressComponents.append(state)
            }
            
            if let postalCode = placemark.postalCode {
                addressComponents.append(postalCode)
            }
            
            if let country = placemark.country {
                addressComponents.append(country)
            }
            
            let address = addressComponents.joined(separator: ", ")
            completion(address.isEmpty ? nil : address)
        }
    }
}

extension LocationHelper: CLLocationManagerDelegate {
    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        guard let location = locations.first else {
            completionHandler?(nil)
            completionHandler = nil
            return
        }
        
        print("✅ Location obtained: lat=\(location.coordinate.latitude), lng=\(location.coordinate.longitude)")
        completionHandler?(location)
        completionHandler = nil
    }
    
    func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        print("❌ Location error: \(error.localizedDescription)")
        completionHandler?(nil)
        completionHandler = nil
    }
    
    func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        authorizationStatus = manager.authorizationStatus
        print("📍 Location authorization status changed: \(authorizationStatus.rawValue)")
    }
}

