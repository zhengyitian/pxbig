import socket
saveDir = 'e:/drawinfo'

add = ('192.168.1.110',8898)
def xx():
    s = socket.socket()
    s.connect(add)
    d = b''
    while True:
        r = s.recv(65536)
        if not r:
            break
        d += r
    import struct
    a = struct.unpack('i',d[:4])[0]
    print(a)
    print(d[4:])
    f = open(saveDir+str(a)+'.txt','wb')
    f.write(d[4:])
    f.close()
while True:
    xx()
        