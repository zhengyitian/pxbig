import pyaudio
import wave,socket
add = ('0.0.0.0',18799)
so = socket.socket()
so.setsockopt(socket.SOL_SOCKET,socket.SO_REUSEADDR,1)
so.bind(add)
so.listen(100)
FORMAT = pyaudio.paInt16
CHANNELS = 2
RATE = 44100
CHUNK = 512*2
index = 2

def one():
    a,b = so.accept()
    ss = a.recv(100)
    print('start',len(ss))    
    audio = pyaudio.PyAudio()    
    stream = audio.open(format=FORMAT, channels=CHANNELS,
                        rate=RATE, input=True,input_device_index = index,
                    frames_per_buffer=CHUNK,as_loopback = True)
    while True:
        try:
            data = stream.read(CHUNK)
            a.sendall(data)
        except:
            break
    stream.stop_stream()
    stream.close()
    audio.terminate()    
while True:
    one()
