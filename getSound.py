import socket
add = ('192.168.1.110',8899)
s = socket.socket()
s.connect(add)
s.sendall(b'aa')
d = b''
while True:
    a = s.recv(10000)
    if not a:
        break
    d+=a
s.close()
print(len(d))
f = open('e:/cc.pcm','wb')
f.write(d)
f.close()