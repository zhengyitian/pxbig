import socket
import struct

saveDir = 'd:/drawinfo/'
import os
l = os.listdir(saveDir)
print(l)
add = ('192.168.43.1',8898)


def ser(i,ff):    
    print(ff)
    a = socket.socket()
    a.connect(add)    
    f = open(ff,'rb')
    s = f.read()
    f.close()
    a.sendall(struct.pack('i',i)+s)
    a.close()

for i in range(300):
    ff = str(i)+'.txt'
    if ff in l:
        ser(i,saveDir+ff)
s = socket.socket()
s.connect(add)
s.sendall(struct.pack('i',333))
s.close()

