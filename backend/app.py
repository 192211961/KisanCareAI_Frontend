from flask import Flask, request, jsonify
from flask_mysqldb import MySQL
from flask_cors import CORS
from dotenv import load_dotenv
from werkzeug.security import generate_password_hash, check_password_hash
import logging
import os
import random
import smtplib
from email.mime.text import MIMEText
from datetime import datetime, timedelta
import MySQLdb

load_dotenv()

app = Flask(__name__)
CORS(app) # Enable CORS for all routes

# Configure logging
logging.basicConfig(
    filename='backend_error.log',
    level=logging.DEBUG,
    format='%(asctime)s %(levelname)s %(name)s %(threadName)s : %(message)s'
)

# ---------------- MySQL Config ----------------
app.config['MYSQL_HOST'] = 'localhost'
app.config['MYSQL_USER'] = 'root'
app.config['MYSQL_PASSWORD'] = ''
app.config['MYSQL_DB'] = 'kisanmitra'

mysql = MySQL(app)

EMAIL_USER = os.getenv("EMAIL_USER")
EMAIL_PASS = os.getenv("EMAIL_PASS")

def init_db():
    try:
        db = MySQLdb.connect(host=app.config['MYSQL_HOST'], user=app.config['MYSQL_USER'], passwd=app.config['MYSQL_PASSWORD'])
        cursor = db.cursor()
        cursor.execute("CREATE DATABASE IF NOT EXISTS kisanmitra")
        db.select_db("kisanmitra")
        
        # Create users table with all required fields
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS users (
                id INT AUTO_INCREMENT PRIMARY KEY,
                full_name VARCHAR(100) NOT NULL,
                email VARCHAR(100) UNIQUE NOT NULL,
                password VARCHAR(255) NOT NULL,
                otp VARCHAR(6),
                otp_expiry DATETIME,
                mobile VARCHAR(15),
                pincode VARCHAR(6),
                state VARCHAR(100),
                district VARCHAR(100),
                is_verified BOOLEAN DEFAULT FALSE,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """)
        
        # Add new columns to existing users table if they don't exist
        columns_to_add = {
            "mobile": "VARCHAR(15)",
            "pincode": "VARCHAR(6)",
            "state": "VARCHAR(100)",
            "district": "VARCHAR(100)"
        }
        for col, col_type in columns_to_add.items():
            try:
                cursor.execute(f"ALTER TABLE users ADD COLUMN {col} {col_type}")
            except:
                pass
        
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS user_profiles (
                user_id INT PRIMARY KEY,
                mobile VARCHAR(15),
                pincode VARCHAR(6),
                state VARCHAR(100),
                district VARCHAR(100),
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
            )
        """)
        
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS query_history (
                id INT AUTO_INCREMENT PRIMARY KEY,
                user_id INT NOT NULL,
                query TEXT NOT NULL,
                response TEXT,
                is_disease BOOLEAN DEFAULT FALSE,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
            )
        """)
        
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS pending_registrations (
                id INT AUTO_INCREMENT PRIMARY KEY,
                full_name VARCHAR(100) NOT NULL,
                email VARCHAR(100) UNIQUE NOT NULL,
                password VARCHAR(255) NOT NULL,
                otp VARCHAR(6),
                otp_expiry DATETIME,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """)
        
        db.commit()
        cursor.close()
        db.close()
    except Exception as e:
        print(f"DB Init Error: {e}")

def send_otp_email(receiver_email, otp, subject):
    if not EMAIL_USER or not EMAIL_PASS: 
        print("Email credentials not set")
        return False
    msg = MIMEText(f"Your KisanCare AI OTP is: {otp}. Valid for 5 minutes.")
    msg['Subject'] = subject
    msg['From'] = EMAIL_USER
    msg['To'] = receiver_email
    try:
        server = smtplib.SMTP('smtp.gmail.com', 587)
        server.starttls()
        server.login(EMAIL_USER, EMAIL_PASS)
        server.send_message(msg)
        server.quit()
        return True
    except Exception as e: 
        print(f"Email Error: {e}")
        return False

@app.route('/')
def index():
    return jsonify({"status": "running", "timestamp": datetime.now().isoformat()}), 200

