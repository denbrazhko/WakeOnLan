## Wake on Lan

### Description

Wake on Lan lets you power on and manage your devices straight from your phone. Add as many devices
as you like and wake them either from the app or from up to three Quick Settings Tiles for instant
access. The list overview shows each device's online status at a glance, so you always know what's
up and running.

A companion app for Wear OS brings the same control to your wrist, letting you wake your devices
directly from the watch.

### TCP status checks

In the device settings, set **Status host** to the computer address or your DDNS hostname,
and **Status TCP port** to a port forwarded to a running TCP service on that computer.
The default is 22 (for example, OpenSSH Server on Windows); any listening TCP service can
be used. For example, forward router TCP port 22222 to PC port 22 and enter 22222 in the app.
These settings are independent of the UDP address and port used to send Wake-on-LAN packets.

Status checks only open and close a TCP connection, without SSH login or commands.
A successful connection means available; a refused connection or timeout means unavailable.
An empty host disables checking, and DNS failures show an unknown status. A stopped service
or blocked port can make an otherwise running PC appear unavailable. The connect timeout is
two seconds (DNS resolution can take longer), with two seconds between completed checks.
Existing devices and older backups use status port 22 until you change it.

### SSH key authentication for shutdown

Enable remote shutdown in the device editor, then turn on **Authenticate with a private key**.
The SSH username is still required; the key replaces the account password. Import an OpenSSH
or PEM private key file (up to 64 KB) using the file picker. For an encrypted key, enter its
passphrase without removing any leading or trailing spaces. A key authentication failure never
falls back to an account password. Existing password configurations continue to work.

Enter the server's **SHA256 fingerprint** from a trusted local session on that computer.
It is required for key authentication; a different server key blocks the connection before login.
For a Windows OpenSSH server with its default Ed25519 host key, run:

```powershell
ssh-keygen -lf "$env:ProgramData\ssh\ssh_host_ed25519_key.pub" -E sha256
```

Copy only the `SHA256:...` part into the fingerprint field. If the server's host keys or
negotiated key algorithm change, verify the new fingerprint locally before changing the pin.

To create a dedicated phone key on Windows:

```powershell
ssh-keygen -t ed25519 -f "$env:USERPROFILE\.ssh\wol_phone" -C "WakeOnLan phone"
```

Choose a key passphrase when prompted. Install the **public** `wol_phone.pub` on the SSH server.
For an ordinary user, append its contents as a single line in that user's `.ssh\authorized_keys`.
With the default Windows OpenSSH configuration, administrators instead use
`C:\ProgramData\ssh\administrators_authorized_keys`, restricted to Administrators and SYSTEM.
See [Microsoft's key authentication guide](https://learn.microsoft.com/en-us/windows-server/administration/openssh/openssh_keymanagement).
Transfer the **private** `wol_phone` file securely to your phone for import, not the `.pub` file.
The private key is never displayed by the app. Its imported copy and passphrase are encrypted
in the database using Android Keystore. JSON backups omit both; restore requires re-importing
the key and passphrase. Restoring an Android database backup on another phone also requires
re-import because the encryption key does not transfer.

For the example TCP forwarding rule above, use the DDNS hostname and **SSH port 22222**
(the SSH port is separate from the TCP status port). Start with shutdown command `whoami`
and use the test button to verify login without shutting down. Then save
`shutdown.exe /s /t 0` as the Windows shutdown command. The test button executes the current
command, so testing after this change shuts down the computer. The Windows account needs
the right to shut down the system. No server settings or authorized keys are changed by the app.

### Installation

APKs are published on the [Releases](../../releases) page. Download the latest release and
grab the APK you need:

- `app-release.apk` – the phone/tablet app
- `wear-release.apk` – the Wear OS companion app

#### Mobile (phone / tablet)

1. On your Android device, open the [Releases](../../releases) page and download `app-release.apk`.
2. When prompted, allow installing apps from this source (**Settings → Apps → Special app access →
   Install unknown apps**, then enable it for your browser or file manager).
3. Open the downloaded APK and tap **Install**.
4. Launch **Wake on Lan** and add your devices.

#### Wear OS (watch)

The watch app is distributed separately (it is not bundled with the phone app), so it has to be
installed onto the watch directly via [ADB](https://developer.android.com/tools/adb). Make sure the
phone app is installed first, as devices are configured from the phone and synced to the watch.

1. On the watch, enable developer options: **Settings → System → About → Versions**, then tap
   **Build number** repeatedly until developer mode is enabled.
2. In **Settings → Developer options**, enable **ADB debugging** and **Debug over Wi-Fi** (or
   **Wireless debugging**). Note the IP address and port shown.
3. From a computer with the Android SDK platform-tools installed, connect to the watch:
   ```bash
   adb connect <watch-ip>:<port>
   ```
   Accept the debugging prompt on the watch if it appears.
4. Install the companion app:
   ```bash
   adb -s <watch-ip>:<port> install wear-release.apk
   ```
5. Open **Wake on Lan** on the watch. Your devices configured on the phone will appear
   automatically.

### Screenshots

<table>
    <tr>
        <td><img src="screenshots/Device_Overview.png" alt="1"></td>
        <td><img src="screenshots/Device_Quick_Settings.png" alt="2"></td>
        <td><img src="screenshots/Wear_OS.png" alt="3"></td>
    </tr>
    <tr>
        <td><img src="screenshots/Device_Color_Theme.png" alt="4"></td>
        <td><img src="screenshots/Device_Shortcut.png" alt="5"></td>
        <td><img src="screenshots/Device_Quick_Access.png" alt="6"></td>
    </tr> 
</table>
