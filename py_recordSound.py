#windows open steroio mix
#linux:
#Install sudo apt-get install pulseaudio-utils lame mpg123
#pacmd list-sinks | grep -e 'name:' -e 'index' -e 'Speakers'
#parec -d alsa_output.pci-0000_00_1f.3.analog-stereo.monitor | lame -r -V0 - out.mp3
#or 
#sudo add-apt-repository ppa:audio-recorder/ppa
#sudo apt-get update && sudo apt-get install audio-recorder

import pyaudio
import wave

FORMAT = pyaudio.paInt16
CHANNELS = 1
RATE = 44100
CHUNK = 512
RECORD_SECONDS = 5
WAVE_OUTPUT_FILENAME = "recordedFile.wav"
device_index = 2
audio = pyaudio.PyAudio()

print("----------------------record device list---------------------")
info = audio.get_host_api_info_by_index(0)
numdevices = info.get('deviceCount')
for i in range(0, numdevices):
    if (audio.get_device_info_by_host_api_device_index(0, i).get('maxInputChannels')) > 0:
        xx =  audio.get_device_info_by_host_api_device_index(0, i).get('name')

        print("Input Device id ", i, " - ",xx)

print("-------------------------------------------------------------")

index = int(input())
print("recording via index "+str(index))

#stream = audio.open(format=FORMAT, channels=CHANNELS,
                    #rate=RATE, input=True,input_device_index = index,
                #frames_per_buffer=CHUNK)
#https://github.com/intxcc/pyaudio_portaudio
stream = audio.open(format=FORMAT, channels=CHANNELS,
                    rate=RATE, input=True,input_device_index = index,
                frames_per_buffer=CHUNK,as_loopback = True)
                
print ("recording started")
Recordframes = []

for i in range(0, int(RATE / CHUNK * RECORD_SECONDS)):
    data = stream.read(CHUNK)
    Recordframes.append(data)
print ("recording stopped")

stream.stop_stream()
stream.close()
audio.terminate()

waveFile = wave.open(WAVE_OUTPUT_FILENAME, 'wb')
waveFile.setnchannels(CHANNELS)
waveFile.setsampwidth(audio.get_sample_size(FORMAT))
waveFile.setframerate(RATE)
waveFile.writeframes(b''.join(Recordframes))
waveFile.close()