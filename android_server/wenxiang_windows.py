from PyQt5.QtWidgets import QApplication, QWidget, QPushButton, QVBoxLayout
import threading,time,socket

def xx():
    while True:
        try:
            
            s = socket.socket()
            add = ('42.51.28.250',50005)
            s.connect(add)
            ss = b'zytlyyzyzy\x05'
            print(len(ss))
            s.sendall(ss)
            x = s.recv(100)
            print(x)
            time.sleep(1)
            print(time.time())
        except Exception as e:
            print(e)
            

t = threading.Thread(target=xx)
t.setDaemon(True)
t.start()
app = QApplication([])
window = QWidget()
layout = QVBoxLayout()
layout.addWidget(QPushButton('Top'))
layout.addWidget(QPushButton('Bottom'))
window.setLayout(layout)
#window.show()
app.exec()