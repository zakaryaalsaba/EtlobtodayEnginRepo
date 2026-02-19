# Code flow after Login button is clicked

Use this to know which code runs after the user taps Login, so you can uncomment / re-enable parts step by step to find the crash.

---

## 1. Login button click (LoginActivity)

**File:** `app/src/main/java/com/mnsf/resturantandroid/ui/auth/LoginActivity.kt`  
**Method:** `setupClickListeners()` → `binding.btnLogin.setOnClickListener { ... }`

- Reads email/password from `binding.etEmail`, `binding.etPassword`
- Calls **`viewModel.login(email, password)`**

---

## 2. ViewModel (AuthViewModel)

**File:** `app/src/main/java/com/mnsf/resturantandroid/viewmodel/AuthViewModel.kt`  
**Method:** `login(email, password)`

- Builds `LoginRequest(email, password)`
- Calls **`authRepository.login(LoginRequest(...))`** (coroutine)
- On success: `_authState.value = AuthState.Success(authResponse)`
- On failure: `_authState.value = AuthState.Error(...)`

---

## 3. Repository → server (AuthRepository)

**File:** `app/src/main/java/com/mnsf/resturantandroid/repository/AuthRepository.kt`  
**Method:** `login(request: LoginRequest)`

- Calls **`apiService.login(request)`** (Retrofit: POST to `API_BASE_URL` + `auth/login`)
- On success: saves token and customer info via `sessionManager`, returns `Result.success(authResponse)`
- On failure: returns `Result.failure(e)`

---

## 4. LoginActivity observes success

**File:** `app/src/main/java/com/mnsf/resturantandroid/ui/auth/LoginActivity.kt`  
**Method:** `setupObservers()` → `viewModel.authState.observe(...)`

- When **`AuthState.Success`**: shows “login successful” toast, then calls **`navigateToMain()`**

---

## 5. Navigate to MainActivity (LoginActivity)

**File:** `app/src/main/java/com/mnsf/resturantandroid/ui/auth/LoginActivity.kt`  
**Method:** `navigateToMain()`

- Starts **`MainActivity`** with `Intent(this, MainActivity::class.java)` (flags: NEW_TASK, CLEAR_TASK)
- Calls **`finish()`** so LoginActivity is closed

---

## 6. MainActivity loads

**File:** `app/src/main/java/com/mnsf/resturantandroid/ui/MainActivity.kt`  
**Method:** `onCreate()`

- Inflates **`activity_main.xml`** → `ActivityMainBinding.inflate(layoutInflater)`, `setContentView(binding.root)`
- Sets up toolbar, **bottom nav** (`binding.navView`), **drawer** (`setupDrawerNavigation`)
- NavController is tied to **`R.id.nav_host_fragment_activity_main`** with graph **`@navigation/mobile_navigation`**
- **Start destination** of that graph is **`navigation_home`** → **HomeFragment**

So as soon as MainActivity is shown, the **NavHostFragment** loads **HomeFragment**.

---

## 7. HomeFragment loads (where Binary XML #52 has been crashing)

**File:** `app/src/main/java/com/mnsf/resturantandroid/ui/home/HomeFragment.kt`  
**Method:** `onCreateView(...)`

- Inflates **`fragment_home.xml`** → `FragmentHomeBinding.inflate(inflater, container, false)`  ← **crash “Binary XML line #52” has been here**
- Then: `SessionManager`, `OrderViewModel`, `setupOrderTracking()`, `setupRecyclerView()`, `setupOrderTypeToggle()`, `setupObservers()`, `setupSearch()`, `restaurantViewModel.loadRestaurants()`
- Returns `binding.root`

**Layout inflated:** `app/src/main/res/layout/fragment_home.xml`  
**Navigation graph:** `app/src/main/res/navigation/mobile_navigation.xml` (start destination = HomeFragment)

---

## Summary

| Step | File | What runs |
|------|------|-----------|
| 1 | LoginActivity.kt | Button click → `viewModel.login(email, password)` |
| 2 | AuthViewModel.kt | `authRepository.login(...)` |
| 3 | AuthRepository.kt | `apiService.login(request)` → HTTP POST to your Mac |
| 4 | LoginActivity.kt | `authState` observer → Success → `navigateToMain()` |
| 5 | LoginActivity.kt | `navigateToMain()` → start MainActivity, finish() |
| 6 | MainActivity.kt | `onCreate()` → inflate **activity_main.xml**, set up NavController + **mobile_navigation** |
| 7 | HomeFragment.kt | `onCreateView()` → inflate **fragment_home.xml** (crash at Binary XML #52) |

To isolate the crash: HomeFragment is currently in “Hello World” mode. When you turn that off, the code that runs next is the inflation of **fragment_home.xml** and the setup below it in **HomeFragment.onCreateView()**.