@app.errorhandler(404)
def not_found(e):
    return jsonify({"error": "Endpoint not found"}), 404

@app.errorhandler(500)
def server_error(e):
    return jsonify({"error": "Internal server error"}), 500

@app.errorhandler(Exception)
def handle_exception(e):
    errorMessage = str(e)
    app.logger.error(f"Unhandled Exception: {errorMessage}", exc_info=True)
    return jsonify({"error": errorMessage}), 500

@app.route('/register', methods=['POST'])
def register():
    app.logger.info("Register request received")
    try:
        data = request.get_json()
        if not data: return jsonify({"error": "No data provided"}), 400
        
        full_name = data.get('full_name')
        email = data.get('email')
        password = data.get('password')

        if not all([full_name, email, password]):
            return jsonify({"error": "Missing required fields"}), 400

        cur = mysql.connection.cursor()
        cur.execute("SELECT id FROM users WHERE email = %s", (email,))
        if cur.fetchone():
            cur.close()
            return jsonify({"error": "Email already registered"}), 400

        otp = str(random.randint(100000, 999999))
        expiry = datetime.now() + timedelta(minutes=5)
        hashed_pw = generate_password_hash(password)

        cur.execute(
            "INSERT INTO pending_registrations (full_name, email, password, otp, otp_expiry) VALUES (%s, %s, %s, %s, %s) "
            "ON DUPLICATE KEY UPDATE full_name=%s, password=%s, otp=%s, otp_expiry=%s",
            (full_name, email, hashed_pw, otp, expiry, full_name, hashed_pw, otp, expiry)
        )
        mysql.connection.commit()
        cur.close()
        
        send_otp_email(email, otp, "KisanCare AI Registration OTP")
        return jsonify({"message": "OTP sent to your email"}), 201
    except Exception as e:
        app.logger.error(f"Registration Error: {str(e)}", exc_info=True)
        return jsonify({"error": f"Database Error: {str(e)}"}), 500

