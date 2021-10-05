import socket,os,time
bb = b'zytlyyzyzy'
lastCmd = 1

def one():
    global lastCmd
    s = socket.socket()
    s.settimeout(3)
    add = ("42.51.28.250",50005)
    s.connect(add)
    s.sendall(bb+b'3')
    x = s.recv(100)
    print(x,time.time())
    s.close()
    if x==b'0':
#        if lastCmd==0:
 #           return
        
        lastCmd=0
        cmd = '/home/pi/hub-ctrl.c/hub-ctrl  -h 0 -P 2 -p 0'
        cmd = "echo '1-1'|tee /sys/bus/usb/drivers/usb/unbind"
        os.system(cmd)
#        time.sleep(10)
        cmd = '/opt/vc/bin/tvservice -o'
        os.system(cmd)
    else:
        if lastCmd==1:
            return
        
        cmd = '/home/pi/hub-ctrl.c/hub-ctrl  -h 0 -P 2 -p 1'
        cmd = "echo '1-1'|tee /sys/bus/usb/drivers/usb/bind"
        os.system(cmd)
        
        lastCmd=1
        cmd = '/opt/vc/bin/tvservice -p'
        os.system(cmd)
        

#time.sleep(10)
while True:
    time.sleep(1)
    try:
        one()
    except:
        pass
