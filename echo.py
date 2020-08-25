import socket,select
import time,random
import struct
from PyQt5.QtGui  import  *
from PyQt5.QtWidgets  import  *
from PyQt5.QtCore  import  *
from PyQt5.QtNetwork import * 

xLen = 2244
yLen = 2244
xS = 800
yS = 800
yL = 50        
xL = 50  
drawP1 = 100
drawP2 = 100
maxX = 1366
maxY = 768


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
    def __init__(self, arg0=None, arg1=None, arg2=None):
        super().__init__()
        t = QTimer(self)
        t.timeout.connect(self.aa)
        t.start(100)
        self.data = b''
        self.pic = None
        self.tcpCli = None
        self.ini()
        self.posCache = None
        QCoreApplication.instance().installEventFilter(self)
        self.pause = False
        self.reso = 0
        self.tcpStartTime = time.monotonic()
        self.setGeometry(QRect(200,200,800,500))
        
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
                
    def draw(self):
        if  not self.pic:
            return        
        painter = QPainter(self)
        pic = self.pic
        if self.reso == 0:
            painter.drawImage(QRect(drawP1,drawP2,pic.width(),pic.height()),pic)    
            return
        pa = maxX
        if pic.width()*self.reso < maxX:
            pa = pic.width()*self.reso
        pb = maxY
        if pic.height()*self.reso < maxY:
            pb = pic.height()*self.reso            
        im = QPixmap(QSize(pa,pb)).toImage()
        
        for i in range(pic.width()):
            for j in range(pic.height()):
                if (i+1)*self.reso>maxX or (j+1)*self.reso>maxY:
                    continue
                c = pic.pixel(QPoint(i,j))
                cc = QColor(c)
                r,g,b = qRed(c),qGreen(c),qBlue(c)
                self.drawOne('r',r,i,j,im)
                self.drawOne('g',g,i,j,im)
                self.drawOne('b',b,i,j,im)
        painter.drawImage(QRect(drawP1,drawP2,im.width(),im.height()),im)    
            
    def onP(self,e):
        self.pause = not self.pause
        if self.pause:            
            self.pBtn.setText('->')
        else:
            self.pBtn.setText('||')
            
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
        if xS1<0 or yS1<0 or yL1<=0 or xL1<=0 or  xS1+xL1>xLen or yS1+yL1>yLen:
            print ('set error')
            return            
        
        if rr<0:
            self.reso = 0
        else:
            self.reso = (rr//3)*3
            
        self.posCache = (xS1,yS1,xL1,yL1)        
        
    def ini(self):
        v = QVBoxLayout()
        h = QHBoxLayout()
        q = QPushButton('set')
        q.clicked.connect(self.onCli)
        
        self.pBtn = q2 = QPushButton('||')
        q2.clicked.connect(self.onP)
        
        self.l1 = myLine('1000')
        self.l2 = myLine('800')
        self.l3 = myLine('50')
        self.l4 = myLine('50')
        self.l5 = myLine('0')

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
    
    def aa(self):
        self.repaint()
        
    def dealRead(self):
        self.data += self.tcpCli.readAll()
                
    def dealClose(self):    
        #print('tcpOver',len(self.data),time.monotonic()-self.tcpStartTime)
        self.tcpCli.close()
        self.tcpCli.deleteLater()
        self.tcpCli = None
        if not self.data:
            return
        d = self.data
        self.data = b''        
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
        self.pic = im   
        self.repaint()        
        
    def dealConn(self):    
        self.tcpCli.readyRead.connect(self.dealRead)
        self.tcpCli.disconnected.connect(self.dealClose)        
        dd = struct.pack('i',xS)+struct.pack('i',yS)+struct.pack('i',xL)+struct.pack('i',yL)        
        self.tcpCli.write(dd)
        
    def paintEvent(self, e):     
        #return super().paintEvent(e)
        global xS,yS,xL,yL
        self.draw()                
        if self.tcpCli or self.pause:                          
            return        
        if self.posCache:                     
            xS,yS,xL,yL = self.posCache
            self.posCache = None            
        self.tcpStartTime = time.monotonic()            
        self.tcpCli = s = QTcpSocket(self)
        s.connected.connect(self.dealConn)
        s.connectToHost('10.23.185.230',8899)

a = QApplication([])        
window = w = ww()
w.show()
a.exec_()




