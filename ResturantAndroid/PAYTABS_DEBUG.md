# PayTabs: Capture crash log when process restarts

The app correctly reaches PayTabs (`[11]` `[12]` in CheckoutPay logs), but then **PROCESS ENDED** and the app restarts (you end up on Cart). To find the **exact cause** (crash or kill), we need the **full** logcat at that moment, not only the `CheckoutPay` filter.

**Try this first:** On the device/emulator, open **Settings → Developer options** and **turn OFF** "Don't keep activities" if it is ON. Then test PayTabs again. (When that option is on, Android destroys activities as soon as they go to the background, which can cause the app to restart when PayTabs opens.)

## Option A – Android Studio (recommended)

1. In **Logcat**, **clear** the log (trash icon) and **remove any filter** (e.g. set the filter dropdown to "No Filters" or leave the search box empty).
2. Reproduce: add to cart → Checkout → select **Credit/Debit** → tap **Place order** → wait for the 5‑second countdown.
3. As soon as you see the app restart (back to Cart/Home), **stop** and in Logcat:
   - Scroll to the time when **PROCESS ENDED** and **PROCESS STARTED** appear.
   - Select **all** lines from about 2–3 seconds **before** PROCESS ENDED until a few seconds **after** PROCESS STARTED.
   - Copy (Ctrl+C / Cmd+C) and save to a file or paste in your reply.

4. **Or** use the filter: type **`AndroidRuntime`** or **`FATAL`** in the Logcat search. After reproducing, copy any **FATAL EXCEPTION** block (the whole stack trace).

## Option B – Terminal (device connected via USB) – **use this to get the crash**

1. In a terminal, run (this captures **everything**; no filter):
   ```bash
   adb logcat -c && adb logcat > paytabs_full.txt
   ```
2. On the device: add to cart → Checkout → Credit/Debit → Place order → wait for countdown until the app restarts (back to Cart).
3. In the terminal: press **Ctrl+C** to stop logcat.
4. Open `paytabs_full.txt` and search for (in this order):
   - **`FATAL EXCEPTION`** – copy the **entire** block (exception + stack trace, usually 20–40 lines).
   - **`am_proc_died`** or **`Process.*killed`** – copy the line and a few lines before/after.
   - **`PROCESS ENDED`** – copy ~80 lines **before** it (the crash is usually just above PROCESS ENDED).

5. Share that snippet. Without it we cannot see why the process ended (crash vs system kill).

## What we’re looking for

- A line like **`FATAL EXCEPTION in main`** or **`Process: com.mnsf.resturantandroid`** followed by **`java.lang.RuntimeException`** / **`NullPointerException`** / etc. and a **stack trace** (lines starting with `at com.` or `at android.`).
- Or a line like **`Kill (signal 9)`** / **`killing`** that would mean the system killed the process (e.g. OOM).

With that crash/kill reason we can fix the PayTabs integration (e.g. theme, SDK call, or launch flags).

---

## Option C – Crash saved to file (after restart)

The app now **saves the last uncaught exception** to a file. When the app **restarts** (new process), it **logs that crash** at startup so you can capture it.

1. Reproduce: Place order with card → countdown → app restarts (back to Cart/Home).
2. **Do not** clear Logcat. When the app has restarted, in Logcat **filter by `CrashCapture`**.
3. If the process ended due to a **crash** (not a system kill), you should see **"PREVIOUS RUN CRASH"** and the full stack trace. Copy that block and send it.
4. If you see nothing under `CrashCapture`, the process was likely **killed** (e.g. OOM) rather than crashed — use Option A or B to capture the full logcat around PROCESS ENDED.
