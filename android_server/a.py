import socket
import threading
import datetime,time
from collections import deque

g_lock = threading.Lock()
#1 is on,0 is off
g_stat = b'0'
ti = datetime.datetime.now()
g_5time = 0

def getStat():
    if g_stat==b'1':
        return g_stat
    if time.time()-g_5time<5:
        return b'1'
    return g_stat
    

def one(a):
    global g_stat,ti,g_5time
    l = []
    co = 0
    
    while True:
        s = a.recv(10000)
        if not s:
            break
        co += len(s)
        
        l.append(s)
        if co>=11:
            break
    ss = b''.join(l)
    if len(ss)!=11:
        return
    if ss[:10]!=b'zytlyyzyzy':
        return
    
    if ss[10:]==b'3':
        g_lock.acquire()
        ti = datetime.datetime.now()
        x = getStat()
        g_lock.release()
        a.sendall(x)
        return

    print (len(ss),ss[10])

    if ss[10]==5:
        g_lock.acquire()
        g_5time = time.time()
        tt = str(ti).encode()
        g_lock.release()
        a.sendall(tt)
        return

    if ss[10]==0:
        g_lock.acquire()
        tt = str(ti).encode()
        g_stat = b'1'
        g_lock.release()
        a.sendall(tt)
        return
    g_lock.acquire()
    tt = str(ti).encode()
    g_stat = b'0'
    g_lock.release()
    a.sendall(tt)



s = socket.socket()
s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
add = ('0.0.0.0',50005)
s.bind(add)
s.listen(30)
tl = deque()
while True:
    if len(tl)>100:
        tl.popleft()
    a,b = s.accept()
    print(a)
    t = threading.Thread(target=one,args=(a,))
    t.setDaemon(True)
    t.start()
    tl.append(t)