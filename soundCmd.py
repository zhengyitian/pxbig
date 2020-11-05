#coding:utf8
import socket
import wave
import pyaudio
RATE = 44100

add = ('192.168.1.110',18799)
s = socket.socket()
s.connect(add)
s.sendall(b'28')
Recordframes = []
le = 0
p=pyaudio.PyAudio()
stream=p.open(format=pyaudio.paInt16, channels=2, rate=RATE, output=True)
    
while True:
    a = s.recv(2048)
    if not a:
        break
    #Recordframes.append(a)
    stream.write(a) 
    le += len(a)
    if le>1024*1024000:
        break

s.close()
stream.stop_stream()            # 停止数据流
stream.close()                        # 关闭数据流
p.terminate()                          # 关闭 PyAudio

WAVE_OUTPUT_FILENAME = "recordedFile.wav"

waveFile = wave.open('a.wav', 'wb')
waveFile.setnchannels(2)
waveFile.setsampwidth(2)
waveFile.setframerate(RATE)
waveFile.writeframes(b''.join(Recordframes))
waveFile.close()