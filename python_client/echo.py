import socket,select
import time,random
import struct
from PyQt5.QtGui  import  *
from PyQt5.QtWidgets  import  *
from PyQt5.QtCore  import  *
from PyQt5.QtNetwork import * 
import socket

xCapLen = 2244
yCapLen = 2244
drawP1 = 0
drawP2 = 50
maxX = 1366
maxY = 768

import threading
g_lock = threading.Lock()
g_para = (1000,100,100,100)
g_buf = (b'0'*40000,100,100,0)

def tcpGetBuf(a,b,c,d):
    s = socket.socket()
    add = ('10.23.185.230',8899)
    s.connect(add)
    s.sendall(struct.pack('i',a)+struct.pack('i',b)+struct.pack('i',c)+struct.pack('i',d))
    d = b''
    while True:
        ss = s.recv(65536)
        if not ss:
            break
        d+=ss
    return d

def thr():
    global g_buf
    co = 0
    while True:
        co =+ 1
        g_lock.acquire()
        a,b,c,d = g_para
        g_lock.release()
        try:
            ss = tcpGetBuf(a,b,c,d)
            if len(ss) != c*d*4:
                continue
        except:
            continue
        
        g_lock.acquire()
        g_buf = (ss,c,d,co)
        g_lock.release()
        
class qThr(QThread):
    def __init__(self):
        super().__init__()
        self.co = 0
        
    def run(self):
        while True:
            self.w()
            
    def w(self):
        self.co += 1
        s = QTcpSocket(self)
        #s.connectToHost('10.23.185.230',8899)
        s.connectToHost('127.0.0.1',8899)
        r = s.waitForConnected(1000)
        if not r:
            s.deleteLater()
            return
        
        g_lock.acquire()
        a,b,c,d = g_para
        g_lock.release()
        s.write(struct.pack('i',a)+struct.pack('i',b)+struct.pack('i',c)+struct.pack('i',d))
        r = s.waitForBytesWritten(1000)
        if not r:
            s.deleteLater()
            return
        
        da = b''     
        t = time.monotonic()
        while True:
            s.waitForReadyRead(1000)
            if time.monotonic()-t>5:
                s.deleteLater()
                return
            if not r:
                continue
            ss = s.readAll()
            if not ss:
                break
            da += ss
        s.deleteLater()    
        if len(da) != c*d*4:
            return
        global g_buf
        g_lock.acquire()
        g_buf = (da,c,d,self.co)
        g_lock.release()        

            
class myLine(QLineEdit):
    def __init__(self,text=''):
        super().__init__(text)
        
    def wheelEvent(self, e):
        a = int(self.text())
        aa = e.angleDelta().y()/120
        t = a+int(aa)
        self.setText(str(t))
        window.onCli(e)

