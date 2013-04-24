import thegiant
import sqlite3
conn = sqlite3.connect('/tmp/example.db')

c = conn.cursor()
try:
    c.execute(''' create table kv(key, value)''')
    conn.commit()
except Exception as e:
    print e

def set(key, val):
    print "inserting", key, val
    # if exists 
    cursor = c.execute("select key from kv where key=?", (key,))
    row = cursor.fetchone()
    if not row:    
        c.execute('insert into kv(key, value) values (?, ?)', (key, val))
    else:
        c.execute('update kv set key=?, value=?', (key, val))

def get(key):    
    print "try to return key", key
    cursor = c.execute("select value from kv where key=?", (key,))
    row = cursor.fetchone()
    print "returning", row[0]
    return row[0]

def commit():
    conn.commit()


