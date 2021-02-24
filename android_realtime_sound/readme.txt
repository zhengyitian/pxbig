1 : ip
2 : original sound point * |  original sound point +
slag :server max sound lad time, clag: client max sound lad time,sleep:when no data, client sleep this millyseconds
last line: left->index,middle->value_change_rate,right->value_change_plus
new value = original sound point value mod index * value_change_rate+value_change_plus

split one value to 3 value(two blanks for one, multi and plus)

switch_in on : sound from normal mic,otherwise from camera mic
switch_out on : sound out speaker when headset on
switch_split on: enable 1->3 change

fft栏，两个对应左右声道，按L按钮加载，被“-”分割成两个数，构成一个范围。当都包含“-”，且fre是1，且没开split时，开启fft（傅里叶变换）。此范围内的频谱保留。每次fft的长度为8192（通过self录音），4096（通过网络收音）.因频谱是对称的，所以范围的最大值分别是4096，2048。