class ww(QWidget):
    def __init__(self):
        super().__init__()
        t = QTimer(self)
        t.timeout.connect(self.onTimer)
        t.start(100)

        QCoreApplication.instance().installEventFilter(self)
        self.pause = False
        self.reso = 0
        self.xStart , self.yStart , self.xLen , self.yLen = g_para
        self.ini()
        self.setGeometry(QRect(200,200,800,500))        
        self.th = t = threading.Thread(target=thr)
        t.setDaemon(True)
       # t.start()
        self.q = q = qThr()
        q.start()
        
    def ini(self):
        v = QVBoxLayout()
        h = QHBoxLayout()
        q = QPushButton('set')
        q.clicked.connect(self.onCli)
        
        self.pBtn = q2 = QPushButton('||')
        q2.clicked.connect(self.onP)
        
        self.l1 = myLine(str(self.xStart))
        self.l2 = myLine(str(self.yStart))
        self.l3 = myLine(str(self.xLen))
        self.l4 = myLine(str(self.yLen))
        self.l5 = myLine(str(self.reso))

        h.addWidget(self.l1)
        h.addWidget(self.l2)
        h.addWidget(self.l3)
        h.addWidget(self.l4)
        h.addWidget(self.l5)
        h.addWidget(q)
        h.addWidget(q2)
        
        v.addLayout(h)
        v.addStretch()
        self.setLayout(v)
        
    def onTimer(self):
        self.repaint()  
        
    def onCli(self,e):       
        try:
            xS1 = int(self.l1.text())
            yS1 = int(self.l2.text())
            xL1 = int(self.l3.text())
            yL1 = int(self.l4.text())
            rr = int(self.l5.text())
        except:
            print ('set error')
            return
        if xS1<0 or yS1<0 or yL1<=0 or xL1<=0 or  xS1+xL1>xCapLen or yS1+yL1>yCapLen:
            print ('set error')
            return            
        
        if rr<0:
            self.reso = 0
        else:
            self.reso = (rr//3)*3
        self.xStart,self.yStart,self.xLen,self.yLen = xS1,yS1,xL1,yL1      
        
    def eventFilter(self,obj,e):
        if e.type() == QEvent.KeyPress:
            print(e.key())
            
            if e.key() == 65:
                self.l1.setText(str(int(self.l1.text())-10))
    
            elif e.key() == 74:
                self.l3.setText(str(int(self.l3.text())-10))   
                
            elif e.key() == 87:
                self.l2.setText(str(int(self.l2.text())-10))  
                
            elif e.key() == 73:
                self.l4.setText(str(int(self.l4.text())-10))  
                
            elif e.key() == 75:
                self.l4.setText(str(int(self.l4.text())+10)) 
                
            elif e.key() == 76:
                self.l3.setText(str(int(self.l3.text())+10))  
                
            elif e.key() == 83:
                self.l2.setText(str(int(self.l2.text())+10))  
                
            elif e.key() == 81:
                self.l5.setText(str(int(self.l5.text())-3))  
                
            elif e.key() == 69:
                self.l5.setText(str(int(self.l5.text())+3))  
                
            elif e.key() == 68:
                self.l1.setText(str(int(self.l1.text())+10))  
                
            elif e.key() == 32:
                self.onP(e)                
            else:
                return super().eventFilter(obj,e)
            self.onCli(e)
            e.accept()
            return True     
        
        return super().eventFilter(obj,e)
    
    def onP(self,e):
        self.pause = not self.pause
        if self.pause:            
            self.pBtn.setText('->')
        else:
            self.pBtn.setText('||')   
    def drawOne(self,ty,v,i,j,im):
        redCount = v/255*self.reso*self.reso/3
        redCo = 0
        if ty=='r':
            cc = Qt.red
            gap = 0
        if ty=='g':
            cc = Qt.green
            gap = self.reso//3
        if ty=='b':
            cc = Qt.blue
            gap = self.reso*2//3
            
        for ii in range(i*self.reso+gap,gap+i*self.reso+self.reso//3):
            for jj in range(j*self.reso,(j+1)*self.reso):
                if redCo >= redCount:
                    im.setPixelColor(ii,jj,Qt.black)
                    continue                
                im.setPixelColor(ii,jj,cc)
                redCo += 1  
                
    def draw(self,d,xl,yl):      
        painter = QPainter(self)
        pa = maxX
        if xl*self.reso < maxX:
            pa = xl*self.reso
        pb = maxY
        if yl*self.reso < maxY:
            pb = yl*self.reso            
        im = QPixmap(QSize(pa,pb)).toImage()
        
        for i in range(xl):
            for j in range(yl):
                if (i+1)*self.reso>maxX or (j+1)*self.reso>maxY:
                    continue
                
                val = i+j*xl
                one = d[val*4:val*4+4]
                argb = struct.unpack('I',one)[0]
                b = (argb>>16)&0xFF;
                g = (argb>>8)&0xFF;
                r = (argb>>0)&0xFF;                 
                self.drawOne('r',r,i,j,im)
                self.drawOne('g',g,i,j,im)
                self.drawOne('b',b,i,j,im)
        painter.drawImage(QRect(drawP1,drawP2,im.width(),im.height()),im)                       
            
    def setPara(self):
        global g_para
        g_lock.acquire()
        g_para = (self.xStart,self.yStart,self.xLen,self.yLen)
        g_lock.release()    
    
    def drawOri(self,d,xL,yL):
        im = QPixmap(QSize(xL,yL)).toImage()           
        for j in range(yL):
            for i in range(xL):
                val = i+j*xL
                one = d[val*4:val*4+4]
                argb = struct.unpack('I',one)[0]
                b = (argb>>16)&0xFF;
                g = (argb>>8)&0xFF;
                r = (argb>>0)&0xFF; 
                im.setPixelColor(i,j,QColor(r,g,b))                     
        painter = QPainter(self)        
        painter.drawImage(QRect(drawP1,drawP2,im.width(),im.height()),im) 
    
    def paintEvent(self, e):     
        #return super().paintEvent(e)
        self.setPara()  
        g_lock.acquire()        
        da,xLenB,yLenB = g_buf[0],g_buf[1],g_buf[2]
        g_lock.release()
        if self.reso == 0:
            return self.drawOri(da,xLenB,yLenB)                
        self.draw(da,xLenB,yLenB)                


a = QApplication([])        
window = w = ww()
w.show()
a.exec_()