@app.route('/verify-otp', methods=['POST'])
def verify_otp():
    try:
        data = request.get_json()
        email, otp = data.get('email'), data.get('otp')
        if not email or not otp: return jsonify({"error": "Email and OTP required"}), 400
        
        cur = mysql.connection.cursor()
        
        # Check pending registrations first
        cur.execute("SELECT full_name, password, otp, otp_expiry FROM pending_registrations WHERE email = %s", (email,))
        pending = cur.fetchone()
        
        if pending:
            if pending[2] == otp:
                if datetime.now() > pending[3]:
                    cur.close()
                    return jsonify({"error": "OTP has expired"}), 400
                
                # Success! Move to users table
                cur.execute(
                    "INSERT INTO users (full_name, email, password, otp, otp_expiry, is_verified) VALUES (%s, %s, %s, %s, %s, TRUE)",
                    (pending[0], email, pending[1], pending[2], pending[3])
                )
                cur.execute("DELETE FROM pending_registrations WHERE email = %s", (email,))
                mysql.connection.commit()
                cur.close()
                return jsonify({"message": "Account verified and created successfully!"}), 200
            else:
                cur.close()
                return jsonify({"error": "Invalid OTP"}), 400

        # Check existing users (for password reset)
        cur.execute("SELECT otp, otp_expiry, is_verified FROM users WHERE email = %s", (email,))
        user = cur.fetchone()
        
        if user and user[0] == otp:
            if datetime.now() > user[1]:
                cur.close()
                return jsonify({"error": "OTP has expired"}), 400
            
            # For reset password flow, we keep the OTP for /reset-password
            cur.close()
            return jsonify({"message": "OTP verified successfully!"}), 200
            
        cur.close()
        return jsonify({"error": "Invalid OTP"}), 400
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route('/resend-otp', methods=['POST'])
def resend_otp():
    try:
        data = request.get_json()
        email = data.get('email')
        if not email: return jsonify({"error": "Email is required"}), 400
        
        otp = str(random.randint(100000, 999999))
        expiry = datetime.now() + timedelta(minutes=5)
        
        cur = mysql.connection.cursor()
        
        # Check if it's a pending registration
        cur.execute("SELECT id FROM pending_registrations WHERE email = %s", (email,))
        if cur.fetchone():
            cur.execute("UPDATE pending_registrations SET otp = %s, otp_expiry = %s WHERE email = %s", (otp, expiry, email))
            mysql.connection.commit()
            cur.close()
            send_otp_email(email, otp, "KisanCare AI Registration OTP (Resent)")
            return jsonify({"message": "New OTP sent to your email"}), 200
            
        # Check if it's an existing user (forgot password flow)
        cur.execute("SELECT id FROM users WHERE email = %s", (email,))
        if cur.fetchone():
            cur.execute("UPDATE users SET otp = %s, otp_expiry = %s WHERE email = %s", (otp, expiry, email))
            mysql.connection.commit()
            cur.close()
            send_otp_email(email, otp, "KisanCare AI Password Reset OTP (Resent)")
            return jsonify({"message": "New OTP sent to your email"}), 200

        cur.close()
        return jsonify({"error": "Email not found"}), 404
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route('/login', methods=['POST'])
def login():
    try:
        data = request.get_json()
        email, password = data.get('email'), data.get('password')
        if not email or not password: return jsonify({"error": "Email and password required"}), 400
        
        cur = mysql.connection.cursor()
        cur.execute("SELECT password, is_verified, full_name, email FROM users WHERE email = %s", (email,))
        user = cur.fetchone()
        cur.close()
        
        if user and check_password_hash(user[0], password):
            if not user[1]: return jsonify({"error": "Please verify your email"}), 403
            return jsonify({
                "message": f"Welcome {user[2]}",
                "full_name": user[2],
                "email": user[3]
            }), 200
        return jsonify({"error": "Invalid credentials"}), 401
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route('/forgot-password', methods=['POST'])
def forgot_password():
    try:
        data = request.get_json()
        email = data.get('email')
        if not email: return jsonify({"error": "Email is required"}), 400
        
        cur = mysql.connection.cursor()
        cur.execute("SELECT id FROM users WHERE email = %s", (email,))
        if not cur.fetchone():
            cur.close()
            return jsonify({"error": "Email not registered"}), 404
            
        otp = str(random.randint(100000, 999999))
        expiry = datetime.now() + timedelta(minutes=5)
        cur.execute("UPDATE users SET otp = %s, otp_expiry = %s WHERE email = %s", (otp, expiry, email))
        mysql.connection.commit()
        cur.close()
        
        send_otp_email(email, otp, "KisanCare AI Password Reset OTP")
        return jsonify({"message": "OTP sent to your email"}), 200
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route('/reset-password', methods=['POST'])
def reset_password():
    try:
        data = request.get_json()
        email = data.get('email')
        otp = data.get('otp')
        new_password = data.get('new_password')
        confirm_p = data.get('confirm_password')

        if not all([email, otp, new_password, confirm_p]):
            return jsonify({"error": "Missing required fields"}), 400
            
        if new_password != confirm_p:
            return jsonify({"error": "Passwords do not match"}), 400

        cur = mysql.connection.cursor()
        cur.execute("SELECT otp, otp_expiry FROM users WHERE email = %s", (email,))
        user = cur.fetchone()
        
        if user and user[0] == otp:
            if datetime.now() > user[1]:
                cur.close()
                return jsonify({"error": "OTP has expired"}), 400
                
            hashed_pw = generate_password_hash(new_password)
            cur.execute("UPDATE users SET password = %s WHERE email = %s", (hashed_pw, email))
            mysql.connection.commit()
            cur.close()
            return jsonify({"message": "Password reset successful"}), 200
            
        cur.close()
        return jsonify({"error": "Invalid OTP"}), 400
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route('/api/user/profile', methods=['GET'])
def get_profile():
    try:
        email = request.args.get('email')
        if not email: return jsonify({"error": "Email required"}), 400
        
        cur = mysql.connection.cursor()
        cur.execute("""
            SELECT full_name, email, mobile, pincode, state, district 
            FROM users
            WHERE email = %s
        """, (email,))
        user = cur.fetchone()
        cur.close()
        
        if not user: return jsonify({"error": "User not found"}), 404
        
        return jsonify({
            "full_name": user[0],
            "email": user[1],
            "mobile": user[2] or "",
            "pincode": user[3] or "",
            "state": user[4] or "",
            "district": user[5] or ""
        }), 200
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route('/api/user/profile', methods=['PUT'])
def update_profile():
    try:
        data = request.get_json()
        email = data.get('email')
        if not email: return jsonify({"error": "Email required"}), 400
        
        cur = mysql.connection.cursor()
        # Get user id
        cur.execute("SELECT id FROM users WHERE email = %s", (email,))
        user_row = cur.fetchone()
        if not user_row:
            cur.close()
            return jsonify({"error": "User not found"}), 404
        
        user_id = user_row[0]
        
        # Update users table
        cur.execute("""
            UPDATE users 
            SET full_name = %s, mobile = %s, pincode = %s, state = %s, district = %s 
            WHERE id = %s
        """, (data.get('full_name'), data.get('mobile'), data.get('pincode'), data.get('state'), data.get('district'), user_id))
            
        mysql.connection.commit()
        cur.close()
        return jsonify({"message": "Profile updated successfully"}), 200
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route('/api/user/history', methods=['POST'])
def save_history():
    try:
        data = request.get_json()
        email = data.get('email')
        query = data.get('query')
        response_text = data.get('response')
        is_disease = data.get('is_disease', False)

        if not email or not query:
            return jsonify({"error": "Email and query are required"}), 400

        cur = mysql.connection.cursor()
        cur.execute("SELECT id FROM users WHERE email = %s", (email,))
        user = cur.fetchone()
        if not user:
            cur.close()
            return jsonify({"error": "User not found"}), 404
        
        user_id = user[0]
        cur.execute("""
            INSERT INTO query_history (user_id, query, response, is_disease)
            VALUES (%s, %s, %s, %s)
        """, (user_id, query, response_text, is_disease))
        mysql.connection.commit()
        cur.close()
        return jsonify({"message": "History saved successfully"}), 201
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route('/api/user/history', methods=['GET'])
def get_user_history():
    try:
        email = request.args.get('email')
        if not email:
            return jsonify({"error": "Email is required"}), 400

        cur = mysql.connection.cursor()
        cur.execute("SELECT id FROM users WHERE email = %s", (email,))
        user = cur.fetchone()
        if not user:
            cur.close()
            return jsonify({"error": "User not found"}), 404
        
        user_id = user[0]
        cur.execute("""
            SELECT id, query, response, is_disease, created_at 
            FROM query_history 
            WHERE user_id = %s 
            ORDER BY created_at DESC
        """, (user_id,))
        history = cur.fetchall()
        cur.close()

        result = []
        for row in history:
            result.append({
                "id": row[0],
                "query": row[1],
                "response": row[2] or "",
                "is_disease": bool(row[3]),
                "timestamp": int(row[4].timestamp() * 1000)
            })
        
        return jsonify(result), 200
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route('/api/user/history/<int:history_id>', methods=['DELETE'])
def delete_history_item(history_id):
    try:
        cur = mysql.connection.cursor()
        cur.execute("DELETE FROM query_history WHERE id = %s", (history_id,))
        mysql.connection.commit()
        cur.close()
        return jsonify({"message": "Item deleted"}), 200
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route('/api/user/history/clear', methods=['DELETE'])
def clear_user_history():
    try:
        email = request.args.get('email')
        if not email:
            return jsonify({"error": "Email is required"}), 400

        cur = mysql.connection.cursor()
        cur.execute("SELECT id FROM users WHERE email = %s", (email,))
        user = cur.fetchone()
        if not user:
            cur.close()
            return jsonify({"error": "User not found"}), 404
        
        user_id = user[0]
        cur.execute("DELETE FROM query_history WHERE user_id = %s", (user_id,))
        mysql.connection.commit()
        cur.close()
        return jsonify({"message": "History cleared"}), 200
    except Exception as e:
        return jsonify({"error": str(e)}), 500

if __name__ == '__main__':
    init_db()
    app.run(host='0.0.0.0', port=8080, debug=True, use_reloader=False)