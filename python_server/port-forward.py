#from PyQt5 import QtCore
#a = QtCore.qCompress(b'aa1')
#print(a)
#b = QtCore.qUncompress( b'\x00\x00\x00\x00'+ a[4:])
#print(b)

import threading
import socket
s = socket.socket()
add = ('192.168.1.188',8899)
s.bind(add)
s.listen(10)

def one(a):
    re = a.recv(16)
    print(re)
    
    add2 = ('127.0.0.1',8899)
    s2 = socket.socket()
    s2.connect(add2)
    s2.sendall(re)
    
    d = b''
    while True:
        ss = s2.recv(65536)
        if not ss:
            break
        d+=ss
    
    print(len(d))
    a.sendall(d)
tl = []
while True:
    a,b = s.accept()
    print(a)
    print('tl',len(tl))
    if len(tl)>100:
        tl = tl[-100:]
    t = threading.Thread(target=one,args=(a,))
    t.setDaemon(True)
    t.start()
    tl.append(t)
    

