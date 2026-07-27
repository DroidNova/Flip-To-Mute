# Flip to Mute

Flip to Mute is an Android 8.0/API 26+ app that changes an incoming cellular call to Silent or Vibrate after the phone is held face-down briefly.

## Status

The project is in **MVP reliability hardening**. Monitoring is user-enabled, runs in a foreground service with an ongoing notification, and uses event-driven cellular call callbacks. The orientation sensor is activated only while an incoming cellular call is ringing and is stopped when ringing ends or after a face-down action succeeds.

## Privacy and permissions

The app uses:

- Phone state access to observe cellular call-state categories.
- Notification access for the required foreground-service notification.
- Notification Policy Access to apply and restore Silent or Vibrate mode.
- Foreground-service permissions for user-visible monitoring.

Flip to Mute does not read or collect phone numbers, contacts, call logs, caller identity, SIM identifiers, location, microphone data, or internet data.

## Process recovery

Before changing the ringer mode, the app durably records only the previous and app-applied modes. After ordinary process recreation, it restores the previous mode only if the device is still in the app-applied mode; a later manual user change is preserved. User-enabled monitoring uses a sticky service restart, validates current setup again, and recreates a clean monitoring session without restoring old call or sensor state.

On app launch, stale monitoring intent is reconciled after a short bounded wait for a possible system service restart. The app never starts monitoring merely because the UI opened.

## Build and run

1. Open the project in a current Android Studio release.
2. Sync Gradle and install the configured SDK.
3. Run on an API 26+ physical phone with an active SIM.
4. Complete App Setup, enable monitoring, and call the device from another phone.
5. Test face-up, face-down, answered, rejected, missed, outgoing-call, notification Stop, process-recreation, and manual sound-change scenarios.

## Known limitations

- Regular cellular calls only; WhatsApp, Telegram, and other VoIP calls are not supported.
- User force-stop or Android's Active Apps Stop prevents monitoring until the app is explicitly opened again.
- Manufacturer-specific background restrictions may affect reliability; behavior is not guaranteed on every device.
- Automatic start after reboot is not implemented.
- The app does not request a battery-optimisation exemption.
- Recovery cannot run while the application remains force-stopped; it runs after the user opens the app again.
