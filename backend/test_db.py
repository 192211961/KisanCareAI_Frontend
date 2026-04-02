import MySQLdb
try:
    print("Connecting...")
    db = MySQLdb.connect(host="localhost", user="root", passwd="")
    print("Connected!")
    cursor = db.cursor()
    cursor.execute("CREATE DATABASE IF NOT EXISTS kisanmitra")
    print("DB OK")
    db.select_db("kisanmitra")
    print("Selected DB")
    db.close()
    print("Done")
except Exception as e:
    print(f"Error: {e}")
