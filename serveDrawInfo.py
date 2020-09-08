import socket
import struct

saveDir = '/home/a/drawinfo/'
import os
l = os.listdir(saveDir)
print(l)
add = ('0.0.0.0',8898)
so = socket.socket()
so.setsockopt(socket.SOL_SOCKET,socket.SO_REUSEADDR,1)
so.bind(add)
so.listen(300)

def ser(i,ff):
    print(ff)
    f = open(ff,'rb')
    s = f.read()
    f.close()
    a,b = so.accept()
    a.sendall(struct.pack('i',i)+s)
    a.close()

for i in range(300):
    ff = str(i)+'.txt'
    if ff in l:
        ser(i,saveDir+ff)

        